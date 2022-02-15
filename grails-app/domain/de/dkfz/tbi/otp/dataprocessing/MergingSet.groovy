/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents a set of {@link ProcessedBamFile}s to be merged
 * for one {@link Sample}. The files are selected using the criteria
 * defined in the corresponding {@link MergingWorkPackage}.
 * A {@link MergingSet} instance is a part of corresponding
 * {@link MergingWorkPackage}.
 */
@Deprecated
@ManagedEntity
class MergingSet implements Entity {

    /**
     * state of processing of {@link MergingSet} instance
     */
    enum State {
        /**
         * The {@link MergingSet} has been declared (created).
         * No processing has been started on the bam files
         * from this set. No processing is planed to be started.
         * Enables manual selection of merging sets
         * to be processed (more control).
         */
        DECLARED,
        /**
         * Flag to be used by workflows to start processing of
         * files of this merging set
         */
        NEEDS_PROCESSING,
        /**
         * Files of this merging set are being processed (merged)
         */
        INPROGRESS,
        /**
         * Files of this merging set has been processed (merged)
         */
        PROCESSED
    }

    /**
     * identifier unique within the corresponding {@link MergingWorkPackage}
     */
    int identifier

    /**
     * current {@link State} of this instance
     */
    State status = State.DECLARED

    MergingWorkPackage mergingWorkPackage

    static belongsTo = [
            mergingWorkPackage: MergingWorkPackage,
    ]

    Project getProject() {
        return mergingWorkPackage.project
    }

    Individual getIndividual() {
        return mergingWorkPackage.individual
    }

    Sample getSample() {
        return mergingWorkPackage.sample
    }

    SampleType getSampleType() {
        return mergingWorkPackage.sampleType
    }

    SeqType getSeqType() {
        return mergingWorkPackage.seqType
    }

    /**
     * @return bam files connected directly with this mergingSet
     */
    List<AbstractBamFile> getBamFiles() {
        return MergingSetAssignment.findAllByMergingSet(this)*.bamFile
    }

    // ignore: will probably replaced with new workflow system
    @SuppressWarnings('ThrowRuntimeException')
    Set<SeqTrack> getContainedSeqTracks() {
        final Set<SeqTrack> seqTracks = [] as Set
        MergingSetAssignment.findAllByMergingSet(this).each { MergingSetAssignment ma ->
            final Set<SeqTrack> seqTracksInIt = ma.bamFile.refresh().containedSeqTracks
            if (!seqTracksInIt) {
                throw new RuntimeException("BAM file ${ma.bamFile} has reported not to contain any SeqTracks.")
            }
            final Collection intersection = seqTracks*.id.intersect(seqTracksInIt*.id)
            if (!intersection.empty) {
                throw new IllegalStateException(
                        "MergingSet ${this} contains at least the SeqTracks with the following IDs more than once:\n${intersection.join(', ')}")
            }
            assert seqTracks.addAll(seqTracksInIt)
        }
        return seqTracks
    }

    /**
     * @return Whether this is the most recent merging set on the referenced {@link MergingWorkPackage}.
     */
    boolean isLatestSet() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    static Integer maxIdentifier(final MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        return MergingSet.createCriteria().get {
            eq("mergingWorkPackage", mergingWorkPackage)
            projections {
                max("identifier")
            }
        }
    }

    static int nextIdentifier(final MergingWorkPackage mergingWorkPackage) {
        assert mergingWorkPackage
        final Integer maxIdentifier = maxIdentifier(mergingWorkPackage)
        if (maxIdentifier == null) {
            return 0
        }
        return maxIdentifier + 1
    }

    static constraints = {
        identifier(unique: 'mergingWorkPackage')
        mergingWorkPackage(validator: { mergingWorkPackage -> mergingWorkPackage.pipeline.name == Pipeline.Name.DEFAULT_OTP })
    }

    static mapping = {
        mergingWorkPackage index: "merging_set_merging_work_package_idx"
    }
}

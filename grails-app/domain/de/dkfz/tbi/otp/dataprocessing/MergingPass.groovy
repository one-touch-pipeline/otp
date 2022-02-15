/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * Instance of this class represent instance of merging process
 * performed on the corresponding {@link MergingSet}.
 */
@Deprecated
@ManagedEntity
class MergingPass implements ProcessParameterObject, Entity {

    /**
     * identifier unique in the scope of corresponding
     * {@link MergingSet}
     */
    int identifier

    String description

    MergingSet mergingSet

    static belongsTo = [
        mergingSet: MergingSet,
    ]

    static constraints = {
        identifier(unique: 'mergingSet')
        description(nullable: true)
    }

    @Override
    Project getProject() {
        return mergingSet.project
    }

    @Override
    Individual getIndividual() {
        return mergingSet.individual
    }

    Sample getSample() {
        return mergingSet.sample
    }

    SampleType getSampleType() {
        return mergingSet.sampleType
    }

    @Override
    SeqType getSeqType() {
        return mergingSet.seqType
    }

    MergingWorkPackage getMergingWorkPackage() {
        return mergingSet.mergingWorkPackage
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return mergingSet.containedSeqTracks
    }

    @Override
    String toString() {
        return "id: ${mergingSet.id} " +
        "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${sample} " +
        "seqType: ${seqType} " +
        "<br>project: ${project}"
    }

    /**
     * @return Whether this is the most recent merging pass on the referenced {@link MergingSet}.
     */
    boolean isLatestPass() {
        return identifier == maxIdentifier(mergingSet)
    }

    static Integer maxIdentifier(final MergingSet mergingSet) {
        assert mergingSet
        return MergingPass.createCriteria().get {
            eq("mergingSet", mergingSet)
            projections {
                max("identifier")
            }
        }
    }

    static int nextIdentifier(final MergingSet mergingSet) {
        assert mergingSet
        Integer maxIdentifier = maxIdentifier(mergingSet)
        if (maxIdentifier == null) {
            return 0
        }
        return maxIdentifier + 1
    }

    static mapping = { mergingSet index: "merging_pass_merging_set_idx" }
}

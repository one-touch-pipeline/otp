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

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@Deprecated
class AlignmentPass implements ProcessParameterObject, Entity {

    enum AlignmentState {
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED,
        /**
         * For legacy data only.
         */
        UNKNOWN,
    }

    int identifier
    SeqTrack seqTrack
    MergingWorkPackage workPackage
    AlignmentState alignmentState

    static belongsTo = [
            workPackage: MergingWorkPackage,
            seqTrack: SeqTrack,
    ]

    static constraints = {
        identifier(unique: 'seqTrack')
        seqTrack(validator: { SeqTrack seqTrack, AlignmentPass pass ->
            pass.workPackage?.satisfiesCriteria(seqTrack)
        })
        workPackage(validator: { workPackage -> workPackage.pipeline.name == Pipeline.Name.DEFAULT_OTP })
    }

    /**
     * The reference genome which is/was used by this alignment pass. This value does not change (in contrast to the
     * return value of {@link SeqTrack#getConfiguredReferenceGenome()} when the configuration changes).
     */
    ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }

    String getDirectory() {
        return "pass${identifier}"
    }

    @Override
    String toString() {
        return "AP ${id}: pass ${identifier} " + (latestPass ? "(latest) " : "") + "on ${seqTrack}"
    }

    /**
     * @return <code>true</code>, if this pass is the latest for the referenced {@link MergingWorkPackage} and {@link SeqTrack}
     */
    boolean isLatestPass() {
        return identifier == maxIdentifier(workPackage, seqTrack)
    }

    static Integer maxIdentifier(final MergingWorkPackage workPackage, final SeqTrack seqTrack) {
        assert workPackage
        assert seqTrack
        return AlignmentPass.createCriteria().get {
            eq("workPackage", workPackage)
            eq("seqTrack", seqTrack)
            projections {
                max("identifier")
            }
        }
    }

    static Integer maxIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        return AlignmentPass.createCriteria().get {
            eq("seqTrack", seqTrack)
            projections {
                max("identifier")
            }
        }
    }

    static int nextIdentifier(final SeqTrack seqTrack) {
        assert seqTrack
        final Integer maxIdentifier = maxIdentifier(seqTrack)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    @Override
    Project getProject() {
        return seqTrack.project
    }

    Sample getSample() {
        return seqTrack.sample
    }

    @Override
    SeqType getSeqType() {
        return seqTrack.seqType
    }

    @Override
    Individual getIndividual() {
        return seqTrack.individual
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([seqTrack])
    }

    static mapping = {
        seqTrack index: "alignment_pass_seq_track_idx"
        workPackage index: "alignment_pass_work_package_idx"
        alignmentState index: "alignment_pass_alignment_state_idx"  // partial index: WHERE alignment_state = 'NOT_STARTED'
    }
}

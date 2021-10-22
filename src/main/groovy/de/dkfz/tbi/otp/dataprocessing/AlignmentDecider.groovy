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

import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * An alignment decider decides how a {@link SeqTrack} will be aligned and merged, and triggers the workflow(s) which
 * will do the alignment and merging.
 *
 * @deprecated class is part of the old workflow system, use {@link de.dkfz.tbi.otp.workflowExecution.decider.Decider} instead
 */
@Deprecated
interface AlignmentDecider {

    /**
     * Decides how the specified {@link SeqTrack} will be aligned, and triggers the workflow(s) which will do the
     * alignment.
     *
     * Finds and/or creates zero, one or more {@link MergingWorkPackage}s which specify how the {@link SeqTrack} will be
     * aligned.
     *
     * @param forceRealign If {@code false}, the method will trigger the alignment only for {@link MergingWorkPackage}s
     * which the alignment of the {@link SeqTrack} has not been triggered for. If {@code true}, the method will trigger
     * the alignment of the {@link SeqTrack} for all found or created {@link MergingWorkPackage}s.
     *
     * @return All found or created {@link MergingWorkPackage}s for the {@link SeqTrack}, regardless of whether the
     * method triggered an alignment for it.
     */
    @Deprecated
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign)
}

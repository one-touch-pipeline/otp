/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflow

import groovy.transform.Immutable

/**
 * Projected scalar data of a single non-withdrawn SNV, Indel, ACEseq or Sophia analysis
 * shown in the analysis table of the trigger workflows page.
 *
 * Multi-valued columns are already joined to display strings (distinct, non-empty, lexically sorted, comma separated).
 */
@Immutable
class TriggerWorkflowsAnalysisRow {

    /** Short type label, see {@link TriggerWorkflowsService#ANALYSIS_TYPE_LABELS} */
    String analysisType

    /** Database ID of the analysis instance, not of a workflow run */
    Long id

    String project

    String individual

    /** Disease and control sample type, in this order */
    String sampleType

    /** Full sequencing type label, matching {@link de.dkfz.tbi.otp.ngsdata.SeqType#getDisplayNameWithLibraryLayout()} */
    String seqType

    String libraryPreparationKits

    /** Species of the individual together with the mixed-in species of both samples */
    String species

    String referenceGenomes

    Long diseaseBamId

    Long controlBamId
}

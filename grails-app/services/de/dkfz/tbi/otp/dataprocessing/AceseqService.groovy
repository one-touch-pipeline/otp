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

class AceseqService extends BamFileAnalysisService implements RoddyBamFileAnalysis, WithReferenceGenomeRestriction {

    @Override
    protected String getProcessingStateCheck() {
        return "sp.aceseqProcessingStatus = :needsProcessing AND " +
                "sp.sophiaProcessingStatus = 'NO_PROCESSING_NEEDED' AND " +
                "EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'FINISHED' AND " +
                "  si.withdrawn = false " +
                ") AND " +
                "NOT EXISTS (FROM SophiaInstance si " +
                "  WHERE si.samplePair = sp AND " +
                "  si.processingState = 'IN_PROGRESS' AND " +
                "  si.withdrawn = false " +
                ") "
    }

    @Override
    Class<AceseqInstance> getAnalysisClass() {
        return AceseqInstance.class
    }

    @Override
    protected Pipeline.Type getAnalysisType() {
        return Pipeline.Type.ACESEQ
    }

    @Override
    Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_ACESEQ
    }

    @Override
    List<String> getReferenceGenomes() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME)
    }
}


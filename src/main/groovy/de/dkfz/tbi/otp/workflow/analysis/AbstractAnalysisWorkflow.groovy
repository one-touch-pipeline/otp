/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.LinearWorkflow

/**
 * Common class for all analysis workflows
 */
abstract class AbstractAnalysisWorkflow implements LinearWorkflow {

    static final String ANALYSIS_OUTPUT = "ANALYSIS_OUTPUT"

    static final String INPUT_TUMOR_BAM = "TUMOR_BAM"

    static final String INPUT_CONTROL_BAM = "CONTROL_BAM"

    @Override
    void reconnectDependencies(Artefact artefact, Artefact newArtefact, String role) {
        BamFilePairAnalysis instance = artefact as BamFilePairAnalysis

        if (role == INPUT_TUMOR_BAM) {
            instance.sampleType1BamFile = newArtefact as AbstractBamFile
        } else if (role == INPUT_CONTROL_BAM) {
            instance.sampleType2BamFile = newArtefact as AbstractBamFile
        }
        instance.save(flush: true)
    }

    @Override
    boolean isAlignment() {
        return false
    }

    @Override
    boolean isAnalysis() {
        return true
    }
}

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
package de.dkfz.tbi.otp.workflow.analysis

import de.dkfz.tbi.otp.dataprocessing.AbstractAnalysisWorkFileService
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.workflowExecution.Artefact
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflow

/**
 * Common class for all analysis workflows
 */
abstract class AbstractAnalysisWorkflow implements OtpWorkflow {

    static final String ANALYSIS_OUTPUT = "ANALYSIS_OUTPUT"

    static final String INPUT_TUMOR_BAM = "TUMOR_BAM"

    static final String INPUT_CONTROL_BAM = "CONTROL_BAM"

    /**
     * Resolve the concrete {@link AbstractAnalysisWorkFileService} used to construct the instance name of the copied artefact.
     */
    abstract AbstractAnalysisWorkFileService getAnalysisWorkFileService()

    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        BamFilePairAnalysis analysisInstance = artefact as BamFilePairAnalysis
        analysisInstance.withdrawn = true
        analysisInstance.save(flush: true)

        SamplePair pair = analysisInstance.samplePair
        String newInstanceName = analysisWorkFileService.constructInstanceName(artefact.workflowArtefact.producedBy.workflowVersion)

        BamFilePairAnalysis outputInstance = analysisInstance.class.newInstance() as BamFilePairAnalysis
        outputInstance.with {
            samplePair = pair
            instanceName = newInstanceName
            config = analysisInstance.config
            sampleType1BamFile = pair.mergingWorkPackage1.bamFileInProjectFolder
            sampleType2BamFile = pair.mergingWorkPackage2.bamFileInProjectFolder
        }
        outputInstance.save(flush: true)

        return outputInstance
    }

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

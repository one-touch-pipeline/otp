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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.SophiaWorkflowQualityAssessment
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.AlignmentLinkFileServiceFactoryService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class SophiaExecuteJob extends AbstractExecuteRoddyPipelineJob implements SophiaWorkflowShared {

    @Autowired
    AlignmentLinkFileServiceFactoryService alignmentLinkFileServiceFactoryService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    SophiaWorkFileService sophiaWorkFileService

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getSophiaInstance(workflowStep)
    }

    @Override
    protected String getRoddyWorkflowName() {
        return 'SophiaWorkflow'
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'sophiaAnalysis'
    }

    @Override
    protected boolean getFilenameSectionKillSwitch() {
        return false
    }

    @Override
    protected Map<String, Map<String, String>> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        SophiaInstance sophiaInstance = getSophiaInstance(workflowStep)

        Path workDirectory = sophiaWorkFileService.getDirectoryPath(sophiaInstance)

        AbstractBamFile bamFileDisease = sophiaInstance.sampleType1BamFile
        AbstractBamFile bamFileControl = sophiaInstance.sampleType2BamFile

        Integer tumorDefaultReadLength = bamFileDisease.maximalReadLength
        Integer controlDefaultReadLength = bamFileControl.maximalReadLength

        SophiaWorkflowQualityAssessment bamFileDiseaseQualityAssessment = bamFileDisease.qualityAssessment as SophiaWorkflowQualityAssessment
        SophiaWorkflowQualityAssessment bamFileControlQualityAssessment = bamFileControl.qualityAssessment as SophiaWorkflowQualityAssessment

        Map<String, Map<String, String>> additionalValues = [
                controlMedianIsize         : roddyConfigValueService.createValueMap(bamFileControlQualityAssessment.insertSizeMedian.toString()),
                tumorMedianIsize           : roddyConfigValueService.createValueMap(bamFileDiseaseQualityAssessment.insertSizeMedian.toString()),
                controlStdIsizePercentage  : roddyConfigValueService.createValueMap(bamFileControlQualityAssessment.insertSizeCV.toString()),
                tumorStdIsizePercentage    : roddyConfigValueService.createValueMap(bamFileDiseaseQualityAssessment.insertSizeCV.toString()),
                controlProperPairPercentage: roddyConfigValueService.createValueMap(bamFileControlQualityAssessment.percentProperlyPaired.toString()),
                tumorProperPairPercentage  : roddyConfigValueService.createValueMap(bamFileDiseaseQualityAssessment.percentProperlyPaired.toString()),
                controlDefaultReadLength   : roddyConfigValueService.createValueMap(controlDefaultReadLength.toString()),
                tumorDefaultReadLength     : roddyConfigValueService.createValueMap(tumorDefaultReadLength.toString()),
        ]

        return roddyConfigValueService.getAnalysisInputVersion2(sophiaInstance, workDirectory) + additionalValues as Map<String, Map<String, String>>
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath) {
    }
}

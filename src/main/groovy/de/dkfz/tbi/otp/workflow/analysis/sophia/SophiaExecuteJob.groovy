/*
 * Copyright 2011-2024 The OTP authors
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
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class SophiaExecuteJob extends AbstractExecuteRoddyPipelineJob implements SophiaWorkflowShared {

    AbstractBamFileServiceFactoryService abstractBamFileServiceFactoryService
    ReferenceGenomeService referenceGenomeService
    SnvCallingService snvCallingService
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
    protected Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        SophiaInstance sophiaInstance = getSophiaInstance(workflowStep)

        Path workDirectory = sophiaWorkFileService.getDirectoryPath(sophiaInstance)

        AbstractBamFile bamFileDisease = sophiaInstance.sampleType1BamFile
        AbstractBamFile bamFileControl = sophiaInstance.sampleType2BamFile

        Path diseaseInsertSizeFile = abstractBamFileServiceFactoryService.getService(bamFileDisease).getFinalInsertSizeFile(bamFileDisease)
        Path controlInsertSizeFile = abstractBamFileServiceFactoryService.getService(bamFileControl).getFinalInsertSizeFile(bamFileControl)

        Integer tumorDefaultReadLength = bamFileDisease.maximalReadLength
        Integer controlDefaultReadLength = bamFileControl.maximalReadLength

        SophiaWorkflowQualityAssessment bamFileDiseaseQualityAssessment = bamFileDisease.qualityAssessment as SophiaWorkflowQualityAssessment
        SophiaWorkflowQualityAssessment bamFileControlQualityAssessment = bamFileControl.qualityAssessment as SophiaWorkflowQualityAssessment

        return roddyConfigValueService.getAnalysisInputVersion2(sophiaInstance, workDirectory) + [
                insertsizesfile_list             : "${controlInsertSizeFile};${diseaseInsertSizeFile}" as String,
                controlMedianIsize               : bamFileControlQualityAssessment.insertSizeMedian.toString(),
                tumorMedianIsize                 : bamFileDiseaseQualityAssessment.insertSizeMedian.toString(),
                controlStdIsizePercentage        : bamFileControlQualityAssessment.insertSizeCV.toString(),
                tumorStdIsizePercentage          : bamFileDiseaseQualityAssessment.insertSizeCV.toString(),
                controlProperPairPercentage      : bamFileControlQualityAssessment.percentProperlyPaired.toString(),
                tumorProperPairPercentage        : bamFileDiseaseQualityAssessment.percentProperlyPaired.toString(),
                controlDefaultReadLength         : controlDefaultReadLength.toString(),
                tumorDefaultReadLength           : tumorDefaultReadLength.toString(),
        ]
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath) {
    }
}

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
package de.dkfz.tbi.otp.workflow.analysis.indel

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelWorkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class IndelExecuteJob extends AbstractExecuteRoddyPipelineJob implements IndelWorkflowShared {

    BedFileService bedFileService
    IndelWorkFileService indelWorkFileService
    ReferenceGenomeService referenceGenomeService

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getIndelInstance(workflowStep)
    }

    @Override
    protected String getRoddyWorkflowName() {
        return 'IndelCallingWorkflow'
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'indelCallingAnalysis'
    }

    @Override
    protected boolean getFilenameSectionKillSwitch() {
        return false
    }

    @Override
    protected Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        IndelCallingInstance indelCallingInstance = getIndelInstance(workflowStep)

        Path workDirectory = indelWorkFileService.getDirectoryPath(indelCallingInstance)

        AbstractBamFile bamFileDisease = indelCallingInstance.sampleType1BamFile
        AbstractBamFile bamFileControl = indelCallingInstance.sampleType2BamFile

        ReferenceGenome referenceGenome = indelCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)

        Path individualPath = individualService.getViewByPidPath(indelCallingInstance.individual, indelCallingInstance.seqType)

        Map<String, String> config = roddyConfigValueService.getAnalysisInputVersion2(indelCallingInstance, workDirectory) + [
                REFERENCE_GENOME          : referenceGenomeFastaFile.path,
                CHR_SUFFIX                : referenceGenome.chromosomeSuffix,
                CHR_PREFIX                : referenceGenome.chromosomePrefix,

                VCF_NORMAL_HEADER_COL     : bamFileControl.sampleType.dirName,
                VCF_TUMOR_HEADER_COL      : bamFileDisease.sampleType.dirName,
                SEQUENCE_TYPE             : bamFileDisease.seqType.roddyName,

                analysisMethodNameOnOutput: individualPath.relativize(workDirectory).toString(),
        ]

        if (bamFileDisease.seqType.needsBedFile) {
            config.put('EXOME_CAPTURE_KIT_BEDFILE', bedFileService.filePath(bamFileDisease.bedFile))
        }

        return config
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath) {
    }
}

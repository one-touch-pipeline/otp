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
package de.dkfz.tbi.otp.workflow.analysis.snv

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvWorkFileService
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class SnvExecuteJob extends AbstractExecuteRoddyPipelineJob implements SnvWorkflowShared {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    SnvWorkFileService snvWorkFileService

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getSnvInstance(workflowStep)
    }

    @Override
    protected String getRoddyWorkflowName() {
        return 'SNVCallingWorkflow'
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'snvCallingAnalysis'
    }

    @Override
    protected boolean getFilenameSectionKillSwitch() {
        return false
    }

    @Override
    protected Map<String, Map<String, String>> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        RoddySnvCallingInstance roddySnvCallingInstance = getSnvInstance(workflowStep)

        Path resultDirectory = snvWorkFileService.getDirectoryPath(roddySnvCallingInstance)

        AbstractBamFile bamFileControl = roddySnvCallingInstance.sampleType2BamFile

        ReferenceGenome referenceGenome = roddySnvCallingInstance.referenceGenome
        File referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        File chromosomeLengthFile = referenceGenomeService.chromosomeLengthFile(bamFileControl.mergingWorkPackage)

        Path individualPath = individualService.getViewByPidPath(roddySnvCallingInstance.individual, roddySnvCallingInstance.seqType)

        Map<String, Map<String, String>> additionalValues = [
                REFERENCE_GENOME          : roddyConfigValueService.createPathValueMap(referenceGenomeFastaFile.path.toString()),
                CHROMOSOME_LENGTH_FILE    : roddyConfigValueService.createPathValueMap(chromosomeLengthFile.path.toString()),
                CHR_SUFFIX                : roddyConfigValueService.createValueMap(referenceGenome.chromosomeSuffix),
                CHR_PREFIX                : roddyConfigValueService.createValueMap(referenceGenome.chromosomePrefix),
                analysisMethodNameOnOutput: roddyConfigValueService.createValueMap(individualPath.relativize(resultDirectory).toString()),
        ]

        return roddyConfigValueService.getAnalysisInputVersion1(roddySnvCallingInstance) + additionalValues +
                roddyConfigValueService.getChromosomeIndexParameterWithoutMitochondrion(roddySnvCallingInstance.referenceGenome)
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath) {
    }
}

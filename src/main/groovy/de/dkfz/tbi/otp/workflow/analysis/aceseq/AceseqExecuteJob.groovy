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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.jobs.AbstractExecuteRoddyPipelineJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

@Component
@Slf4j
class AceseqExecuteJob extends AbstractExecuteRoddyPipelineJob implements AceseqWorkflowShared {

    @Autowired
    AceseqWorkFileService aceseqWorkFileService

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Override
    protected RoddyResult getRoddyResult(WorkflowStep workflowStep) {
        return getAceseqInstance(workflowStep)
    }

    @Override
    protected String getRoddyWorkflowName() {
        return 'ACEseqWorkflow'
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'copyNumberEstimationAnalysis'
    }

    @Override
    protected boolean getFilenameSectionKillSwitch() {
        return false
    }

    @Override
    protected Map<String, Map<String, String>> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        AceseqInstance aceseqInstance = getAceseqInstance(workflowStep)

        Path workDirectory = aceseqWorkFileService.getDirectoryPath(aceseqInstance)

        AbstractBamFile bamFileDisease = aceseqInstance.sampleType1BamFile

        ReferenceGenome referenceGenome = bamFileDisease.referenceGenome
        Path referenceGenomeFastaFile = referenceGenomeService.fastaFilePath(referenceGenome)
        Path chromosomeLengthFile = referenceGenomeService.chromosomeLengthFile(bamFileDisease.mergingWorkPackage)
        Path gcContentFile = referenceGenomeService.gcContentFile(bamFileDisease.mergingWorkPackage)

        Map<String, Map<String, String>> additionalValues = [
                REFERENCE_GENOME              : roddyConfigValueService.createPathValueMap(referenceGenomeFastaFile.toString()),
                CHROMOSOME_LENGTH_FILE        : roddyConfigValueService.createPathValueMap(chromosomeLengthFile.toString()),
                CHR_SUFFIX                    : roddyConfigValueService.createValueMap(referenceGenome.chromosomeSuffix),
                CHR_PREFIX                    : roddyConfigValueService.createValueMap(referenceGenome.chromosomePrefix),

                aceseqOutputDirectory         : roddyConfigValueService.createPathValueMap(workDirectory.toString()),
                svOutputDirectory             : roddyConfigValueService.createPathValueMap(workDirectory.toString()),
                MAPPABILITY_FILE              : roddyConfigValueService.createPathValueMap(referenceGenome.mappabilityFile),
                REPLICATION_TIME_FILE         : roddyConfigValueService.createPathValueMap(referenceGenome.replicationTimeFile),
                GC_CONTENT_FILE               : roddyConfigValueService.createPathValueMap(gcContentFile.toString()),
                GENETIC_MAP_FILE              : roddyConfigValueService.createPathValueMap(referenceGenome.geneticMapFile),
                KNOWN_HAPLOTYPES_FILE         : roddyConfigValueService.createPathValueMap(referenceGenome.knownHaplotypesFile),
                KNOWN_HAPLOTYPES_LEGEND_FILE  : roddyConfigValueService.createPathValueMap(referenceGenome.knownHaplotypesLegendFile),
                GENETIC_MAP_FILE_X            : roddyConfigValueService.createPathValueMap(referenceGenome.geneticMapFileX),
                KNOWN_HAPLOTYPES_FILE_X       : roddyConfigValueService.createPathValueMap(referenceGenome.knownHaplotypesFileX),
                KNOWN_HAPLOTYPES_LEGEND_FILE_X: roddyConfigValueService.createPathValueMap(referenceGenome.knownHaplotypesLegendFileX),
        ]

        return roddyConfigValueService.getAnalysisInputVersion1(aceseqInstance) + additionalValues
    }

    @Override
    protected List<String> getAdditionalParameters(WorkflowStep workflowStep) {
        return []
    }

    @Override
    protected void createAdditionalConfigFiles(WorkflowStep workflowStep, Path configPath) {
    }
}

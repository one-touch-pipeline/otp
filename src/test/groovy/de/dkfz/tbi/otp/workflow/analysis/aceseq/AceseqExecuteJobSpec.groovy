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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AceseqDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path

class AceseqExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    AceseqExecuteJob job
    AceseqInstance instance
    WorkflowStep workflowStep

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AceseqInstance,
                FastqFile,
                FastqImportInstance,
                FileType,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyMergedBamQa,
                RoddyWorkflowConfig,
                SampleTypePerProject,
        ]
    }

    void setupDataForGetConfigurationValues() {
        instance = AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : AceseqDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        job = new AceseqExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }
        job.aceseqWorkFileService = Mock(AceseqWorkFileService) {
            getDirectoryPath(instance) >> tempDir
        }
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.abstractBamFileServiceFactoryService = Mock(AbstractBamFileServiceFactoryService) {
            getService(_) >> Mock(AbstractAbstractBamFileService) {
                getPathForFurtherProcessing(instance.sampleType1BamFile) >> tempDir.resolve('bam1')
                getPathForFurtherProcessing(instance.sampleType2BamFile) >> tempDir.resolve('bam2')
            }
        }
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(instance.sampleType1BamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeLengthFile(instance.sampleType1BamFile.mergingWorkPackage) >> { new File("/chr-length-path") }
            gcContentFile(instance.sampleType1BamFile.mergingWorkPackage) >> { new File("/gc-path") }
        }
    }

    void "test getRoddyResult"() {
        given:
        AceseqInstance instance = AceseqDomainFactory.INSTANCE.createInstanceWithRoddyBamFiles()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : AceseqDomainFactory.INSTANCE.findOrCreateWorkflow(),
                ]),
        ])

        AceseqExecuteJob job = new AceseqExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, "ANALYSIS_OUTPUT") >> instance
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == instance
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new AceseqExecuteJob().roddyWorkflowName == "ACEseqWorkflow"
    }

    void "test getAnalysisConfiguration"() {
        expect:
        new AceseqExecuteJob().getAnalysisConfiguration(createSeqType()) == 'copyNumberEstimationAnalysis'
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        !(new AceseqExecuteJob().filenameSectionKillSwitch)
    }

    void "test getConfigurationValues"() {
        given:
        setupDataForGetConfigurationValues()

        expect:
        TestCase.assertContainSame(job.getConfigurationValues(workflowStep, "{}"), [
                bamfile_list                     : "${tempDir.resolve('bam2')};${tempDir.resolve('bam1')}",
                possibleControlSampleNamePrefixes: instance.sampleType2BamFile.sampleType.dirName,
                possibleTumorSampleNamePrefixes  : instance.sampleType1BamFile.sampleType.dirName,
                sample_list                      : "${instance.sampleType2BamFile.sampleType.dirName};${instance.sampleType1BamFile.sampleType.dirName}",
                REFERENCE_GENOME                 : '/fasta-path',
                CHROMOSOME_LENGTH_FILE           : '/chr-length-path',
                GC_CONTENT_FILE                  : '/gc-path',
                CHR_SUFFIX                       : instance.sampleType1BamFile.referenceGenome.chromosomeSuffix,
                CHR_PREFIX                       : instance.sampleType1BamFile.referenceGenome.chromosomePrefix,
                MAPPABILITY_FILE                 : instance.sampleType1BamFile.referenceGenome.mappabilityFile,
                REPLICATION_TIME_FILE            : instance.sampleType1BamFile.referenceGenome.replicationTimeFile,
                GENETIC_MAP_FILE                 : instance.sampleType1BamFile.referenceGenome.geneticMapFile,
                KNOWN_HAPLOTYPES_FILE            : instance.sampleType1BamFile.referenceGenome.knownHaplotypesFile,
                KNOWN_HAPLOTYPES_LEGEND_FILE     : instance.sampleType1BamFile.referenceGenome.knownHaplotypesLegendFile,
                GENETIC_MAP_FILE_X               : instance.sampleType1BamFile.referenceGenome.geneticMapFileX,
                KNOWN_HAPLOTYPES_FILE_X          : instance.sampleType1BamFile.referenceGenome.knownHaplotypesFileX,
                KNOWN_HAPLOTYPES_LEGEND_FILE_X   : instance.sampleType1BamFile.referenceGenome.knownHaplotypesLegendFileX,
                aceseqOutputDirectory            : tempDir.toString(),
                svOutputDirectory                : tempDir.toString(),
        ])
    }

    void "test getAdditionalParameters"() {
        expect:
        new AceseqExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}

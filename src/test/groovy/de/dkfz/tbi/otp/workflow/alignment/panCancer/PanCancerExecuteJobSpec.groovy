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
package de.dkfz.tbi.otp.workflow.alignment.panCancer

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.PanCancerWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*
import java.nio.file.Path
import java.nio.file.Paths

class PanCancerExecuteJobSpec extends Specification implements DataTest, PanCancerWorkflowDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    PanCancerExecuteJob job
    RoddyBamFile roddyBamFile
    WorkflowStep workflowStep

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BedFile,
                FastqFile,
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingPriority,
                Project,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                SeqType,
                Workflow,
        ]
    }

    void setupDataForGetConfigurationValues() {
        roddyBamFile = createBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])

        job = new PanCancerExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> roddyBamFile
            0 * _
        }

        job.processingOptionService = new ProcessingOptionService()
        job.bedFileService = Mock(BedFileService)
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
        }
        job.roddyConfigValueService.rawSequenceDataViewFileService = new RawSequenceDataViewFileService()
        job.roddyConfigValueService.rawSequenceDataViewFileService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> { Paths.get("/viewbypidpath") }
        }

        DomainFactory.createRoddyAlignableSeqTypes()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.toString(),
        ])

        DomainFactory.createProcessingOptionBasePathReferenceGenome(tempDir.resolve("reference_genomes").toString())

        DomainFactory.createBedFile([referenceGenome: roddyBamFile.referenceGenome, libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit])
    }

    void cleanup() {
        configService?.clean()
    }

    void "test getRoddyResult"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreatePanCancerWorkflow(),
                ]),
        ])

        PanCancerExecuteJob job = new PanCancerExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, PanCancerWorkflow.OUTPUT_BAM) >> bamFile
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == bamFile
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new PanCancerExecuteJob().roddyWorkflowName == "AlignmentAndQCWorkflows"
    }

    void "test getAnalysisConfiguration"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()

        expect:
        new PanCancerExecuteJob().getAnalysisConfiguration(seqTypeClosure()) == result

        where:
        // created objects in where part are not deleted during cleanup (in integration tests), hence we use closures for consistency also in unit test
        result          || seqTypeClosure
        "qcAnalysis"    || { DomainFactory.createChipSeqType() }
        "qcAnalysis"    || { DomainFactory.createWholeGenomeSeqType() }
        "exomeAnalysis" || { DomainFactory.createExomeSeqType() }
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        new PanCancerExecuteJob().filenameSectionKillSwitch
    }

    void "test getConfigurationValues, with exome seq. type"() {
        given:
        setupDataForGetConfigurationValues()
        job.bedFileService.filePath(_) >> { BedFile bedFile ->
            return "BedFilePath"
        }

        SeqType exomeSeqType = DomainFactory.createExomeSeqType()
        roddyBamFile.mergingWorkPackage.seqType = exomeSeqType
        roddyBamFile.mergingWorkPackage.save(flush: true)

        Map<String, String> expectedCommand = [
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
                "TARGET_REGIONS_FILE"              : "BedFilePath",
                "TARGETSIZE"                       : "1",
                "fastq_list"                       : fastqFilesAsString(roddyBamFile),
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getConfigurationValues, with whole genome seq. type, with fingerprinting"() {
        given:
        setupDataForGetConfigurationValues()
        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        referenceGenome.save(flush: true)

        job.roddyConfigValueService.referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome) >> { new File("/fingerprint-path") }

        Map<String, String> expectedCommand = [
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "true",
                "fingerprintingSitesFile"          : "/fingerprint-path",
                "fastq_list"                       : fastqFilesAsString(roddyBamFile),
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getConfigurationValues, with whole genome seq. type"() {
        given:
        setupDataForGetConfigurationValues()

        Map<String, String> expectedCommand = [
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
                "fastq_list"                       : fastqFilesAsString(roddyBamFile),
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getAdditionalParameters"() {
        expect:
        new PanCancerExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.seqTracks.collectMany { SeqTrack seqTrack ->
            RawSequenceFile.findAllBySeqTrack(seqTrack).collect { RawSequenceFile rawSequenceFile ->
                job.roddyConfigValueService.rawSequenceDataViewFileService.getFilePath(rawSequenceFile).toString()
            }
        }.join(';')
    }
}

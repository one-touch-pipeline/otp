/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Paths

class PanCancerExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Rule
    TemporaryFolder tmpDir

    PanCancerExecuteJob job
    RoddyBamFile roddyBamFile
    WorkflowStep workflowStep

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BedFile,
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingPriority,
                Project,
                Realm,
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
                fileOperationStatus         : AbstractMergedBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        workflowStep = createWorkflowStep()

        job = Spy(PanCancerExecuteJob) {
            getRoddyBamFile(workflowStep) >> roddyBamFile
        }

        job.bedFileService = Mock(BedFileService)
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.roddyConfigValueService.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage) >> { new File("/chrom-size-path") }
        }
        job.roddyConfigValueService.lsdfFilesService = new LsdfFilesService()
        job.roddyConfigValueService.lsdfFilesService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> { Paths.get("/viewbypidpath") }
        }

        DomainFactory.createRoddyAlignableSeqTypes()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tmpDir.root.path,
        ])

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(tmpDir.root, "reference_genomes").path)

        DomainFactory.createBedFile([referenceGenome: roddyBamFile.referenceGenome, libraryPreparationKit: roddyBamFile.mergingWorkPackage.libraryPreparationKit])
    }

    void cleanup() {
        configService?.clean()
    }

    void "test getRoddyResult"() {
        given:
        RoddyBamFile bamFile = createBamFile()
        WorkflowStep workflowStep = createWorkflowStep()
        PanCancerExecuteJob job = Spy(PanCancerExecuteJob) {
            1 * getRoddyBamFile(workflowStep) >> bamFile
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
        new PanCancerExecuteJob().getAnalysisConfiguration(seqType) == result

        where:
        seqType                                || result
        DomainFactory.createChipSeqType        || "qcAnalysis"
        DomainFactory.createWholeGenomeSeqType || "qcAnalysis"
        DomainFactory.createExomeSeqType       || "exomeAnalysis"
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

    void "test getConfigurationValues, with whole genome seq. type, without base bam file, with fingerprinting"() {
        given:
        setupDataForGetConfigurationValues()
        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        referenceGenome.save(flush: true)

        job.roddyConfigValueService.referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome) >> { new File("/fingerprint-path") }

        Map<String, String> expectedCommand = [
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

    void "test getConfigurationValues, with whole genome seq. type, without base bam file"() {
        given:
        setupDataForGetConfigurationValues()

        Map<String, String> expectedCommand = [
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

    void "test getConfigurationValues, with whole genome seq. type, with base bam file"() {
        given:
        setupDataForGetConfigurationValues()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileSize = roddyBamFile.workBaiFile.size()
        roddyBamFile.save(flush: true)

        roddyBamFile.mergingWorkPackage.bamFileInProjectFolder = roddyBamFile
        roddyBamFile.mergingWorkPackage.save(flush: true)

        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)

        WorkflowStep workflowStep2 = createWorkflowStep()
        job.getRoddyBamFile(workflowStep2) >> { roddyBamFile2 }

        Map<String, String> expectedCommand = [
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile2.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
                "fastq_list"                       : fastqFilesAsString(roddyBamFile2),
                "bam"                              : roddyBamFile.workBamFile.path,
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep2, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getAdditionalParameters"() {
        expect:
        new PanCancerExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.seqTracks.collectMany { SeqTrack seqTrack ->
            DataFile.findAllBySeqTrack(seqTrack).collect { DataFile dataFile ->
                job.roddyConfigValueService.lsdfFilesService.getFileViewByPidPathAsPath(dataFile).toString()
            }
        }.join(';')
    }
}

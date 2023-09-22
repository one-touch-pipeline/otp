/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.rna

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

class RnaAlignmentExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, RoddyRnaFactory {

    @TempDir
    Path tempDir

    RnaAlignmentExecuteJob job
    RnaRoddyBamFile roddyBamFile
    WorkflowStep workflowStep

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RnaRoddyBamFile,
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

    static final String ADAPTER_SEQUENCE = "dummy adapter sequence"
    static final String WORK_DIRECTORY_NAME = "work"

    void setupDataForGetConfigurationValues() {
        roddyBamFile = createBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                workDirectoryName           : WORK_DIRECTORY_NAME,
                seqTracks                   : [createSeqTrack([
                        libraryPreparationKit: createLibraryPreparationKit([
                                reverseComplementAdapterSequence: ADAPTER_SEQUENCE,
                        ])
                ])],
        ])
        workflowStep = createWorkflowStep()

        job = Spy(RnaAlignmentExecuteJob) {
            getRoddyBamFile(workflowStep) >> roddyBamFile
            getWorkDirectory(workflowStep) >> tempDir
        }

        job.processingOptionService = new ProcessingOptionService()
        job.roddyConfigValueService = new RoddyConfigValueService()

        job.roddyConfigValueService.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage) >> { new File("/chrom-size-path") }
        }
        job.roddyConfigValueService.lsdfFilesService = new LsdfFilesService()
        job.roddyConfigValueService.lsdfFilesService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> { Paths.get("/viewbypidpath") }
        }

        DomainFactory.createRnaAlignableSeqTypes()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.toString(),
        ])

        DomainFactory.createProcessingOptionBasePathReferenceGenome(tempDir.resolve("reference_genomes").toString())
    }

    void cleanup() {
        configService?.clean()
    }

    void "test getRoddyResult"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RnaAlignmentExecuteJob job = Spy(RnaAlignmentExecuteJob) {
            1 * getRoddyBamFile(workflowStep) >> roddyBamFile
        }

        expect:
        job.getRoddyResult(workflowStep) == roddyBamFile
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new RnaAlignmentExecuteJob().roddyWorkflowName == "RNAseqWorkflow"
    }

    void "test getAnalysisConfiguration"() {
        given:
        DomainFactory.createRnaAlignableSeqTypes()

        expect:
        new RnaAlignmentExecuteJob().getAnalysisConfiguration(seqTypeClosure()) == result

        where:
        // created objects in where part are not deleted during cleanup (in integration tests), hence we use closures for consistency also in unit test
        result           || seqTypeClosure
        "RNAseqAnalysis" || { DomainFactory.createRnaPairedSeqType() }
        "RNAseqAnalysis" || { DomainFactory.createRnaSingleSeqType() }
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        new RnaAlignmentExecuteJob().filenameSectionKillSwitch == false
    }

    @Unroll
    void "test getConfigurationValues, with RNA alignment specific configs"() {
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
                "ADAPTER_SEQ"                      : ADAPTER_SEQUENCE,
                "ALIGNMENT_DIR"                    : tempDir.toString(),
                "outputBaseDirectory"              : tempDir.toString(),
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getAdditionalParameters"() {
        expect:
        new RnaAlignmentExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }

    private String fastqFilesAsString(RoddyBamFile roddyBamFileToUse = roddyBamFile) {
        return roddyBamFileToUse.seqTracks.collectMany { SeqTrack seqTrack ->
            RawSequenceFile.findAllBySeqTrack(seqTrack).collect { RawSequenceFile rawSequenceFile ->
                job.roddyConfigValueService.lsdfFilesService.getFileViewByPidPathAsPath(rawSequenceFile).toString()
            }
        }.join(';')
    }
}

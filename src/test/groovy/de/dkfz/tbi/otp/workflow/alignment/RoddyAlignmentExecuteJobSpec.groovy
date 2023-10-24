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
package de.dkfz.tbi.otp.workflow.alignment

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

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
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentExecuteJob
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

class RoddyAlignmentExecuteJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @TempDir
    Path tempDir

    RoddyAlignmentExecuteJob job
    RoddyBamFile roddyBamFile
    WorkflowStep workflowStep

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                RoddyBamFile,
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

    static final String WORK_DIRECTORY_NAME = "work"

    void setupDataForGetConfigurationValues() {
        roddyBamFile = createBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
                workDirectoryName           : WORK_DIRECTORY_NAME,
                seqTracks                   : [createSeqTrack([
                        libraryPreparationKit: createLibraryPreparationKit()
                ])],
        ])
        workflowStep = createWorkflowStep()

        job = Spy(RoddyAlignmentExecuteJob) {
            getRoddyBamFile(workflowStep) >> roddyBamFile
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

        DomainFactory.createRoddyAlignableSeqTypes()

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
        new RoddyAlignmentExecuteJob().roddyWorkflowName == "AlignmentAndQCWorkflows"
    }

    void "test getAnalysisConfiguration"() {
        given:
        DomainFactory.createRoddyAlignableSeqTypes()

        expect:
        new RoddyAlignmentExecuteJob().getAnalysisConfiguration(seqTypeClosure()) == result

        where:
        // created objects in where part are not deleted during cleanup (in integration tests), hence we use closures for consistency also in unit test
        result          || seqTypeClosure
        "qcAnalysis"    || { DomainFactory.createChipSeqType() }
        "qcAnalysis"    || { DomainFactory.createWholeGenomeSeqType() }
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        new RoddyAlignmentExecuteJob().filenameSectionKillSwitch == false
    }

    void "test getConfigurationValues, with general alignment configs"() {
        given:
        setupDataForGetConfigurationValues()

        Map<String, String> expectedCommand = [
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(expectedCommand, actualCommand)
    }

    void "test getAdditionalParameters"() {
        expect:
        new RoddyAlignmentExecuteJob().getAdditionalParameters(createWorkflowStep()) == []
    }
}

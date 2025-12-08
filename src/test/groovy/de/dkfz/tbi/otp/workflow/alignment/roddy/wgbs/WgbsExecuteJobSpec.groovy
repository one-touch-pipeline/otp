/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WgbsAlignmentWorkflowDomainFactory
import de.dkfz.tbi.otp.infrastructure.alignment.WgbsAlignmentWorkFileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path
import java.nio.file.Paths

class WgbsExecuteJobSpec extends Specification implements DataTest, WgbsAlignmentWorkflowDomainFactory, IsRoddy {

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
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                SeqType,
                Workflow,
        ]
    }

    @TempDir
    Path tempDir

    WgbsExecuteJob job
    RoddyBamFile roddyBamFile
    WorkflowStep workflowStep

    TestConfigService configService

    void setupDataForGetConfigurationValues() {
        roddyBamFile = createBamFile([
                md5sum                      : null,
                fileOperationStatus         : AbstractBamFile.FileOperationStatus.DECLARED,
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflowVersion: null,
                        workflow       : findOrCreateWgbsAlignmenWorkflow(),
                ]),
        ])

        job = new WgbsExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> roddyBamFile
            0 * _
        }

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.toString(),
        ])

        job.processingOptionService = new ProcessingOptionService()
        job.bedFileService = Mock(BedFileService)
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { tempDir.resolve("fasta-path").toAbsolutePath().toFile() }
            cytosinePositionIndexFilePath(roddyBamFile.referenceGenome) >>
                    { Paths.get(tempDir.toString(), "cytosine-position-index-path").toAbsolutePath().toFile() }
        }
        job.roddyConfigValueService.referenceGenomeService = job.referenceGenomeService

        DomainFactory.createRoddyAlignableSeqTypes()

        DomainFactory.createProcessingOptionBasePathReferenceGenome(new File(tempDir.toString(), "reference_genomes").path)
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
                        workflow       : findOrCreateWgbsAlignmenWorkflow(),
                ]),
        ])

        WgbsExecuteJob job = new WgbsExecuteJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(workflowStep, WgbsWorkflow.OUTPUT_BAM) >> bamFile
            0 * _
        }

        expect:
        job.getRoddyResult(workflowStep) == bamFile
    }

    void "test getRoddyWorkflowName"() {
        expect:
        new WgbsExecuteJob().roddyWorkflowName == "AlignmentAndQCWorkflows"
    }

    void "test getAnalysisConfiguration"() {
        expect:
        new WgbsExecuteJob().getAnalysisConfiguration(createSeqType()) == "bisulfiteCoreAnalysis"
    }

    void "test getFileNamesKillSwitch"() {
        expect:
        new WgbsExecuteJob().filenameSectionKillSwitch
    }

    void "test getConfigurationValues"() {
        given:
        setupDataForGetConfigurationValues()

        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, ["adsf"])

        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.cytosinePositionsIndex = "cytosinePositionsIndex"
        referenceGenome.save(flush: true)

        job.roddyConfigValueService.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        Map<String, String> expectedCommand = [
                sharedFilesBaseDirectory         : [value: null, type: "path"],
                INDEX_PREFIX                     : [value: tempDir.resolve("fasta-path").toString(), type: "path"],
                GENOME_FA                        : [value: tempDir.resolve("fasta-path").toString(), type: "path"],
                possibleControlSampleNamePrefixes: [value: roddyBamFile.sampleType.dirName],
                possibleTumorSampleNamePrefixes  : [value: ""],
                runFingerprinting                : [value: "false", type: "boolean"],
                CHROMOSOME_INDICES               : [value: "( adsf )", type: "bashArray"],
                CYTOSINE_POSITIONS_INDEX         : [value: tempDir.resolve("cytosine-position-index-path").toString(), type: "path"],
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(actualCommand, expectedCommand)
    }

    void "test getConfigurationValues, with fingerprinting"() {
        given:
        setupDataForGetConfigurationValues()
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, ["adsf"])

        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.cytosinePositionsIndex = "cytosinePositionsIndex"
        referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        referenceGenome.save(flush: true)

        job.roddyConfigValueService.referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome) >> { tempDir.resolve("fingerprint-path").toAbsolutePath().toFile() }
        job.roddyConfigValueService.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        Map<String, String> expectedCommand = [
                sharedFilesBaseDirectory         : [value: null, type: "path"],
                INDEX_PREFIX                     : [value: tempDir.resolve("fasta-path").toString(), type: "path"],
                GENOME_FA                        : [value: tempDir.resolve("fasta-path").toString(), type: "path"],
                possibleControlSampleNamePrefixes: [value: roddyBamFile.sampleType.dirName],
                possibleTumorSampleNamePrefixes  : [value: ""],
                runFingerprinting                : [value: "true", type: "boolean"],
                fingerprintingSitesFile          : [value: tempDir.resolve("fingerprint-path").toString(), type: "path"],
                CHROMOSOME_INDICES               : [value: "( adsf )", type: "bashArray"],
                CYTOSINE_POSITIONS_INDEX         : [value: tempDir.resolve("cytosine-position-index-path").toString(), type: "path"],
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        TestCase.assertContainSame(actualCommand, expectedCommand)
    }

    void "test getAdditionalParameters"() {
        given:
        setupDataForGetConfigurationValues()
        job.wgbsAlignmentWorkFileService = Mock(WgbsAlignmentWorkFileService) {
            getMetadataTableFile(_) >> Paths.get("/asdf")
        }

        expect:
        job.getAdditionalParameters(workflowStep) == ["--usemetadatatable=${File.separator}asdf"]
    }
}

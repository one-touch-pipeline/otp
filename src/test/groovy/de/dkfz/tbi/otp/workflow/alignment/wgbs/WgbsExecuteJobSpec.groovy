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
package de.dkfz.tbi.otp.workflow.alignment.wgbs

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WgbsAlignmentWorkflowDomainFactory
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

        job.processingOptionService = new ProcessingOptionService()
        job.bedFileService = Mock(BedFileService)
        job.roddyConfigValueService = new RoddyConfigValueService()
        job.referenceGenomeService = Mock(ReferenceGenomeService) {
            fastaFilePath(roddyBamFile.referenceGenome) >> { new File("/fasta-path") }
            chromosomeStatSizeFile(roddyBamFile.mergingWorkPackage) >> { new File("/chrom-size-path") }
            cytosinePositionIndexFilePath(roddyBamFile.referenceGenome) >> { new File("/cytosine-position-index-path") }
        }
        job.roddyConfigValueService.referenceGenomeService = job.referenceGenomeService

        DomainFactory.createRoddyAlignableSeqTypes()

        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT): tempDir.toString(),
        ])

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
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "false",
                "CHROMOSOME_INDICES"               : "( adsf )",
                "CYTOSINE_POSITIONS_INDEX"         : "/cytosine-position-index-path",
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        new HashMap(expectedCommand) == new HashMap(actualCommand)
    }

    void "test getConfigurationValues, with fingerprinting"() {
        given:
        setupDataForGetConfigurationValues()
        DomainFactory.createReferenceGenomeEntries(roddyBamFile.referenceGenome, ["adsf"])

        ReferenceGenome referenceGenome = roddyBamFile.referenceGenome
        referenceGenome.cytosinePositionsIndex = "cytosinePositionsIndex"
        referenceGenome.fingerPrintingFileName = "fingerprintingFile"
        referenceGenome.save(flush: true)

        job.roddyConfigValueService.referenceGenomeService.fingerPrintingFile(roddyBamFile.referenceGenome) >> { new File("/fingerprint-path") }
        job.roddyConfigValueService.chromosomeIdentifierSortingService = new ChromosomeIdentifierSortingService()

        Map<String, String> expectedCommand = [
                "sharedFilesBaseDirectory"         : null,
                "INDEX_PREFIX"                     : "/fasta-path",
                "GENOME_FA"                        : "/fasta-path",
                "possibleControlSampleNamePrefixes": roddyBamFile.sampleType.dirName,
                "possibleTumorSampleNamePrefixes"  : "",
                "runFingerprinting"                : "true",
                "fingerprintingSitesFile"          : "/fingerprint-path",
                "CHROMOSOME_INDICES"               : "( adsf )",
                "CYTOSINE_POSITIONS_INDEX"         : "/cytosine-position-index-path",
        ]

        when:
        Map<String, String> actualCommand = job.getConfigurationValues(workflowStep, "{}")

        then:
        new HashMap(expectedCommand) == new HashMap(actualCommand)
    }

    void "test getAdditionalParameters"() {
        given:
        setupDataForGetConfigurationValues()
        job.roddyBamFileService = Mock(RoddyBamFileService) {
            getWorkMetadataTableFile(_) >> Paths.get("/asdf")
        }

        expect:
        job.getAdditionalParameters(workflowStep) == ["--usemetadatatable=/asdf"]
    }
}

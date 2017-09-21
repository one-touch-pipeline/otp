package workflows

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import org.junit.*

import java.nio.file.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

abstract class AbstractRnaAlignmentWorkflowTests extends AbstractRoddyAlignmentWorkflowTests {

    @Test
    void testAlignLanesOnly_NoBaseBamExist_OneLane_allFine() {
        createProjectConfigRna(exactlyOneElement(MergingWorkPackage.findAll()), [:])
        createSeqTrack("readGroup1")

        // run
        execute()

        // check
        checkWorkPackageState()

        RoddyBamFile bamFile = exactlyOneElement(RnaRoddyBamFile.findAll())
        checkFirstBamFileState(bamFile, true)
        assertBamFileFileSystemPropertiesSet(bamFile)

        checkFileSystemState(bamFile)
        checkFileSystemStateRna(bamFile)

        checkQcRna(bamFile)
    }

    @Test
    void testAlignLanesOnly_withoutArribaProcessing_NoBaseBamExist_OneLane_allFine() {
        createProjectConfigRna(exactlyOneElement(MergingWorkPackage.findAll()), [
                configVersion: "v2_0",
        ], [
                mouseData: true,

        ])
        createSeqTrack("readGroup1")

        // run
        execute()

        // check
        checkWorkPackageState()

        RoddyBamFile bamFile = exactlyOneElement(RnaRoddyBamFile.findAll())
        checkFirstBamFileState(bamFile, true)
        assertBamFileFileSystemPropertiesSet(bamFile)

        checkFileSystemState(bamFile)
        checkFileSystemStateRna(bamFile)

        checkQcRna(bamFile)
    }

    static void checkFileSystemStateRna(RnaRoddyBamFile bamFile) {
        [
                bamFile.workDirectory,
                bamFile.workQADirectory,
                bamFile.workExecutionStoreDirectory,
                bamFile.workMergedQADirectory,
        ].each {
            assert it.exists() && it.isDirectory() && it.canRead() && it.canExecute()
        }

        [
                bamFile.finalQADirectory,
                bamFile.finalExecutionStoreDirectory,
                bamFile.finalBamFile,
                bamFile.finalBaiFile,
                bamFile.finalMd5sumFile,
                bamFile.finalMergedQADirectory,
        ].each {
            assert it.exists() && Files.isSymbolicLink(it.toPath()) && it.canRead()
        }

        [
                bamFile.workBamFile,
                bamFile.workBaiFile,
                bamFile.workMd5sumFile,
                bamFile.correspondingWorkChimericBamFile,
                bamFile.workMergedQAJsonFile,
        ].each {
            assert it.exists() && it.isFile() && it.canRead() && it.size() > 0
        }
    }

    static void checkQcRna(RnaRoddyBamFile bamFile) {
        QualityAssessmentMergedPass qaPass = QualityAssessmentMergedPass.findWhere(
                abstractMergedBamFile: bamFile,
                identifier: 0,
        )
        assert qaPass

        RnaQualityAssessment rnaQa = RnaQualityAssessment.findByQualityAssessmentMergedPass(qaPass)
        assert rnaQa

        JSON.parse(bamFile.getFinalMergedQAJsonFile().text)
        assert bamFile.getFinalMergedQAJsonFile().text.trim() != ""

        assert bamFile.coverage == null

        assert bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }


    Pipeline findPipeline() {
        DomainFactory.createRnaPipeline()
    }

    @Override
    void createProjectConfig(MergingWorkPackage workPackage, Map options = [:]) {}

    void createProjectConfigRna(MergingWorkPackage workPackage, Map configOptions = [:], Map referenceGenomeConfig = [:]) {

        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(workPackage.project)), realm)

        GeneModel geneModel = DomainFactory.createGeneModel(
                referenceGenome: workPackage.referenceGenome,
                path: "gencode19",
                fileName: "gencode.v19.annotation_plain.gtf",
                excludeFileName: "gencode.v19.annotation_plain.chrXYMT.rRNA.tRNA.gtf",
                dexSeqFileName: "gencode.v19.annotation_plain.dexseq.gff",
                gcFileName: "gencode.v19.annotation_plain.transcripts.autosomal_transcriptTypeProteinCoding_nonPseudo.1KGRef.gc",
        )

        ToolName toolNameGatk = DomainFactory.createToolName(
                name: "GENOME_GATK_INDEX",
                path: "gatk",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameStar200 = DomainFactory.createToolName(
                name: "GENOME_STAR_INDEX_200",
                path: "star_200",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameKallisto = DomainFactory.createToolName(
                name: ProjectService.GENOME_KALLISTO_INDEX,
                path: "kallisto",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameArribaFusions = DomainFactory.createToolName(
                name: ProjectService.ARRIBA_KNOWN_FUSIONS,
                path: "arriba-fusion",
                type: ToolName.Type.RNA,
        )
        ToolName toolNameArribaBlacklist = DomainFactory.createToolName(
                name: ProjectService.ARRIBA_BLACKLIST,
                path: "arriba-blacklist",
                type: ToolName.Type.RNA,
        )

        ReferenceGenomeIndex referenceGenomeIndexGatk = DomainFactory.createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameGatk,
                path: "hs37d5_PhiX.fa",
        )
        ReferenceGenomeIndex referenceGenomeIndexStar200 = DomainFactory.createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameStar200,
                path: "STAR_2.5.2b_1KGRef_PhiX_Gencode19_200bp",
        )
        ReferenceGenomeIndex referenceGenomeIndexKallisto = DomainFactory.createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameKallisto,
                path: "kallisto-0.43.0_1KGRef_Gencode19_k31.index",
        )
        ReferenceGenomeIndex referenceGenomeIndexArribaFusions = DomainFactory.createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameArribaFusions,
                path: "known_fusions_CancerGeneCensus_gencode19_2017-01-16.tsv.gz",
        )
        ReferenceGenomeIndex referenceGenomeIndexArribaBlacklist = DomainFactory.createReferenceGenomeIndex(
                referenceGenome: workPackage.referenceGenome,
                toolName: toolNameArribaBlacklist,
                path: "blacklist_hs37d5_gencode19_2017-01-09.tsv.gz",
        )

        SpringSecurityUtils.doWithAuth("operator") {
            projectService.configureRnaAlignmentConfig(new RoddyConfiguration([
                    project          : workPackage.project,
                    seqType          : workPackage.seqType,
                    pluginName       : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_NAME, workPackage.seqType.roddyName, null),
                    pluginVersion    : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_PLUGIN_VERSION, workPackage.seqType.roddyName, null),
                    baseProjectConfig: ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_BASE_PROJECT_CONFIG, workPackage.seqType.roddyName, null),
                    configVersion    : "v1_0",
                    resources        : "t",
            ] + configOptions))
            projectService.configureRnaAlignmentReferenceGenome(new RnaAlignmentReferenceGenomeConfiguration([
                    project          : workPackage.project,
                    seqType          : workPackage.seqType,
                    referenceGenome  : workPackage.referenceGenome,
                    geneModel        : geneModel,
                    referenceGenomeIndex: [
                            referenceGenomeIndexGatk,
                            referenceGenomeIndexStar200,
                            referenceGenomeIndexKallisto,
                            referenceGenomeIndexArribaFusions,
                            referenceGenomeIndexArribaBlacklist,
                    ],
            ] + referenceGenomeConfig))

            workPackage.alignmentProperties = ReferenceGenomeProjectSeqType.getConfiguredReferenceGenomeProjectSeqType(workPackage.project, workPackage.seqType, workPackage.sampleType).alignmentProperties.collect {
                new MergingWorkPackageAlignmentProperty(name: it.name, value: it.value, mergingWorkPackage: workPackage)
            }
            workPackage.save(flush: true)
        }
        assert ReferenceGenomeProjectSeqTypeAlignmentProperty.list().size() >= 1
        assert MergingWorkPackageAlignmentProperty.list().size() >= 1
    }


    void setUpFilesVariables() {
        File baseTestDataDirRNA = new File(rootDirectory, 'RNATestFiles')
        testFastqFiles = [
                readGroup1: [
                        new File(baseTestDataDirRNA, 'rna_100k_reads_L001_R1_complete_filtered.fastq.gz'),
                        new File(baseTestDataDirRNA, 'rna_100k_reads_L001_R2_complete_filtered.fastq.gz'),
                ].asImmutable(),
                readGroup2: [
                        new File(baseTestDataDirRNA, 'rna_100k_reads_L002_R1_complete_filtered.fastq.gz'),
                        new File(baseTestDataDirRNA, 'rna_100k_reads_L002_R2_complete_filtered.fastq.gz'),
                ].asImmutable(),
        ].asImmutable()
        baseTestDataDir = new File(rootDirectory, 'PanCanAlignmentSetupFiles')
        firstBamFile = new File(baseTestDataDir, 'first-bam-file/first-bam-file_merged.mdup.bam')
        refGenDir = new File(getReferenceGenomeDirectory(), referenceGenomeSpecificPath)
        chromosomeNamesFile = new File(baseTestDataDir, 'reference-genomes/chromosome-names.txt')
        fingerPrintingFile = new File(baseTestDataDir, 'fingerPrinting/snp138Common.n1000.vh20140318.bed')
    }

    @Override
    public String getRefGenFileNamePrefix() {
        return 'hs37d5_PhiX'
    }

    @Override
    protected String getReferenceGenomeSpecificPath() {
        'bwa06_1KGRef_PhiX'
    }


    @Override
    protected String getChromosomeStatFileName() {
        return null
    }

    @Override
    List<String> getWorkflowScripts() {
        ["scripts/workflows/RnaAlignmentWorkflow.groovy",
         "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
         "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
         "scripts/initializations/RoddyProcessingOptions.groovy",
         "scripts/initializations/LoadSoftwareModules.groovy",
        ]
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(5)
    }
}

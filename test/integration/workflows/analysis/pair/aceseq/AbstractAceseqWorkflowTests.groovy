package workflows.analysis.pair.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.joda.time.Duration
import workflows.analysis.pair.*

abstract class AbstractAceseqWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<AceseqInstance> {


    ConfigService configService

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createAceseqPipelineLazy()
        DomainFactory.createAceseqSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(project)), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureAceseqPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_VERSION, null, null),
                            baseProjectConfig: ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_BASE_PROJECT_CONFIG, seqType.roddyName, null),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        referenceGenome.gcContentFile = 'hg19_GRch37_100genomes_gc_content_10kb.txt'

        File referenceGenomePath = new File (referenceGenomeService.referenceGenomeDirectory(referenceGenome, false), 'databases')
        referenceGenome.geneticMapFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/genetic_map_chr${CHR_NAME}_combined_b37.txt').absolutePath
        referenceGenome.geneticMapFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/genetic_map_chrX_nonPAR_combined_b37.txt').absolutePath
        referenceGenome.knownHaplotypesFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.haplotypes.gz').absolutePath
        referenceGenome.knownHaplotypesFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.hap.gz').absolutePath
        referenceGenome.knownHaplotypesLegendFile = new File(referenceGenomePath, 'IMPUTE/ALL.integrated_phase1_SHAPEIT_16-06-14.nomono/ALL.chr${CHR_NAME}.integrated_phase1_v3.20101123.snps_indels_svs.genotypes.nomono.legend.gz').absolutePath
        referenceGenome.knownHaplotypesLegendFileX = new File(referenceGenomePath, 'IMPUTE/ALL_1000G_phase1integrated_v3_impute/ALL_1000G_phase1integrated_v3_chrX_nonPAR_impute.legend.gz').absolutePath
        referenceGenome.mappabilityFile = new File(referenceGenomePath, 'UCSC/wgEncodeCrgMapabilityAlign100mer_chr.bedGraph.gz').absolutePath
        referenceGenome.replicationTimeFile = new File(referenceGenomePath, 'ENCODE/ReplicationTime_10cellines_mean_10KB.Rda').absolutePath
        referenceGenome.save(flush: true)

        SpringSecurityUtils.doWithAuth("operator") {
            processingOptionService.createOrUpdate(
                    AceseqService.PROCESSING_OPTION_REFERENCE_KEY,
                    null,
                    null,
                    referenceGenome.name,
                    "Name of reference genomes for aceseq",
            )
        }

        return referenceGenome
    }


    void createSophiaInput() {
        File sourceSophiaInputFile = new File(workflowData, "svs_stds_filtered_somatic_minEventScore3.tsv")

        SophiaInstance sophiaInstance = DomainFactory.createSophiaInstance(samplePair)
        File sophiaInputFile = sophiaInstance.finalAceseqInputFile
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        assert samplePair.save(flush: true)

        linkFileUtils.createAndValidateLinks([
                (sourceSophiaInputFile): sophiaInputFile,
        ], realm)
    }


    void executeTest() {
        createSophiaInput()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyAceseqWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
                "scripts/initializations/CreateAceseqPipelineOptions.groovy",
        ]
    }

    List<File> filesToCheck(AceseqInstance aceseqInstance) {
        return aceseqInstance.getAllFiles()
    }

    @Override
    void checkAnalysisSpecific(AceseqInstance aceseqInstance) {
        assert AceseqQc.countByAceseqInstance(aceseqInstance) > 0
    }

    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'aceseq')
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(24)
    }
}

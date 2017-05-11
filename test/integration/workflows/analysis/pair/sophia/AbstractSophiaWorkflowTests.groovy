package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.joda.time.Duration
import workflows.analysis.pair.*

abstract class AbstractSophiaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<SophiaInstance> {


    ConfigService configService

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createSophiaSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(project)), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureSophiaPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SOPHIA_PIPELINE_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SOPHIA_PIPELINE_PLUGIN_VERSION, null, null),
                            baseProjectConfig: ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SOPHIA_BASE_PROJECT_CONFIG, seqType.roddyName, null),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        SpringSecurityUtils.doWithAuth("operator") {
            processingOptionService.createOrUpdate(
                    SophiaService.PROCESSING_OPTION_REFERENCE_KEY,
                    null,
                    null,
                    referenceGenome.name,
                    "Name of reference genomes for sophia",
            )
        }

        return referenceGenome
    }


    void linkQualityControlFiles() {
        File tumorInsertSizeFile = new File(workflowData, "tumor_insertsize_plot.png_qcValues.txt")
        File controlInsertSizeFile = new File(workflowData, "control_insertsize_plot.png_qcValues.txt")

        File finalTumorInsertSizeFile = bamFileTumor.getFinalInsertSizeFile()
        File finalControlInsertSizeFile = bamFileControl.getFinalInsertSizeFile()

        linkFileUtils.createAndValidateLinks([
                (tumorInsertSizeFile): finalTumorInsertSizeFile,
                (controlInsertSizeFile): finalControlInsertSizeFile,
        ], realm)
    }


    void executeTest() {
        linkQualityControlFiles()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySophiaWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
                "scripts/initializations/CreateSophiaPipelineOptions.groovy",
        ]
    }

    @Override
    List<File> filesToCheck(SophiaInstance sophiaInstance) {
        return [sophiaInstance.getFinalAceseqInputFile()]
    }

    @Override
    void checkAnalysisSpecific(SophiaInstance sophiaInstance) {

    }

    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'sophia')
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(5)
    }

}

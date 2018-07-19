package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import workflows.analysis.pair.*

abstract class AbstractSophiaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<SophiaInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createSophiaPipelineLazy()
        DomainFactory.createSophiaSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(configService.getProjectSequencePath(project), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureSophiaPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_SOPHIA_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_SOPHIA_PLUGIN_VERSIONS, null, null),
                            baseProjectConfig: ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_SOPHIA_BASE_PROJECT_CONFIG, seqType.roddyName, null),
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
                    ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                    null,
                    null,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }


    void linkQualityControlFiles() {
        File tumorInsertSizeFile = new File(workflowData, "tumor_insertsize_plot.png_qcValues.txt")
        File controlInsertSizeFile = new File(workflowData, "control_insertsize_plot.png_qcValues.txt")

        File finalTumorInsertSizeFile = bamFileTumor.getFinalInsertSizeFile()
        File finalControlInsertSizeFile = bamFileControl.getFinalInsertSizeFile()

        LogThreadLocal.withThreadLog(System.out) {
            linkFileUtils.createAndValidateLinks([
                    (tumorInsertSizeFile)  : finalTumorInsertSizeFile,
                    (controlInsertSizeFile): finalControlInsertSizeFile,
            ], realm)
        }
    }


    void executeTest() {
        linkQualityControlFiles()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySophiaWorkflow.groovy",
                "scripts/initializations/RoddyOptions.groovy",
                "scripts/initializations/SophiaPipelineOptions.groovy",
        ]
    }

    @Override
    List<File> filesToCheck(SophiaInstance sophiaInstance) {
        return [
                sophiaInstance.getFinalAceseqInputFile(),
                sophiaInstance.getQcJsonFile(),
                sophiaInstance.getCombinedPlotPath(),
        ]
    }

    @Override
    void checkAnalysisSpecific(SophiaInstance sophiaInstance) {
        CollectionUtils.exactlyOneElement(SophiaQc.findAllBySophiaInstance(sophiaInstance))
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

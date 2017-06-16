package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.joda.time.*
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
                            pluginName       : ProcessingOptionService.findOption(Names.pipelineSophiaPluginName, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(Names.pipelineSophiaPluginVersions, null, null),
                            baseProjectConfig: ProcessingOptionService.findOption(Names.pipelineSophiaBaseProjectConfig, seqType.roddyName, null),
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
                    Names.pipelineSophiaReferenceGenome,
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
        return [sophiaInstance.getFinalAceseqInputFile(), sophiaInstance.getQcJsonFile()]
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

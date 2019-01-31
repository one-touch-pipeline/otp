package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import workflows.analysis.pair.*

import java.time.*

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
        lsdfFilesService.createDirectory(project.projectSequencingDirectory, realm)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            config = projectService.configureSophiaPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME),
                            pluginVersion    : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            processingOptionService.createOrUpdate(
                    ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME,
                    referenceGenome.name
            )
        }

        return referenceGenome
    }


    void linkQualityControlFiles() {
        File tumorInsertSizeFile = new File(workflowData, "tumor_HCC1187-div128_insertsize_plot.png_qcValues.txt")
        File controlInsertSizeFile = new File(workflowData, "blood_HCC1187-div128_insertsize_plot.png_qcValues.txt")

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
    void setupExternalBamFile() {
        super.setupExternalBamFile()
        DomainFactory.createExternalProcessedMergedBamFileQualityAssessment(QC_VALUES, bamFileControl)
        DomainFactory.createExternalProcessedMergedBamFileQualityAssessment(QC_VALUES, bamFileTumor)
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySophiaWorkflow.groovy",
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
        new File(getInputRootDirectory(), 'sophia')
    }

    @Override
    Duration getTimeout() {
        Duration.ofHours(5)
    }

}

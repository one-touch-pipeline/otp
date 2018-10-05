package workflows.analysis.pair.sophia

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import org.junit.*
import workflows.analysis.pair.*

abstract class AbstractSophiaWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<SophiaInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService

    //Sophia do not work with external bam files, since the needed qa values are not available,
    //TODO: OTP-2950: After solving OTP-2950 the test should be re-enable (remove this overriding of the superclass.
    @Ignore
    @Override
    void testWholeWorkflowWithExternalBamFile() {

    }

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

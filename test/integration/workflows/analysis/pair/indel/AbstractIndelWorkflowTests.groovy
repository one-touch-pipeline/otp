package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import org.joda.time.*
import workflows.analysis.pair.*

abstract class AbstractIndelWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<IndelCallingInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createIndelSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(project.projectSequencingDirectory, realm)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            config = projectService.configureIndelPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_NAME),
                            pluginVersion    : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyIndelWorkflow.groovy",
        ]
    }

    List<File> filesToCheck(IndelCallingInstance indelCallingInstance) {
        return [
                indelCallingInstance.getResultFilePathsToValidate(),
                indelCallingInstance.getCombinedPlotPath(),
                indelCallingInstance.getIndelQcJsonFile(),
                indelCallingInstance.getSampleSwapJsonFile(),
        ].flatten()
    }


    @Override
    File getWorkflowData() {
        new File(getInputRootDirectory(), 'indel')
    }

    @Override
    void checkAnalysisSpecific(IndelCallingInstance indelCallingInstance) {
        CollectionUtils.exactlyOneElement(IndelQualityControl.findAllByIndelCallingInstance(indelCallingInstance))
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(indelCallingInstance))
    }

    @Override
    Duration getTimeout() {
        Duration.standardHours(5)
    }
}

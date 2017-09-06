package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.junit.*
import workflows.analysis.pair.*

abstract class AbstractIndelWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<IndelCallingInstance> {

    ConfigService configService

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessMergedBamFile()

        executeTest()
    }


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createIndelSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(project)), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureIndelPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_PLUGIN_VERSION, seqType.roddyName, null),
                            baseProjectConfig: ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_PLUGIN_CONFIG, seqType.roddyName, null),
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
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
                "scripts/initializations/CreateRoddyIndelPipelineOptions.groovy",
        ]
    }

    List<File> filesToCheck(IndelCallingInstance indelCallingInstance) {
        return [
                indelCallingInstance.getResultFilePathsToValidate(),
                indelCallingInstance.getCombinedPlotPath(),
        ].flatten()
    }


    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'indel')
    }
}

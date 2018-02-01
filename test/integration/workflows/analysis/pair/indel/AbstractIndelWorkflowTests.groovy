package workflows.analysis.pair.indel

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.plugin.springsecurity.*
import htsjdk.samtools.util.CollectionUtil
import org.junit.*
import workflows.analysis.pair.*

abstract class AbstractIndelWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<IndelCallingInstance> {

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createIndelPipelineLazy()
        DomainFactory.createIndelSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(project).path), realm)

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
                "scripts/initializations/RoddyOptions.groovy",
                "scripts/initializations/CreateRoddyIndelPipelineOptions.groovy",
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
        new File(getDataDirectory(), 'indel')
    }

    @Override
    void checkAnalysisSpecific(IndelCallingInstance indelCallingInstance) {
        CollectionUtils.exactlyOneElement(IndelQualityControl.findAllByIndelCallingInstance(indelCallingInstance))
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.findAllByIndelCallingInstance(indelCallingInstance))
    }
}

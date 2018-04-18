package workflows.analysis.pair.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.plugin.springsecurity.*
import org.junit.*
import workflows.analysis.pair.*

abstract class AbstractSnvWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<RoddySnvCallingInstance> {

    LsdfFilesService lsdfFilesService
    ProjectService projectService
    ProcessingOptionService processingOptionService


    @Test
    void testWholeWorkflowWithProcessedMergedBamFile() {
        setupProcessedMergedBamFile()

        executeTest()
    }


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createSnvSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(configService.getProjectSequencePath(project), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureSnvPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_PLUGIN_VERSION, seqType.roddyName, null),
                            baseProjectConfig: ProcessingOptionService.findOption(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_BASE_PROJECT_CONFIG, seqType.roddyName, null),
                            configVersion    : 'v1_0',
                            resources        : 't',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        return createAndSetup_Bwa06_1K_ReferenceGenome()
    }

    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddySnvWorkflow.groovy",
                "scripts/initializations/RoddyOptions.groovy",
                "scripts/initializations/SnvPipelineOptions.groovy",
        ]
    }

    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'snv')
    }

    @Override
    List<File> filesToCheck(RoddySnvCallingInstance instance) {
        return [
                instance.getSnvCallingResult(),
                instance.getSnvDeepAnnotationResult(),
        ]
    }
}

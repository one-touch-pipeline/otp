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
    ConfigPerProjectAndSeqType createConfig() {
        DomainFactory.createRoddySnvPipelineLazy()
        DomainFactory.createSnvSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(configService.getProjectSequencePath(project), realm)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            config = projectService.configureSnvPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME),
                            pluginVersion    : processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION, seqType.roddyName),
                            baseProjectConfig: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG, seqType.roddyName),
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

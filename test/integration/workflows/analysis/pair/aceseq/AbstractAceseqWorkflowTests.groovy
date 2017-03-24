package workflows.analysis.pair.aceseq

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.plugin.springsecurity.*
import workflows.analysis.pair.*

abstract class AbstractAceseqWorkflowTests extends AbstractRoddyBamFilePairAnalysisWorkflowTests<AceseqInstance> {


    ConfigService configService

    LsdfFilesService lsdfFilesService

    ProjectService projectService

    ProcessingOptionService processingOptionService


    @Override
    ConfigPerProject createConfig() {
        DomainFactory.createAceseqPipelineLazy()
        DomainFactory.createAceseqSeqTypes()
        DomainFactory.createReferenceGenomeProjectSeqType(
                referenceGenome: referenceGenome,
                project: project,
                seqType: seqType,
        )
        lsdfFilesService.createDirectory(new File(configService.getProjectSequencePath(project)), realm)

        SpringSecurityUtils.doWithAuth("operator") {
            config = projectService.configureAceseqPipelineProject(
                    new RoddyConfiguration([
                            project          : project,
                            seqType          : seqType,
                            pluginName       : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_NAME, null, null),
                            pluginVersion    : ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_PIPELINE_PLUGIN_VERSION, null, null),
                            baseProjectConfig: ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_ACESEQ_BASE_PROJECT_CONFIG, seqType.roddyName, null),
                            configVersion    : 'v1_0',
                    ])
            )
        }
    }

    @Override
    ReferenceGenome createReferenceGenome() {
        ReferenceGenome referenceGenome = super.createReferenceGenome()

        referenceGenome.gcContentFile = 'hg19_GRch37_100genomes_gc_content_10kb.txt'

        //TODO: OTP-2510: fill with correct values after OTP-2510
        File referenceGenomePath = getBaseDirectory()
        referenceGenome.geneticMapFile = CreateFileHelper.createFile(new File(referenceGenomePath, 'geneticMapFile.file')).absolutePath
        referenceGenome.geneticMapFileX = CreateFileHelper.createFile(new File(referenceGenomePath, 'geneticMapFileX.file')).absolutePath
        referenceGenome.knownHaplotypesFile = CreateFileHelper.createFile(new File(referenceGenomePath, 'knownHaplotypesFile.file')).absolutePath
        referenceGenome.knownHaplotypesFileX = CreateFileHelper.createFile(new File(referenceGenomePath, 'knownHaplotypesFileX.file')).absolutePath
        referenceGenome.knownHaplotypesLegendFile = CreateFileHelper.createFile(new File(referenceGenomePath, 'knownHaplotypesLegendFile.file')).absolutePath
        referenceGenome.knownHaplotypesLegendFileX = CreateFileHelper.createFile(new File(referenceGenomePath, 'knownHaplotypesLegendFileX.file')).absolutePath
        referenceGenome.mappabilityFile = CreateFileHelper.createFile(new File(referenceGenomePath, 'mappabilityFile.file')).absolutePath
        referenceGenome.replicationTimeFile = CreateFileHelper.createFile(new File(referenceGenomePath, 'replicationTimeFile.file')).absolutePath
        referenceGenome.save(flush: true)

        SpringSecurityUtils.doWithAuth("operator") {
            processingOptionService.createOrUpdate(
                    AceseqService.PROCESSING_OPTION_REFERENCE_KEY,
                    null,
                    null,
                    referenceGenome.name,
                    "Name of reference genomes for aceseq",
            )
        }

        return referenceGenome
    }


    void createSophieInput() {
        //TODO: adapt next line for using sophia input file
        File sophiaInputFile = new File(project.getProjectDirectory(), 'testpid.DELLY.somaticFilter.highConf.bedpe.txt')
        String cmd = "touch ${sophiaInputFile.absolutePath} && chmod 664 ${sophiaInputFile.absolutePath} && echo OK"
        assert executionService.executeCommandReturnProcessOutput(realm, cmd).assertExitCodeZeroAndStderrEmpty().stdout.trim() == 'OK'
    }


    void executeTest() {
        createSophieInput()

        super.executeTest()
    }


    @Override
    List<String> getWorkflowScripts() {
        return [
                "scripts/workflows/RoddyAceseqWorkflow.groovy",
                "scripts/initializations/AddPathToConfigFilesToProcessingOptions.groovy",
                "scripts/initializations/AddRoddyPathAndVersionToProcessingOptions.groovy",
                "scripts/initializations/CreateAceseqPipelineOptions.groovy",
        ]
    }

    List<File> filesToCheck(AceseqInstance aceseqInstance) {
        return aceseqInstance.getAllFiles()
    }

    @Override
    void checkAnalysisSpecific(AceseqInstance aceseqInstance) {
        assert AceseqQc.countByAceseqInstance(aceseqInstance) > 0
    }

    @Override
    File getWorkflowData() {
        new File(getDataDirectory(), 'aceseq')
    }
}

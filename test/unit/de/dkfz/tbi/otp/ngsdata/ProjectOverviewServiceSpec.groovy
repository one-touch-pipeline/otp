package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.apache.commons.logging.impl.*
import spock.lang.*

@Mock([
        Individual,
        Pipeline,
        Project,
        ProjectCategory,
        ProcessingOption,
        RoddyWorkflowConfig,
        SeqType,
])
class ProjectOverviewServiceSpec extends Specification {


    void "getDefaultOtpAlignmentInformation, when project is null, throw assert"() {
        when:
        new ProjectOverviewService().getDefaultOtpAlignmentInformation(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert project')
    }


    void "getDefaultOtpAlignmentInformation, when project is given, return OTP default alignment info"() {
        given:
        Project project = new Project()
        Map map = DomainFactory.createOtpAlignmentProcessingOptions()
        ProjectOverviewService service = new ProjectOverviewService([
                processingOptionService: new ProcessingOptionService(),
        ])

        when:
        ProjectOverviewService.AlignmentInfo alignmentInfo = service.getDefaultOtpAlignmentInformation(project)

        then:
        alignmentInfo
        map[ProjectOverviewService.BWA_COMMAND] + ' aln' == alignmentInfo.bwaCommand
        map[ProjectOverviewService.BWA_Q_PARAMETER] == alignmentInfo.bwaOptions
        map[ProjectOverviewService.SAM_TOOLS_COMMAND] == alignmentInfo.samToolsCommand
        map[ProjectOverviewService.PICARD_MDUP_COMMAND] == alignmentInfo.mergeCommand
        map[ProjectOverviewService.PICARD_MDUP_OPTIONS] == alignmentInfo.mergeOptions
    }


    void "getRoddyAlignmentInformation, when roddyWorkflowConfig is null, throw assert"() {
        when:
        new ProjectOverviewService().getRoddyAlignmentInformation(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert workflowConfig')
    }


    @Unroll
    void "getRoddyAlignmentInformation, when useConfig is #useConvey and mergeTool is #mergeTool, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        ProjectOverviewService service = new ProjectOverviewService([
                processingOptionService   : new ProcessingOptionService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
        ])

        GroovyMock(ProcessHelperService, global: true)
        1 * ProcessHelperService.executeAndWait(_) >> {
            new ProcessHelperService.ProcessOutput("""
useAcceleratedHardware=${useConvey}
markDuplicatesVariant=${mergeTool}

BWA_BINARY=nonConvey
BWA_MEM_OPTIONS=alnOpt

BWA_ACCELERATED_BINARY=convey
BWA_MEM_CONVEY_ADDITIONAL_OPTIONS=conveyOpt

MARKDUPLICATES_BINARY=bioBamBam
mergeAndRemoveDuplicates_argumentList=bioBamBamOpt

PICARD_BINARY=picard

SAMBAMBA_MARKDUP_BINARY=sambamba
SAMBAMBA_MARKDUP_OPTS=sambambaOpt

SAMTOOLS_BINARY=samTool

""", '', 0)
        }

        when:
        ProjectOverviewService.AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        alignmentInfo
        alignCommand == alignmentInfo.bwaCommand
        alignOpt == alignmentInfo.bwaOptions
        mergeCommand == alignmentInfo.mergeCommand
        mergeOpt == alignmentInfo.mergeOptions
        'samTool' == alignmentInfo.samToolsCommand

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)

        where:
        useConvey | mergeTool                               || alignCommand || alignOpt           || mergeCommand || mergeOpt
        false     | MergeConstants.MERGE_TOOL_BIOBAMBAM     || 'nonConvey'  || 'alnOpt'           || 'bioBamBam'  || 'bioBamBamOpt'
        false     | MergeConstants.MERGE_TOOL_PICARD        || 'nonConvey'  || 'alnOpt'           || 'picard'     || ''
        false     | MergeConstants.MERGE_TOOL_SAMBAMBA      || 'nonConvey'  || 'alnOpt'           || 'sambamba'   || 'sambambaOpt'
        true      | MergeConstants.MERGE_TOOL_BIOBAMBAM     || 'convey'     || 'alnOpt conveyOpt' || 'bioBamBam'  || 'bioBamBamOpt'
    }


    void "getRoddyAlignmentInformation, when rna, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(
                seqType: DomainFactory.createRnaSeqType(),
                adapterTrimmingNeeded: true,
        )
        ProjectOverviewService service = new ProjectOverviewService([
                processingOptionService   : new ProcessingOptionService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
        ])

        GroovyMock(ProcessHelperService, global: true)
        1 * ProcessHelperService.executeAndWait(_) >> {
            new ProcessHelperService.ProcessOutput("""
SAMTOOLS_BINARY=samTool
SAMBAMBA_BINARY=SAMBAMBA_BINARY
STAR_BINARY=STAR_BINARY

${['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
    "STAR_PARAMS_${name}=${name}"
}.join('\n')
}
""", '', 0)

        }

        when:
        ProjectOverviewService.AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        'STAR_BINARY'  == alignmentInfo.bwaCommand
        ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
            "${name}"
        }.join(' ') == alignmentInfo.bwaOptions
        'SAMBAMBA_BINARY' == alignmentInfo.mergeCommand
        ''  == alignmentInfo.mergeOptions
        'samTool' == alignmentInfo.samToolsCommand

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }

    @Unroll
    void "getRoddyAlignmentInformation, when roddy fail for #name, throw Exception"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        ProjectOverviewService service = new ProjectOverviewService([
                processingOptionService   : new ProcessingOptionService(),
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
                log                       : new NoOpLog(),
        ])

        GroovyMock(ProcessHelperService, global: true)
        1 * ProcessHelperService.executeAndWait(_) >> {
            new ProcessHelperService.ProcessOutput(stdout, stderr.replaceFirst('roddyWorkflowConfig', roddyWorkflowConfig.nameUsedInConfig), exitcode)
        }
        String expectedError = expectedErrorTemplate.replaceFirst('roddyWorkflowConfig', roddyWorkflowConfig.nameUsedInConfig)

        when:
        service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        Exception e = thrown()
        e.message.contains(expectedError)

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)

        where:
        name                  | stdout                                          | stderr                                                                      | exitcode || expectedErrorTemplate
        'roddy error'         | 'some'                                          | 'some'                                                                      | 1        || 'Alignment information can\'t be detected. Is Roddy with support for printidlessruntimeconfig installed?'
        'file not found'      | 'some'                                          | 'The project configuration "roddyWorkflowConfig.config" could not be found' | 0        || 'Roddy could not find the configuration \'roddyWorkflowConfig\'. Probably some access problem.'
        'no config values'    | 'some'                                          | 'some'                                                                      | 0        || 'Could not extract any configuration value from the roddy output'
        'no alignment values' | 'a=b\nc=d'                                      | 'some'                                                                      | 0        || 'Could not extract alignment configuration value from the roddy output'
        'no merging values'   | 'useAcceleratedHardware=false\nBWA_BINARY=algn' | 'some'                                                                      | 0        || 'Could not extract merging configuration value from the roddy output'
    }


    void "getAlignmentInformationFromConfig, when config is null, throw assert"() {
        when:
        new ProjectOverviewService().getAlignmentInformationFromConfig(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert config')
    }


    void "getAlignmentInformationFromConfig, when config is instance of RoddyWorkflowConfig, then call getRoddyAlignmentInformation"() {
        given:
        ProjectOverviewService.AlignmentInfo alignmentInfo = new ProjectOverviewService.AlignmentInfo()
        ProjectOverviewService projectOverviewService = Spy(ProjectOverviewService) {
            1 * getRoddyAlignmentInformation(_) >> alignmentInfo
            0 * getDefaultOtpAlignmentInformation(_)
        }

        expect:
        alignmentInfo == projectOverviewService.getAlignmentInformationFromConfig(new RoddyWorkflowConfig())
    }


    void "getAlignmentInformationFromConfig, when config is instance of Project, then call getDefaultOtpAlignmentInformation"() {
        given:
        ProjectOverviewService.AlignmentInfo alignmentInfo = new ProjectOverviewService.AlignmentInfo()
        ProjectOverviewService projectOverviewService = Spy(ProjectOverviewService) {
            1 * getDefaultOtpAlignmentInformation(_) >> alignmentInfo
            0 * getRoddyAlignmentInformation(_)
        }

        expect:
        alignmentInfo == projectOverviewService.getAlignmentInformationFromConfig(new Project())
    }

}

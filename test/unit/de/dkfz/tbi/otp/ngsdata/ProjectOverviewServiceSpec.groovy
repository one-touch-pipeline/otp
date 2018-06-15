package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
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
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyWorkflowConfig,
        SeqType,
])
class ProjectOverviewServiceSpec extends Specification {


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
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(["pluginVersion": "pluginVersion:1.1.0"])
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

                BWA_VERSION=1.0
                BWA_MEM_OPTIONS=alnOpt

                BWA_ACCELERATED_VERSION=2.0
                BWA_MEM_CONVEY_ADDITIONAL_OPTIONS=conveyOpt

                BIOBAMBAM_VERSION=3.0
                mergeAndRemoveDuplicates_argumentList=bioBamBamOpt

                PICARD_VERSION=4.0

                SAMBAMBA_MARKDUP_VERSION=5.0
                SAMBAMBA_MARKDUP_OPTS=sambambaOpt

                SAMTOOLS_VERSION=6.0

                """.stripIndent(), '', 0)
        }

        when:
        ProjectOverviewService.AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        alignmentInfo
        alignCommand == alignmentInfo.bwaCommand
        alignOpt == alignmentInfo.bwaOptions
        mergeCommand == alignmentInfo.mergeCommand
        mergeOpt == alignmentInfo.mergeOptions
        'Version 6.0' == alignmentInfo.samToolsCommand
        roddyPipelineVersion == alignmentInfo.pluginVersion

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)

        where:
        useConvey | mergeTool                           || alignCommand         || alignOpt           || mergeCommand                              || mergeOpt       || roddyPipelineVersion
        false     | MergeConstants.MERGE_TOOL_BIOBAMBAM || 'BWA Version 1.0'    || 'alnOpt'           || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'pluginVersion:1.1.0'
        false     | MergeConstants.MERGE_TOOL_PICARD    || 'BWA Version 1.0'    || 'alnOpt'           || 'Picard Version 4.0'                      || ''             || 'pluginVersion:1.1.0'
        false     | MergeConstants.MERGE_TOOL_SAMBAMBA  || 'BWA Version 1.0'    || 'alnOpt'           || 'Sambamba Version 5.0'                    || 'sambambaOpt'  || 'pluginVersion:1.1.0'
        true      | MergeConstants.MERGE_TOOL_BIOBAMBAM || 'bwa-bb Version 2.0' || 'alnOpt conveyOpt' || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'pluginVersion:1.1.0'
    }


    void "getRoddyAlignmentInformation, when rna, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(
                seqType: DomainFactory.createRnaPairedSeqType(),
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
                |SAMTOOLS_VERSION=1.0
                |SAMBAMBA_VERSION=3.0
                |STAR_VERSION=2.0

                |${
                ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name ->
                    "STAR_PARAMS_${name}=${name}"
                }.join('\n')
            }
            """.stripMargin(), '', 0)
        }

        when:
        ProjectOverviewService.AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        'STAR Version 2.0' == alignmentInfo.bwaCommand
        ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].join(' ') == alignmentInfo.bwaOptions
        'Sambamba Version 3.0' == alignmentInfo.mergeCommand
        '' == alignmentInfo.mergeOptions
        'Version 1.0' == alignmentInfo.samToolsCommand

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
        'no merging values'   | 'useAcceleratedHardware=false\nBWA_VERSION=aln' | 'some'                                                                      | 0        || 'Could not extract merging configuration value from the roddy output'
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


    void "listReferenceGenome, when one item, show one item in listing"() {
        given:
        ReferenceGenomeProjectSeqType rgpst = DomainFactory.createReferenceGenomeProjectSeqType()

        when:
        List<ReferenceGenomeProjectSeqType> list = new ProjectOverviewService().listReferenceGenome(rgpst.project)

        then:
        CollectionUtils.containSame(list, [rgpst])
    }


    void "listReferenceGenome, when searching other project, don't find this"() {
        given:
        Project otherProject = DomainFactory.createProject()
        ReferenceGenomeProjectSeqType rgpst = DomainFactory.createReferenceGenomeProjectSeqType()

        when:
        List<ReferenceGenomeProjectSeqType> list = new ProjectOverviewService().listReferenceGenome(otherProject)

        then:
        CollectionUtils.containSame(list, [])
    }


    void "listReferenceGenome, when searching deprecated reference genome, don't find it"() {
        given:
        ReferenceGenomeProjectSeqType rgpst = DomainFactory.createReferenceGenomeProjectSeqType([deprecatedDate: new Date()])

        when:
        List<ReferenceGenomeProjectSeqType> list = new ProjectOverviewService().listReferenceGenome(rgpst.project)

        then:
        CollectionUtils.containSame(list, [])
    }


    void "listReferenceGenome, when multiple items, show all of them"() {
        given:
        Project theProject = DomainFactory.createProject()
        ReferenceGenomeProjectSeqType rgpst1 = DomainFactory.createReferenceGenomeProjectSeqType([project: theProject])
        ReferenceGenomeProjectSeqType rgpst2 = DomainFactory.createReferenceGenomeProjectSeqType([project: theProject])
        ReferenceGenomeProjectSeqType rgpst3 = DomainFactory.createReferenceGenomeProjectSeqType([project: theProject])

        when:
        List<ReferenceGenomeProjectSeqType> list = new ProjectOverviewService().listReferenceGenome(theProject)

        then:
        CollectionUtils.containSame(list, [rgpst1, rgpst2, rgpst3])
    }

}

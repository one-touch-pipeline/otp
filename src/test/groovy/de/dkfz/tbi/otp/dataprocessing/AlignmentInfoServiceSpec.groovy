/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.*

class AlignmentInfoServiceSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Individual,
                Pipeline,
                ProcessingOption,
                Project,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyWorkflowConfig,
                WorkflowVersionSelector,
                SeqType,
                Workflow,
                WorkflowVersion,
        ]
    }

    void "getRoddyAlignmentInformation, throws AssertionError when WorkflowConfig is null"() {
        when:
        new AlignmentInfoService().getRoddyAlignmentInformation(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert workflowConfig')
    }

    @Unroll
    void "getRoddyAlignmentInformation, when useConfig is #useConvey and mergeTool is #mergeTool, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(["programVersion": "programVersion:1.1.0"])
        AlignmentInfoService service = new AlignmentInfoService([executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
            1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
            0 * roddyGetRuntimeConfigCommand(_, _, _)
        },])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_) >> {
                new ProcessOutput("""
                    useAcceleratedHardware=${useConvey}
                    markDuplicatesVariant=${mergeTool.name}

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
        }

        when:
        AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        alignmentInfo
        alignmentInfo.alignmentProgram == alignCommand
        alignmentInfo.alignmentParameter == alignOpt
        alignmentInfo.mergeCommand == mergeCommand
        alignmentInfo.mergeOptions == mergeOpt
        alignmentInfo.samToolsCommand == 'Version 6.0'
        alignmentInfo.programVersion == roddyPipelineVersion

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)

        where:
        useConvey | mergeTool           || alignCommand         || alignOpt           || mergeCommand                              || mergeOpt       || roddyPipelineVersion
        false     | MergeTool.BIOBAMBAM || 'BWA Version 1.0'    || 'alnOpt'           || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
        false     | MergeTool.PICARD    || 'BWA Version 1.0'    || 'alnOpt'           || 'Picard Version 4.0'                      || ''             || 'programVersion:1.1.0'
        false     | MergeTool.SAMBAMBA  || 'BWA Version 1.0'    || 'alnOpt'           || 'Sambamba Version 5.0'                    || 'sambambaOpt'  || 'programVersion:1.1.0'
        true      | MergeTool.BIOBAMBAM || 'bwa-bb Version 2.0' || 'alnOpt conveyOpt' || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
    }

    void "getRoddyAlignmentInformation, when rna, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(seqType: DomainFactory.createRnaPairedSeqType(),
                adapterTrimmingNeeded: true,)
        AlignmentInfoService service = new AlignmentInfoService([executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
            1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
            0 * roddyGetRuntimeConfigCommand(_, _, _)
        },])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_) >> {
                new ProcessOutput("""
                    |SAMTOOLS_VERSION=1.0
                    |SAMBAMBA_VERSION=3.0
                    |STAR_VERSION=2.0

                    |${['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].collect { name -> "STAR_PARAMS_${name}=${name}" }.join('\n')}
                """.stripMargin(), '', 0)
            }
        }

        when:
        AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        alignmentInfo.alignmentProgram == 'STAR Version 2.0'
        alignmentInfo.alignmentParameter == ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].join(' ')
        alignmentInfo.mergeCommand == 'Sambamba Version 3.0'
        alignmentInfo.mergeOptions == ''
        alignmentInfo.samToolsCommand == 'Version 1.0'

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
    }

    @Unroll
    void "getRoddyAlignmentInformation, when roddy fail for #name, throw Exception"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        AlignmentInfoService service = new AlignmentInfoService([executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
            1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
            0 * roddyGetRuntimeConfigCommand(_, _, _)
        },])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_) >> {
                new ProcessOutput(stdout, stderr.replaceFirst('roddyWorkflowConfig', roddyWorkflowConfig.nameUsedInConfig), exitcode)
            }
        }
        String expectedError = expectedErrorTemplate.replaceFirst('roddyWorkflowConfig', roddyWorkflowConfig.nameUsedInConfig)

        when:
        service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        Exception e = thrown()
        e.message.contains(expectedError)

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)

        where:
        name                  | stdout                                          | stderr                                                                      | exitcode || expectedErrorTemplate
        'roddy error'         | 'some'                                          | 'some'                                                                      | 1        || 'Alignment information can\'t be detected. Is Roddy with support for printidlessruntimeconfig installed?'
        'file not found'      | 'some'                                          | 'The project configuration "roddyWorkflowConfig.config" could not be found' | 0        || 'Roddy could not find the configuration \'roddyWorkflowConfig\'. Probably some access problem.'
        'no config values'    | 'some'                                          | 'some'                                                                      | 0        || 'Could not extract any configuration value from the roddy output'
        'no alignment values' | 'a=b\nc=d'                                      | 'some'                                                                      | 0        || 'Could not extract alignment configuration value from Roddy config'
        'no merging values'   | 'useAcceleratedHardware=false\nBWA_VERSION=aln' | 'some'                                                                      | 0        || 'Could not extract merging configuration value from Roddy config'
    }

    void "getAlignmentInformationFromConfig, when config is null, throw assert"() {
        when:
        new AlignmentInfoService().getAlignmentInformationFromConfig(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert config')
    }

    @Unroll
    void "getAlignmentInformationFromConfig, from Roddy WorkflowConfig and AlignmentInfo"() {
        given:
        AlignmentInfo alignmentInfo = new RoddyAlignmentInfo()
        AlignmentConfig alignmentConfig = new RoddyWorkflowConfig()

        AlignmentInfoService service = Spy(AlignmentInfoService) {
            1 * getRoddyAlignmentInformation(_) >> alignmentInfo
        }

        expect:
        alignmentInfo == service.getAlignmentInformationFromConfig(alignmentConfig)
    }

    @Unroll
    void "test getAlignmentInformationForProject, when workflow is PanCancer alignment, useConfig is #useConvey and mergeTool is #mergeTool, return alignment info with the correct data"() {
        given:
        String config = """{"RODDY": {"cvalues": {
        "useAcceleratedHardware": {"value": "${useConvey}", "type": "string"},
        "markDuplicatesVariant": {"value": "${mergeTool.name}", "type": "string"},
        "BWA_VERSION": {"value": "1.0", "type": "string"},
        "BWA_MEM_OPTIONS": {"value": "alnOpt", "type": "string"},
        "BWA_ACCELERATED_VERSION": {"value": "2.0", "type": "string"},
        "BWA_MEM_CONVEY_ADDITIONAL_OPTIONS": {"value": "conveyOpt", "type": "string"},
        "BIOBAMBAM_VERSION": {"value": "3.0", "type": "string"},
        "mergeAndRemoveDuplicates_argumentList": {"value": "bioBamBamOpt", "type": "string"},
        "PICARD_VERSION": {"value": "4.0", "type": "string"},
        "SAMBAMBA_MARKDUP_VERSION": {"value": "5.0", "type": "string"},
        "SAMBAMBA_MARKDUP_OPTS": {"value": "sambambaOpt", "type": "string"},
        "SAMTOOLS_VERSION": {"value": "6.0", "type": "string"}
        }}}"""
        AlignmentInfoService service = new AlignmentInfoService()
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }
        service.configFragmentService = Mock(ConfigFragmentService) {
            1 * getSortedFragments(_) >> _
            1 * mergeSortedFragments(_) >> config
        }

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        WorkflowVersion version = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: createWorkflow(name: PanCancerWorkflow.WORKFLOW)),
                workflowVersion: roddyPipelineVersion,)
        Project project = createProject()
        createWorkflowVersionSelector(project: project, seqType: seqType, workflowVersion: version)

        when:
        Map<SeqType, AlignmentInfo> alignmentInfos = service.getAlignmentInformationForProject(project)

        then:
        alignmentInfos.size() == 1
        RoddyAlignmentInfo alignmentInfo = alignmentInfos.get(seqType)
        alignmentInfo.alignmentProgram == alignCommand
        alignmentInfo.alignmentParameter == alignOpt
        alignmentInfo.mergeCommand == mergeCommand
        alignmentInfo.mergeOptions == mergeOpt
        alignmentInfo.samToolsCommand == 'Version 6.0'
        alignmentInfo.programVersion == roddyPipelineVersion

        cleanup:

        where:
        useConvey | mergeTool           || alignCommand         || alignOpt           || mergeCommand                              || mergeOpt       || roddyPipelineVersion
        false     | MergeTool.BIOBAMBAM || 'BWA Version 1.0'    || 'alnOpt'           || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
        false     | MergeTool.PICARD    || 'BWA Version 1.0'    || 'alnOpt'           || 'Picard Version 4.0'                      || ''             || 'programVersion:1.1.0'
        false     | MergeTool.SAMBAMBA  || 'BWA Version 1.0'    || 'alnOpt'           || 'Sambamba Version 5.0'                    || 'sambambaOpt'  || 'programVersion:1.1.0'
        true      | MergeTool.BIOBAMBAM || 'bwa-bb Version 2.0' || 'alnOpt conveyOpt' || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
    }

    void "test getAlignmentInformationForProject, when rna, return alignment info with the correct data"() {
        given:
        String config = """{"RODDY": {"cvalues": {
        "SAMTOOLS_VERSION": {"value": "1.0", "type": "string"},
        "SAMBAMBA_VERSION": {"value": "3.0", "type": "string"},
        "STAR_VERSION": {"value": "2.0", "type": "string"},
        "STAR_PARAMS_2PASS": {"value": "2PASS", "type": "string"},
        "STAR_PARAMS_OUT": {"value": "OUT", "type": "string"},
        "STAR_PARAMS_CHIMERIC": {"value": "CHIMERIC", "type": "string"},
        "STAR_PARAMS_INTRONS": {"value": "INTRONS", "type": "string"}
        }}}"""

        AlignmentInfoService service = new AlignmentInfoService()
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }
        service.configFragmentService = Mock(ConfigFragmentService) {
            getSortedFragments(_) >> _
            mergeSortedFragments(_) >> { config }
        }

        SeqType seqType = DomainFactory.createRnaPairedSeqType()
        WorkflowVersion version = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: createWorkflow(name: PanCancerWorkflow.WORKFLOW)))
        Project project = createProject()
        createWorkflowVersionSelector(project: project, seqType: seqType, workflowVersion: version)

        when:
        Map<SeqType, AlignmentInfo> alignmentInfos = service.getAlignmentInformationForProject(project)

        then:
        alignmentInfos.size() == 1
        RoddyAlignmentInfo alignmentInfo = alignmentInfos.get(seqType)
        alignmentInfo.alignmentProgram == 'STAR Version 2.0'
        alignmentInfo.alignmentParameter == ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].join(' ')
        alignmentInfo.mergeCommand == 'Sambamba Version 3.0'
        alignmentInfo.mergeOptions == ''
        alignmentInfo.samToolsCommand == 'Version 1.0'
    }

    @Unroll
    void "test getAlignmentInformationForProject, when #name, throw exception"() {
        given:
        AlignmentInfoService service = new AlignmentInfoService()
        service.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }
        service.configFragmentService = Mock(ConfigFragmentService) {
            getSortedFragments(_) >> _
            mergeSortedFragments(_) >> { """{"RODDY": {"cvalues": { ${config}}}}""" }
        }

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        WorkflowVersion version = createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: createWorkflow(name: PanCancerWorkflow.WORKFLOW)))
        Project project = createProject()
        createWorkflowVersionSelector(project: project, seqType: seqType, workflowVersion: version)

        when:
        service.getAlignmentInformationForProject(project)

        then:
        Exception e = thrown()
        e.message.contains(expectedError)

        where:
        name                  | config                                                                                                                 || expectedError
        'no alignment values' | """"a": {"value": "b", "type": "string"},"c": {"value": "d", "type": "string"}"""                                      || 'Could not extract alignment configuration value from Roddy config'
        'no merging values'   | """"useAcceleratedHardware": {"value": "false", "type": "string"},"BWA_VERSION": {"value": "aln", "type": "string"}""" || 'Could not extract merging configuration value from Roddy config'
    }

    void "test getAlignmentInformationForProject, from Roddy WorkflowConfig and AlignmentInfo"() {
        when:
        new AlignmentInfoService().getAlignmentInformationForProject(null)

        then:
        thrown(AssertionError)
    }

    void "listReferenceGenome, when one item, show one item in listing"() {
        given:
        ReferenceGenomeProjectSeqType rgpst = DomainFactory.createReferenceGenomeProjectSeqType()

        when:
        List<ReferenceGenomeProjectSeqType> list = new AlignmentInfoService().listReferenceGenome(rgpst.project)

        then:
        CollectionUtils.containSame(list, [rgpst])
    }

    void "listReferenceGenome, when searching other project, don't find this"() {
        given:
        Project otherProject = DomainFactory.createProject()
        DomainFactory.createReferenceGenomeProjectSeqType()

        when:
        List<ReferenceGenomeProjectSeqType> list = new AlignmentInfoService().listReferenceGenome(otherProject)

        then:
        CollectionUtils.containSame(list, [])
    }

    void "listReferenceGenome, when searching deprecated reference genome, don't find it"() {
        given:
        ReferenceGenomeProjectSeqType rgpst = DomainFactory.createReferenceGenomeProjectSeqType([deprecatedDate: new Date()])

        when:
        List<ReferenceGenomeProjectSeqType> list = new AlignmentInfoService().listReferenceGenome(rgpst.project)

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
        List<ReferenceGenomeProjectSeqType> list = new AlignmentInfoService().listReferenceGenome(theProject)

        then:
        CollectionUtils.containSame(list, [rgpst1, rgpst2, rgpst3])
    }
}

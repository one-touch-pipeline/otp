/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.*

class AlignmentInfoServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                Pipeline,
                Project,
                ProcessingOption,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RoddyWorkflowConfig,
                SeqType,
        ]
    }

    void "getRoddyAlignmentInformation, throws AssertionError when WorkflowConfig is null"() {
        when:
        new AlignmentInfoService().getRoddyAlignmentInformation(null)

        then:
        AssertionError e = thrown()
        e.message.contains('assert config')
    }

    @Unroll
    void "getRoddyAlignmentInformation, when useConfig is #useConvey and mergeTool is #mergeTool, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(["programVersion": "programVersion:1.1.0"])
        AlignmentInfoService service = new AlignmentInfoService([
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
        ])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> {
                new ProcessOutput("""
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
        }

        when:
        AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        alignmentInfo
        alignCommand == alignmentInfo.alignmentProgram
        alignOpt == alignmentInfo.alignmentParameter
        mergeCommand == alignmentInfo.mergeCommand
        mergeOpt == alignmentInfo.mergeOptions
        'Version 6.0' == alignmentInfo.samToolsCommand
        roddyPipelineVersion == alignmentInfo.programVersion

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)

        where:
        useConvey | mergeTool                           || alignCommand         || alignOpt           || mergeCommand                              || mergeOpt       || roddyPipelineVersion
        false     | MergeConstants.MERGE_TOOL_BIOBAMBAM || 'BWA Version 1.0'    || 'alnOpt'           || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
        false     | MergeConstants.MERGE_TOOL_PICARD    || 'BWA Version 1.0'    || 'alnOpt'           || 'Picard Version 4.0'                      || ''             || 'programVersion:1.1.0'
        false     | MergeConstants.MERGE_TOOL_SAMBAMBA  || 'BWA Version 1.0'    || 'alnOpt'           || 'Sambamba Version 5.0'                    || 'sambambaOpt'  || 'programVersion:1.1.0'
        true      | MergeConstants.MERGE_TOOL_BIOBAMBAM || 'bwa-bb Version 2.0' || 'alnOpt conveyOpt' || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || 'programVersion:1.1.0'
    }

    void "getRoddyAlignmentInformation, when rna, return alignment info with the correct data"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig(
                seqType: DomainFactory.createRnaPairedSeqType(),
                adapterTrimmingNeeded: true,
        )
        AlignmentInfoService service = new AlignmentInfoService([
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
        ])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> {
                new ProcessOutput("""
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
        }

        when:
        AlignmentInfo alignmentInfo = service.getRoddyAlignmentInformation(roddyWorkflowConfig)

        then:
        'STAR Version 2.0' == alignmentInfo.alignmentProgram
        ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].join(' ') == alignmentInfo.alignmentParameter
        'Sambamba Version 3.0' == alignmentInfo.mergeCommand
        '' == alignmentInfo.mergeOptions
        'Version 1.0' == alignmentInfo.samToolsCommand

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
    }

    @Unroll
    void "getRoddyAlignmentInformation, when roddy fail for #name, throw Exception"() {
        given:
        RoddyWorkflowConfig roddyWorkflowConfig = DomainFactory.createRoddyWorkflowConfig()
        AlignmentInfoService service = new AlignmentInfoService([
                executeRoddyCommandService: Mock(ExecuteRoddyCommandService) {
                    1 * roddyGetRuntimeConfigCommand(roddyWorkflowConfig, _, roddyWorkflowConfig.seqType.roddyName) >> ''
                    0 * roddyGetRuntimeConfigCommand(_, _, _)
                },
        ])

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> {
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
        'no alignment values' | 'a=b\nc=d'                                      | 'some'                                                                      | 0        || 'Could not extract alignment configuration value from the roddy output'
        'no merging values'   | 'useAcceleratedHardware=false\nBWA_VERSION=aln' | 'some'                                                                      | 0        || 'Could not extract merging configuration value from the roddy output'
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

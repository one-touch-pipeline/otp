/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflowExecution.OtpWorkflowService
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

@Rollback
@Integration
class AlignmentInfoServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory, IsRoddy {

    AlignmentInfoService alignmentInfoService

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

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        Workflow workflow = createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        WorkflowRun workflowRun = createWorkflowRun(
                workflow: workflow,
                combinedConfig: config,
                workflowVersion: createWorkflowVersion(
                        apiVersion: createWorkflowApiVersion(workflow: workflow),
                        workflowVersion: roddyPipelineVersion
                )
        )
        RoddyBamFile bam = createBamFile(
                workflowArtefact: createWorkflowArtefact(
                        producedBy: workflowRun,
                        outputRole: PanCancerWorkflow.OUTPUT_BAM
                )
        )
        bam.workPackage.seqType = seqType
        alignmentInfoService.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }

        when:
        RoddyAlignmentInfo alignmentInfo = alignmentInfoService.getAlignmentInformationForRun(workflowRun)

        then:
        alignmentInfo
        alignmentInfo.alignmentProgram == alignCommand
        alignmentInfo.alignmentParameter == alignOpt
        alignmentInfo.mergeCommand == mergeCommand
        alignmentInfo.mergeOptions == mergeOpt
        alignmentInfo.samToolsCommand == 'Version 6.0'
        alignmentInfo.programVersion == roddyPipelineVersion

        cleanup:

        where:
        useConvey | mergeTool           || alignCommand         || alignOpt           || mergeCommand                              || mergeOpt       || roddyPipelineVersion
        false     | MergeTool.BIOBAMBAM || 'BWA Version 1.0'    || 'alnOpt'           || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || '1.1.0'
        false     | MergeTool.PICARD    || 'BWA Version 1.0'    || 'alnOpt'           || 'Picard Version 4.0'                      || ''             || '1.1.0'
        false     | MergeTool.SAMBAMBA  || 'BWA Version 1.0'    || 'alnOpt'           || 'Sambamba Version 5.0'                    || 'sambambaOpt'  || '1.1.0'
        true      | MergeTool.BIOBAMBAM || 'bwa-bb Version 2.0' || 'alnOpt conveyOpt' || 'Biobambam bammarkduplicates Version 3.0' || 'bioBamBamOpt' || '1.1.0'
    }

    void "test getAlignmentInformationForRun, when rna, return alignment info with the correct data"() {
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

        SeqType seqType = DomainFactory.createRnaPairedSeqType()
        Workflow workflow = createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        WorkflowRun workflowRun = createWorkflowRun(workflow: workflow,
                combinedConfig: config, workflowVersion: createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: workflow)))
        RoddyBamFile bam = createBamFile(workflowArtefact: createWorkflowArtefact(producedBy: workflowRun, outputRole: PanCancerWorkflow.OUTPUT_BAM))
        bam.workPackage.seqType = seqType
        alignmentInfoService.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }

        when:
        RoddyAlignmentInfo alignmentInfo = alignmentInfoService.getAlignmentInformationForRun(workflowRun)

        then:
        alignmentInfo.alignmentProgram == 'STAR Version 2.0'
        alignmentInfo.alignmentParameter == ['2PASS', 'OUT', 'CHIMERIC', 'INTRONS'].join(' ')
        alignmentInfo.mergeCommand == 'Sambamba Version 3.0'
        alignmentInfo.mergeOptions == ''
        alignmentInfo.samToolsCommand == 'Version 1.0'
    }

    @Unroll
    void "test getAlignmentInformationForRun, when #name, throw exception"() {
        given:
        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        Workflow workflow = createWorkflow(name: PanCancerWorkflow.WORKFLOW)
        WorkflowRun workflowRun = createWorkflowRun(
                workflow: workflow,
                combinedConfig: """{"RODDY": {"cvalues": { ${config}}}}""",
                workflowVersion: createWorkflowVersion(apiVersion: createWorkflowApiVersion(workflow: workflow)),
        )
        RoddyBamFile bam = createBamFile(workflowArtefact: createWorkflowArtefact(producedBy: workflowRun, outputRole: PanCancerWorkflow.OUTPUT_BAM))
        bam.workPackage.seqType = seqType
        alignmentInfoService.otpWorkflowService = Mock(OtpWorkflowService) {
            1 * lookupOtpWorkflowBean(_) >> new WgbsWorkflow()
        }

        when:
        alignmentInfoService.getAlignmentInformationForRun(workflowRun)

        then:
        Exception e = thrown()
        e.message.contains(expectedError)

        where:
        name                  | config                                                                                                                 || expectedError
        'no alignment values' | """"a": {"value": "b", "type": "string"},"c": {"value": "d", "type": "string"}"""                                      || 'Could not extract alignment configuration value from Roddy config'
        'no merging values'   | """"useAcceleratedHardware": {"value": "false", "type": "string"},"BWA_VERSION": {"value": "aln", "type": "string"}""" || 'Could not extract merging configuration value from Roddy config'
    }

    void "test getAlignmentInformationForRun, from Roddy WorkflowConfig and AlignmentInfo"() {
        when:
        alignmentInfoService.getAlignmentInformationForRun(null)

        then:
        thrown(AssertionError)
    }
}

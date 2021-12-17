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

package rollout.docker

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowVersion

/**
 * Creates some realistic WorkflowVersions for Docker Setup.
 */

ProcessingOptionService processingOptionService = ctx.processingOptionService

WorkflowVersion.withTransaction {
    new WorkflowVersion(
            workflow: Workflow.findByName("Roddy ACEseq (CNV calling)"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_ACESEQ_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.aceseqPipelineSeqTypes.first().roddyName)
    ).save(flush: true)

    new WorkflowVersion(
            workflow: Workflow.findByName("Roddy Indel calling"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_INDEL_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.indelPipelineSeqTypes.first().roddyName)
    ).save(flush: true)

    new WorkflowVersion(
            workflow: Workflow.findByName("WGBS alignment"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType.roddyName)
    ).save(flush: true)

    new WorkflowVersion(
            workflow: Workflow.findByName("RNA alignment"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_ALIGNMENT_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.rnaPairedSeqType.roddyName)
    ).save(flush: true)

    new WorkflowVersion(
            workflow: Workflow.findByName("Roddy SNV calling"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.snvPipelineSeqTypes.first().roddyName)
    ).save(flush: true)

    new WorkflowVersion(
            workflow: Workflow.findByName("Roddy Sophia (structural variation calling)"),
            workflowVersion: processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION,
                    SeqTypeService.sophiaPipelineSeqTypes.first().roddyName)
    ).save(flush: true)
}

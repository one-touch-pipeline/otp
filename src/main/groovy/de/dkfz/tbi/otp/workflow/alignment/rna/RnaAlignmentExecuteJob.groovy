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
package de.dkfz.tbi.otp.workflow.alignment.rna

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.RoddyAlignmentExecuteJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@Component
@Slf4j
class RnaAlignmentExecuteJob extends RoddyAlignmentExecuteJob implements RnaAlignmentShared {

    @Override
    protected String getRoddyWorkflowName() {
        return "RNAseqWorkflow"
    }

    @Override
    protected String getAnalysisConfiguration(SeqType seqType) {
        return 'RNAseqAnalysis'
    }

    @Override
    protected final Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig) {
        Map<String, String> conf = super.getConfigurationValues(workflowStep, combinedConfig)

        RnaRoddyBamFile roddyBamFile = getRoddyBamFile(workflowStep)

        conf.putAll(roddyConfigValueService.getFilesToMerge(roddyBamFile))

        String adapterSequence = CollectionUtils.exactlyOneElement(
                roddyBamFile.containedSeqTracks*.libraryPreparationKit*.reverseComplementAdapterSequence.unique().findAll(),
                "There is not exactly one reverse complement adapter sequence available for fastq file(s) for the bam file ${roddyBamFile}")
        assert adapterSequence: "adapterSequence not found in the BAM file ${roddyBamFile}"
        conf.put("ADAPTER_SEQ", adapterSequence)
        // the following two variables need to be provided since Roddy does not use the normal path definition for RNA
        conf.put("ALIGNMENT_DIR", getWorkDirectory(workflowStep).toString())
        conf.put("outputBaseDirectory", getWorkDirectory(workflowStep).toString())

        return conf
    }
}

/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.ExecutePanCanJob
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class ExecuteRnaAlignmentJob extends ExecutePanCanJob {

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile : "roddyBamFile must not be null"
        List<File> filesToMerge = getFilesToMerge(roddyBamFile)
        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        cValues.add("fastq_list:${filesToMerge.join(";")}")

        String adapterSequence = CollectionUtils.exactlyOneElement(
                roddyBamFile.containedSeqTracks*.libraryPreparationKit*.reverseComplementAdapterSequence.unique().findAll(),
                "There is not exactly one reverse complement adapter sequence available for BAM file ${roddyBamFile}")
        assert adapterSequence : "There is exactly one reverse complement adapter sequence available for BAM file ${roddyBamFile}, but it is null"

        cValues.add("ADAPTER_SEQ:${adapterSequence}")
        // the following two variables need to be provided since Roddy does not use the normal path definition for RNA
        cValues.add("ALIGNMENT_DIR:${roddyBamFile.workDirectory}")
        cValues.add("outputBaseDirectory:${roddyBamFile.workDirectory}")

        if (roddyBamFile.seqType.libraryLayout == SequencingReadType.SINGLE) {
            cValues.add("useSingleEndProcessing:true")
        } else if (roddyBamFile.seqType.libraryLayout == SequencingReadType.PAIRED) {
            cValues.add("useSingleEndProcessing:false")
        }

        return cValues
    }
}

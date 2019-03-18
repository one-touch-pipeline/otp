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

package de.dkfz.tbi.otp.alignment

import org.junit.Ignore

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Ignore
class RnaSingleAlignmentWorkflowTests extends AbstractRnaAlignmentWorkflowTests {

    @Override
    SeqType findSeqType() {
        DomainFactory.createRnaSingleSeqType()
    }

    @Override
    SeqTrack createSeqTrack(String readGroupNum, Map properties = [:]) {
        MergingWorkPackage workPackage = exactlyOneElement(MergingWorkPackage.findAll())

        Map seqTrack1Properties = [
                laneId               : readGroupNum,
                fastqcState          : SeqTrack.DataProcessingState.FINISHED,
                dataInstallationState: SeqTrack.DataProcessingState.FINISHED,
        ] + properties

        SeqTrack seqTrack1 = DomainFactory.createSeqTrackWithDataFiles(workPackage, seqTrack1Properties)

        DataFile.findAllBySeqTrack(seqTrack1).eachWithIndex { DataFile dataFile, int index ->
            dataFile.vbpFileName = dataFile.fileName = "fastq_${seqTrack1.individual.pid}_${seqTrack1.sampleType.name}_${seqTrack1.laneId}_${index + 1}.fastq.gz"
            dataFile.nReads = AbstractRoddyAlignmentWorkflowTests.NUMBER_OF_READS
            dataFile.save(flush: true)
        }

        linkFastqFiles(seqTrack1, testFastqFiles.get(readGroupNum))

        workPackage.needsProcessing = true
        workPackage.save(flush: true)
        return seqTrack1
    }
}

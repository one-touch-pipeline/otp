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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.*

/*
Changes the qcTrafficLightStatus of multiple BamFiles at once.

Beware that this script can take a long time (like 30 seconds per BamFile) because
accepting a file also means doing changes on the file system.

Batches are created by the combination of Project, a selection of SeqTypes and the
current qcTrafficLight status.
*/

String projectName = ""
List<SeqType> seqTypes = [
        //SeqTypeService.wholeGenomePairedSeqType,
        //SeqTypeService.exomePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfitePairedSeqType,
        //SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
        //SeqTypeService.rnaSingleSeqType,
        //SeqTypeService.rnaPairedSeqType,
        //SeqTypeService.chipSeqPairedSeqType,
        //SeqTypeService.'10xSingleCellRnaSeqType',
]

// QC_PASSED, ACCEPTED, BLOCKED, REJECTED, AUTO_ACCEPTED, UNCHECKED
AbstractBamFile.QcTrafficLightStatus fromStatus = BLOCKED
AbstractBamFile.QcTrafficLightStatus toStatus = ACCEPTED

String comment = ""
String author = ""

assert projectName: "define which project the BamFiles are from"
assert seqTypes: "define which SeqTypes you want to cover"
assert fromStatus: "define the status the BamFiles have right now"
assert toStatus: "define the status the BamFiles should have"
assert comment: "define a comment for why the status is set"
assert author: "define which name is set as the author of the comment"

List<AbstractBamFile> bams = AbstractBamFile.createCriteria().list {
    workPackage {
        sample {
            individual {
                project {
                    eq("name", projectName)
                }
            }
        }
        'in'("seqType", seqTypes)
    }
    eq("qcTrafficLightStatus", fromStatus)
    eq("withdrawn", false)
    eq("fileOperationStatus", AbstractBamFile.FileOperationStatus.PROCESSED)
    eq("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.FINISHED)
}

bams.each { AbstractBamFile bam ->
    println String.format(
            "%-15s %s: %-13s %-20s %-10s '%-13s' by %s",
            bam.class.simpleName,
            bam.id,
            bam.individual.pid,
            bam.seqType.displayNameWithLibraryLayout,
            bam.qcTrafficLightStatus,
            bam.comment.comment,
            bam.comment.author,
    )
}
println "${bams.size()} BAMs"

assert false: "fail for debug"

AbstractBamFile.withTransaction {
    bams.each { AbstractBamFile bam ->
        ctx.qcTrafficLightService.setQcTrafficLightStatusWithComment(bam, toStatus, comment)
        bam.comment.author = author
        bam.comment.save(flush: true)
    }
}

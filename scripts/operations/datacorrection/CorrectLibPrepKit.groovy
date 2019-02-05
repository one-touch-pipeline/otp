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

package operations.datacorrection

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.*

/**
 * Correct the set library preparation kit in seqTracks, metadata and MergingWorkPackages
 * for the given ilse numbers and seqType.
 */


String libPrepKit = '' //new lib prep kit

List<Long> ilseNumbers = []

SeqType seqType = null //SeqTypeService.
/*
getWholeGenomePairedSeqType()
getExomePairedSeqType()
getWholeGenomeBisulfitePairedSeqType()
getWholeGenomeBisulfiteTagmentationPairedSeqType()
getRnaPairedSeqType()
getChipSeqPairedSeqType()
getRnaSingleSeqType()
get10xSingleCellRnaSeqType()
getSingleSeqType()
 */

String commentInfo = '' //some additional Information about the change, perhaps link to OTRS ticket


//---------------------------

assert libPrepKit
assert commentInfo


LibraryPreparationKit libraryPreparationKit = CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllByName(libPrepKit))

MetaDataKey key = CollectionUtils.exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.LIB_PREP_KIT.name()))

CommentService commentService = ctx.commentService

List<SeqTrack> seqTrackList = SeqTrack.createCriteria().list {
    ilseSubmission {
        'in'('ilseNumber', ilseNumbers)
    }
    eq('seqType', seqType)
}


SeqTrack.withTransaction {
    seqTrackList.each { SeqTrack seqTrack ->
        println "$seqTrack  ${seqTrack.libraryPreparationKit}"
        seqTrack.libraryPreparationKit = libraryPreparationKit
        seqTrack.kitInfoReliability = InformationReliability.KNOWN
        DataFile.findAllBySeqTrack(seqTrack).each {
            MetaDataEntry entry = CollectionUtils.exactlyOneElement(MetaDataEntry.findAllByDataFileAndKey(it, key))
            String oldComment = it.comment?.comment ?: ''
            String newComment = "Correct ${entry.value} to ${libPrepKit},\n${commentInfo}".trim()
            String combinedComment = (oldComment ? "$oldComment\n\n" : '') + newComment
            println "    $entry"
            println newComment
            entry.value = libPrepKit
            commentService.saveComment(it, combinedComment)
        }
        List<RoddyBamFile> rbf = RoddyBamFile.createCriteria().list {
            seqTracks {
                eq('id', seqTrack.id)
            }
        }

        /*
            WGBS can, for experimental reasons, have lanes with different libPrepKits, that still need to be
            merged. This is OK as long as they all end up using the same Adapter File. The WGBS-alignment for
            unique(MWP*.seqTracks*.libraryPreparationKit*.adapterfile)
        */
        if (!seqType.isWgbs()) {
            rbf*.mergingWorkPackage.unique().each { MergingWorkPackage workPackage ->
                println "    $workPackage  ${workPackage.libraryPreparationKit}"
                workPackage.libraryPreparationKit = libraryPreparationKit
            }
        }
    }

    assert false
}


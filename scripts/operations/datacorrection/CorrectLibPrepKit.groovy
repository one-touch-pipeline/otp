package operations.datacorrection

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Correct the set library preparation kit in seqTracks, metadata and MergingWorkPackages
 * for the given ilse numbers and seqType.
 */


String libPrepKit = '' //new lib prep kit

List<Long> ilseNumbers = [
        // ilse numbers to update

]
SeqType seqType = SeqType.rnaPairedSeqType

String commentInfo = '' //some additional Information about the change, perhaps link to OTRS ticket


//---------------------------

assert libPrepKit
assert commentInfo


LibraryPreparationKit libraryPreparationKit = CollectionUtils.exactlyOneElement(LibraryPreparationKitSynonym.findAllByName(libPrepKit)).libraryPreparationKit

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
        DataFile.findAllBySeqTrack(seqTrack).each {
            MetaDataEntry entry = CollectionUtils.exactlyOneElement(MetaDataEntry.findAllByDataFileAndKey(it, key))
            String oldComment = it.comment?.comment ?: ''
            String newComment = "Correct ${entry.value} to ${libPrepKit},\n${commentInfo}".trim()
            String combinedComment = (oldComment ? "$oldComment\n\n" : '') + newComment
            println "    $entry"
            println combinedComment
            entry.value = libPrepKit
            commentService.saveComment(it, combinedComment)
        }
        List<RoddyBamFile> rbf = RoddyBamFile.createCriteria().list {
            seqTracks {
                eq('id', seqTrack.id)
            }
        }
        rbf*.mergingWorkPackage.unique().each {MergingWorkPackage workPackage ->
            println "    $workPackage  ${workPackage.libraryPreparationKit}"
            workPackage.libraryPreparationKit = libraryPreparationKit
        }
    }

    assert false
}


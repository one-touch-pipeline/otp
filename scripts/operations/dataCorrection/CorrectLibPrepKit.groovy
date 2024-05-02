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
package operations.datacorrection

import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Correct the set library preparation kit in seqTracks, metadata and MergingWorkPackages if it used there.
 * The seqtracks can be selected about ilse, project or pid and filtered by seqType
 */

// ---------------------------
// input

String libPrepKit = '' // new lib prep kit

String commentInfo = '' // some additional Information about the change, perhaps link to ticket

boolean overrideLibPrepKit = false // if a lib prep kit already exist, override it

boolean dryRun = true // run first without saving the changes

Collection<SeqTrack> seqTrackList = SeqTrack.withCriteria {
    or {
        sample {
            individual {
                or {
                    'in'('pid', [
                            '',
                    ])
                    project {
                        'in'('name', [
                                '',
                        ])
                    }
                }
            }
        }
        ilseSubmission(JoinType.LEFT_OUTER_JOIN.joinTypeValue) {
            'in'('ilseNumber', [
                    -1,
            ])
        }
    }
    'in'('seqType', [
            // SeqTypeService.wholeGenomePairedSeqType,
            // SeqTypeService.exomePairedSeqType,
            // SeqTypeService.wholeGenomeBisulfitePairedSeqType,
            // SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType,
            // SeqTypeService.rnaPairedSeqType,
            // SeqTypeService.rnaSingleSeqType,
            // SeqTypeService.chipSeqPairedSeqType,
    ])
}

// ---------------------------
// ensure keeping of import of disabled code

SeqTypeService seqTypeService

// -----------------------------
assert libPrepKit : 'No lib prep kit is provided'
assert commentInfo: 'No comment is provided'
assert seqTrackList :' No seq tracks found'

LibraryPreparationKit libraryPreparationKit = CollectionUtils.exactlyOneElement(LibraryPreparationKit.findAllByName(libPrepKit), "Lib prep '${libPrepKit}' not found")

MetaDataKey key = CollectionUtils.exactlyOneElement(MetaDataKey.findAllByName(MetaDataColumn.LIB_PREP_KIT.name()))

CommentService commentService = ctx.commentService

SeqTrack.withTransaction {
    seqTrackList.each { SeqTrack seqTrack ->
        if (!seqTrack.libraryPreparationKit || (seqTrack.libraryPreparationKit && overrideLibPrepKit)) {
            println "Change:\t$seqTrack  ${seqTrack.libraryPreparationKit}"
            seqTrack.libraryPreparationKit = libraryPreparationKit
            seqTrack.kitInfoReliability = InformationReliability.KNOWN
            RawSequenceFile.findAllBySeqTrack(seqTrack).each {
                MetaDataEntry entry = CollectionUtils.atMostOneElement(MetaDataEntry.findAllBySequenceFileAndKey(it, key))

                String oldComment = it.comment?.comment ?: ''
                String newComment = "Correct ${entry?.value} to ${libPrepKit},\n${commentInfo}".trim()
                String combinedComment = (oldComment ? "$oldComment\n\n" : '') + newComment
                println "    MetaDataEntry: $entry"
                println "    added comment: ${newComment.replaceAll('\n','\n\t')}"
                println "    complete comment: ${combinedComment.replaceAll('\n','\n\t')}"
                commentService.saveComment(it, combinedComment)
            }
            /*
            * Update MergingWorkPackages only, if mergingCriteria use lib prep kit
            */
            MergingCriteria mergingCriteria = CollectionUtils.atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(seqTrack.project, seqTrack.seqType))
            if (mergingCriteria && mergingCriteria.useLibPrepKit) {
                RoddyBamFile.createCriteria().list {
                    seqTracks {
                        eq('id', seqTrack.id)
                    }
                }*.mergingWorkPackage.unique().each { MergingWorkPackage workPackage ->
                    println "    $workPackage  ${workPackage.libraryPreparationKit}"
                    workPackage.libraryPreparationKit = libraryPreparationKit
                    workPackage.save(flush: true)
                }
            }
        } else {
            println "Not changed:\t$seqTrack  ${seqTrack.libraryPreparationKit}"
        }
    }

    assert !dryRun: "DRY RUN: transaction intentionally failed to rollback changes"
}


/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.ngsqc

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

@CompileDynamic
@Transactional
class FastqcResultsService {

    SeqTrackService seqTrackService

    boolean isFastqcAvailable(RawSequenceFile rawSequenceFile) {
        return CollectionUtils.atMostOneElement(FastqcProcessedFile.findAllBySequenceFileAndContentUploaded(rawSequenceFile, true))
    }

    Map<Long, Boolean> fastqcLinkMap(Run run) {
        Map<Long, Boolean> map = [:]
        List<SeqTrack> seqTracks = SeqTrack.findAllByRun(run) // to be protected by ACLs
        seqTracks.each { SeqTrack seqTrack ->
            List<RawSequenceFile> files = seqTrackService.getSequenceFilesForSeqTrack(seqTrack)
            files.each { RawSequenceFile file ->
                map.put(file.id, isFastqcAvailable(file))
            }
        }
        return map
    }

    /**
     * Retrieves the FastQC {@link RawSequenceFile}s for the given {@link Sequence}s where FastQC is finished.
     *
     * The returned list includes the found DataFiles but does not order them by
     * Sequences. If a mapping to the Sequence is needed it's the task of the
     * callee to perform this operation.
     * @param sequences The Sequences for which the FastQC DataFiles should be retrieved
     * @return The FastQC DataFiles found by the Sequences
     */
    List<RawSequenceFile> fastQCFiles(List<Sequence> sequences) {
        return FastqcProcessedFile.createCriteria().list {
            sequenceFile {
                seqTrack {
                    'in'('id', sequences*.seqTrackId)
                    eq('fastqcState', SeqTrack.DataProcessingState.FINISHED)
                }
            }
        }*.sequenceFile
    }
}

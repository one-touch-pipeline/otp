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
package de.dkfz.tbi.otp.monitor.alignment

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.monitor.MonitorOutputCollector
import de.dkfz.tbi.otp.monitor.PipelinesChecker
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType

class AllAlignmentsChecker extends PipelinesChecker<SeqTrack> {

    static final String HEADER_NOT_SUPPORTED_SEQTYPES =
            'The following SeqTypes are unsupported by any alignment workflow supported by OTP'

    @CompileDynamic
    @Override
    List handle(List<SeqTrack> seqTracks, MonitorOutputCollector output) {
        if (!seqTracks) {
            return []
        }

        seqTracks.unique()
        Map<SeqType, List<SeqTrack>> seqTracksBySeqType = seqTracks.groupBy {
            it.seqType
        }

        List<AbstractAlignmentChecker> checkers = [
                new PanCanAlignmentChecker(),
                new WgbsRoddyAlignmentChecker(),
                new RnaRoddyAlignmentChecker(),
                new CellRangerAlignmentChecker(),
        ]

        Map<AbstractAlignmentChecker, List<SeqTrack>> seqTracksPerChecker = checkers.collectEntries { AbstractAlignmentChecker checker ->
            [
                    (checker): checker.seqTypes.collectMany { SeqType seqType ->
                        seqTracksBySeqType.remove(seqType) ?: []
                    }.unique().findAll()
            ]
        }

        List<SeqTrack> unsupportedSeqTracks = seqTracksBySeqType.values().flatten()

        output.showUniqueList(HEADER_NOT_SUPPORTED_SEQTYPES, unsupportedSeqTracks) { SeqTrack seqTrack ->
            "${seqTrack.seqType.displayNameWithLibraryLayout}"
        }

        return seqTracksPerChecker.collectMany { AbstractAlignmentChecker checker, List<SeqTrack> seqTrackList ->
            checker.handle(seqTrackList, output) ?: []
        }.findAll()
    }
}

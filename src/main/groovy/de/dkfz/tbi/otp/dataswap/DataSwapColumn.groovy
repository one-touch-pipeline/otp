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
package de.dkfz.tbi.otp.dataswap

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.SeqTrack

@TupleConstructor
enum DataSwapColumn {
    SEQ_TRACK("sequence.list.headers.seqTrack"),
    ILSE_ID("sequence.list.headers.ilseId"),
    RUN("sequence.list.headers.run"),
    LANE("sequence.list.headers.lane"),
    SINGLE_CELL_WELL_LABEL('sequence.list.headers.singleCellWellLabel'),

    PROJECT("sequence.list.headers.project", true, { SeqTrack seqTrack -> seqTrack.project?.name }),
    INDIVIDUAL("sequence.list.headers.individual", true, { SeqTrack seqTrack -> seqTrack.individual?.pid }),
    SAMPLE_TYPE("sequence.list.headers.sampleType", true, { SeqTrack seqTrack -> seqTrack.sampleType?.name }),
    SEQ_TYPE("sequence.list.headers.seqType", true, { SeqTrack seqTrack -> seqTrack.seqType?.displayName }),

    LIBRARY_LAYOUT("sequence.list.headers.sequencingReadType"),
    SINGLE_CELL("sequence.list.headers.singleCell"),

    SAMPLE_NAME("sequence.list.headers.sampleName", true, { SeqTrack seqTrack -> seqTrack.sampleIdentifier }),
    ANTIBODY_TARGET("sequence.list.headers.antibodyTarget", true, { SeqTrack seqTrack -> seqTrack.antibodyTarget?.name }),

    final String message
    final Boolean duplicateColumnForSampleSwapTemplate
    final Closure<String> seqTrackTextMapping

    static Collection<DataSwapColumn> getDuplicatedColumns() {
        return values().findAll { it.duplicateColumnForSampleSwapTemplate }
    }
}

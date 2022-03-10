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
package de.dkfz.tbi.otp.egaSubmission

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

import de.dkfz.tbi.util.spreadsheet.Row

import static de.dkfz.tbi.otp.egaSubmission.EgaSubmissionFileService.EgaColumnName.*

@EqualsAndHashCode
@ToString
class EgaMapKey implements Comparable<EgaMapKey> {
    final String individualName
    final String seqTypeName
    final String sequencingReadType
    final String singleCell
    final String sampleTypeName

    EgaMapKey(Row row) {
        individualName = row.getCellByColumnTitle(INDIVIDUAL.value).text
        sampleTypeName = row.getCellByColumnTitle(SAMPLE_TYPE.value).text
        seqTypeName = row.getCellByColumnTitle(SEQ_TYPE_NAME.value).text
        sequencingReadType = row.getCellByColumnTitle(SEQUENCING_READ_TYPE.value).text
        singleCell = row.getCellByColumnTitle(SINGLE_CELL.value).text
    }

    EgaMapKey(SampleSubmissionObject sampleSubmissionObject) {
        individualName = sampleSubmissionObject.sample.individual.displayName
        sampleTypeName = sampleSubmissionObject.sample.sampleType.displayName
        seqTypeName = sampleSubmissionObject.seqType.displayName
        sequencingReadType = sampleSubmissionObject.seqType.libraryLayout
        singleCell = sampleSubmissionObject.seqType.singleCellDisplayName
    }

    EgaMapKey(String individualName, String seqTypeName, String sequencingReadType, String singleCell, String sampleTypeName) {
        this.individualName = individualName
        this.seqTypeName = seqTypeName
        this.sequencingReadType = sequencingReadType
        this.singleCell = singleCell
        this.sampleTypeName = sampleTypeName
    }

    @Override
    int compareTo(EgaMapKey egaMapKey) {
        return individualName <=> egaMapKey.individualName ?:
                sampleTypeName <=> egaMapKey.sampleTypeName ?:
                        sequencingReadType <=> egaMapKey.sequencingReadType ?:
                                singleCell <=> egaMapKey.singleCell ?:
                                        seqTypeName <=> egaMapKey.seqTypeName
    }
}

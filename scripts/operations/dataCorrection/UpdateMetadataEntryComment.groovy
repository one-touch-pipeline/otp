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
package operations.dataCorrection

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

/**
 * Script to add/change the COMMENT for meta data entries
 * The input for this script is the path to a tsv file containing
 * SAMPLE_ID's (which should be unique) and their associated COMMENT.
 * (You can use either the normal metadata tsv file used for import, or create a new file with just these two columns)
 */


File input = new File("/absolute/path/to/file.tsv")


/////////////////////////////////////////////////////

Spreadsheet s = new Spreadsheet(input.text)

[MetaDataColumn.SAMPLE_ID, MetaDataColumn.COMMENT]*.name().each { String it ->
    assert it in s.header.cells*.text : "Input file doesn't contain column ${it}."
}

s.dataRows.each { Row row ->
    Sample sample = SampleIdentifier.findByName(row.getCellByColumnTitle(MetaDataColumn.SAMPLE_ID.name()).text).sample
    List<MetaDataEntry> entries = MetaDataEntry.createCriteria().list {
        key {
            eq("name", MetaDataColumn.COMMENT.name())
        }
        dataFile {
            seqTrack {
                eq("sample", sample)
            }
        }
    } as List<MetaDataEntry>
    String comment = row.getCellByColumnTitle(MetaDataColumn.COMMENT.name()).text
    entries.each { MetaDataEntry entry ->
        entry.value = comment
        entry.save(flush: true)
    }
}

[]
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
package de.dkfz.tbi.util.spreadsheet

import spock.lang.Specification

class FilteredSpreadsheetSpec extends Specification {

    void 'dataRows, when document contains rows to be excluded, does not contain the excluded rows'() {
        when:
        Spreadsheet spreadsheet = new FilteredSpreadsheet("c 0 1 2 3 4".replaceAll(' ', '\n'), Delimiter.TAB, Closure.IDENTITY, { Row row ->
            Integer.parseInt(row.getCellByColumnTitle('c').text) % 2 == 0
        })

        then:
        spreadsheet.dataRows.size() == 3
        spreadsheet.dataRows[0].cells[0].text == '0'
        spreadsheet.dataRows[1].cells[0].text == '2'
        spreadsheet.dataRows[2].cells[0].text == '4'
    }
}

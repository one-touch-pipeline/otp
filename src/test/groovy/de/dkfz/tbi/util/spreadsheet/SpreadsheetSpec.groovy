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
import spock.lang.Unroll

class SpreadsheetSpec extends Specification {

    Spreadsheet spreadsheet = new Spreadsheet(
            'A1\tB1\tC1\n' +
            'A2\tB2\n' +
            '\n' +
            'A4\tB4\tC4\tD4')

    void test_header() {
        expect:
        spreadsheet.header.spreadsheet == spreadsheet
        spreadsheet.header.rowIndex == 0
        spreadsheet.header.cells*.text == ['A1', 'B1', 'C1']
    }

    void test_dataRows() {
        expect:

        spreadsheet.dataRows.size() == 3

        spreadsheet.dataRows[0].spreadsheet == spreadsheet
        spreadsheet.dataRows[0].rowIndex == 1
        spreadsheet.dataRows[0].cells*.text == ['A2', 'B2', '']

        spreadsheet.dataRows[1].spreadsheet == spreadsheet
        spreadsheet.dataRows[1].rowIndex == 2
        spreadsheet.dataRows[1].cells*.text == ['', '', '']

        spreadsheet.dataRows[2].spreadsheet == spreadsheet
        spreadsheet.dataRows[2].rowIndex == 3
        spreadsheet.dataRows[2].cells*.text == ['A4', 'B4', 'C4', 'D4']
    }

    void test_getColumn_WithExistingTitle_ShouldReturnColumn() {
        expect:
        spreadsheet.getColumn('A1').headerCell != null
        spreadsheet.getColumn('B1').headerCell != null
        spreadsheet.getColumn('C1').headerCell != null
    }

    void test_getColumn_WithMissingTitle_ShouldReturnNull() {
        expect:
        spreadsheet.getColumn('D1') == null
    }

    void test_getCell_RowHasThisColumn_ShouldReturnCell() {
        given:
        Column column = spreadsheet.getColumn('B1')
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCell(column).text == 'B2'
    }

    void test_getCell_RowDoesNotHaveColumn_ShouldReturnEmptyCell() {
        given:
        Column column = spreadsheet.getColumn('C1')
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCell(column).text == ''
    }

    void test_getCell_WithNull_ShouldReturnNull() {
        given:
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCell(null) == null
    }

    void test_getCellByColumnTitle_RowHasColumnWithTitle_ShouldReturnCell() {
        given:
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCellByColumnTitle('B1').text == 'B2'
    }

    void test_getCellByColumnTitle_RowDoesNotHaveColumnWithTitle_ShouldReturnEmptyCell() {
        given:
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCellByColumnTitle('C1').text == ''
    }

    void test_getCellByColumnTitle_WithMissingTitle_ShouldReturnNull() {
        given:
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCellByColumnTitle('D1') == null
    }

    void test_getCellByColumnTitle_WithNull_ShouldReturnNull() {
        given:
        Row dataRow = spreadsheet.dataRows[0]

        expect:
        dataRow.getCellByColumnTitle(null) == null
    }

    void test_Cell_Properties() {
        given:
        Row dataRow = spreadsheet.dataRows[0]
        Cell cell = dataRow.cells[1]

        expect:
        cell.text == 'B2'
        cell.row == dataRow
        cell.rowIndex == dataRow.rowIndex
        cell.columnIndex == 1
    }

    void test_GetCellAddress() {
        expect:
        spreadsheet.header.cells*.cellAddress == ['A1', 'B1', 'C1']
        spreadsheet.dataRows[0].cells*.cellAddress == ['A2', 'B2', 'C2']
        spreadsheet.dataRows[1].cells*.cellAddress == ['A3', 'B3', 'C3']
        spreadsheet.dataRows[2].cells*.cellAddress == ['A4', 'B4', 'C4', 'D4']
    }

    @Unroll('column index #index maps to column address #address')
    void test_GetColumnAddress() {
        expect:
        Cell.getColumnAddress(index) == address

        where:
        index || address

        0 || 'A'
        1 || 'B'

        // exceeding A-Z (26)
        26 - 2 || 'Y'
        26 - 1 || 'Z'
        26 + 0 || 'AA'
        26 + 1 || 'AB'
        26 + 2 || 'AC'

        // exceeding A-Z (26) and AA-AZ (26)
        26 + 26 - 2 || 'AY'
        26 + 26 - 1 || 'AZ'
        26 + 26 + 0 || 'BA'
        26 + 26 + 1 || 'BB'
        26 + 26 + 2 || 'BC'

        // exceeding A-Z (26) and AA-ZZ (26 ** 2)
        26 + 26 ** 2 - 2 || 'ZY'
        26 + 26 ** 2 - 1 || 'ZZ'
        26 + 26 ** 2 + 0 || 'AAA'
        26 + 26 ** 2 + 1 || 'AAB'
        26 + 26 ** 2 + 2 || 'AAC'

        // exceeding A-Z (26) and AA-ZZ (26 ** 2) and AAA-AAZ (26)
        26 + 26 ** 2 + 26 - 2 || 'AAY'
        26 + 26 ** 2 + 26 - 1 || 'AAZ'
        26 + 26 ** 2 + 26 + 0 || 'ABA'
        26 + 26 ** 2 + 26 + 1 || 'ABB'
        26 + 26 ** 2 + 26 + 2 || 'ABC'

        // exceeding A-Z (26) and AA-ZZ (26 ** 2) and AAA-AZZ (26 ** 2)
        26 + 26 ** 2 + 26 ** 2 - 2 || 'AZY'
        26 + 26 ** 2 + 26 ** 2 - 1 || 'AZZ'
        26 + 26 ** 2 + 26 ** 2 + 0 || 'BAA'
        26 + 26 ** 2 + 26 ** 2 + 1 || 'BAB'
        26 + 26 ** 2 + 26 ** 2 + 2 || 'BAC'

        // exceeding A-Z (26) and AA-ZZ (26 ** 2) and AAA-ZZZ (26 ** 3)
        26 + 26 ** 2 + 26 ** 3 - 2 || 'ZZY'
        26 + 26 ** 2 + 26 ** 3 - 1 || 'ZZZ'
        26 + 26 ** 2 + 26 ** 3 + 0 || 'AAAA'
        26 + 26 ** 2 + 26 ** 3 + 1 || 'AAAB'
        26 + 26 ** 2 + 26 ** 3 + 2 || 'AAAC'
    }

    void "Cell constructor trims and shortens whitespace on text"() {
        given:
        String document = """\
            |headerA\theaderB
            |${given}\tsomething else""".stripMargin()

        Cell cell = new Spreadsheet(document).dataRows[0].cells[0]

        expect:
        cell.text == expected

        where:
        given  || expected
        "x"    || "x"
        " x"   || "x"
        "x "   || "x"
        ""     || ""
        "  "   || ""
        "x  x" || "x x"
    }
}

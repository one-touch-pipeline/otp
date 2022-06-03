/*
 * Copyright 2011-2020 The OTP authors
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

import com.opencsv.*
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import de.dkfz.tbi.otp.utils.StringUtils

/**
 * An immutable in-memory representation of a delimiter-separated spreadsheet file.
 *
 * <p>
 * Each row has at least as many cells as the header. If an input line has less columns than the header line, cells with
 * an empty string as text will be added at the end of the row.
 *
 * <p>
 * This reader internally follows the CSV recommendations of RFC-4180.
 */
class Spreadsheet {
    final Row header
    final Delimiter delimiter
    private final Map<String, Column> columnsByTitle
    private final List<Row> dataRows

    /**
     * Creates a new Spreadsheet from string contents.
     *
     * @param document String representing the unparsed spreadsheet content.
     * @param delimiter Character used to separate values. Defaults to {@link Delimiter#TAB}.
     * @param renameHeader Closure that can optionally preprocess the headers after parsing, but before saving. E.g. to rename, or to normalise aliases.
     */
    Spreadsheet(String document,
                Delimiter delimiter = Delimiter.TAB,
                @ClosureParams(value = SimpleType, options = ['java.lang.String']) Closure<String> renameHeader = Closure.IDENTITY) {
        this.delimiter = delimiter
        Map<String, Column> columnsByTitle = [:]
        List<Row> dataRows = []
        int rowIndex = 0

        BufferedReader rawReader
        CSVReader csvReader
        try {
            rawReader = new BufferedReader(new StringReader(document))

            if (delimiter == Delimiter.AUTO_DETECT) {
                delimiter = Delimiter.detectDelimiter(rawReader)
            }

            csvReader = new CSVReaderBuilder(rawReader).
                    withCSVParser(new RFC4180ParserBuilder().withSeparator(delimiter.delimiter).build()).build()
            csvReader.errorLocale = Locale.ENGLISH
            String[] line
            while ((line = csvReader.readNext()) != null) {
                if (header) { // normal lines are kept without processing.
                    dataRows.add(new Row(this, rowIndex++, line as List<String>))
                } else { // intercept first line as header
                    header = new Row(this, rowIndex++, line.collect(renameHeader))
                    for (Cell cell : header.cells) {
                        if (cell.text.empty) {
                            throw new EmptyHeaderException(cell.columnAddress)
                        }
                        if (columnsByTitle.containsKey(cell.text)) {
                            throw new DuplicateHeaderException(cell.text)
                        }
                        columnsByTitle.put(cell.text, new Column(cell))
                    }
                }
            }
        } finally {
            if (rawReader != null) {
                rawReader.close()
            }
            if (csvReader != null) {
                csvReader.close()
            }
        }

        this.columnsByTitle = columnsByTitle.asImmutable()
        this.dataRows = dataRows.asImmutable()
    }

    /**
     * @return The column with the specified title or {@code null} if the spreadsheet does not contain a column with
     * that title
     */
    Column getColumn(String columnTitle) {
        return columnsByTitle.get(columnTitle)
    }

    List<Row> getDataRows() {
        return dataRows
    }
}

class Row {

    final Spreadsheet spreadsheet
    /**
     * The first row (header) has index 0
     */
    final int rowIndex
    final List<Cell> cells

    protected Row(Spreadsheet spreadsheet, int rowIndex, List<String> line) {
        this.spreadsheet = spreadsheet
        this.rowIndex = rowIndex
        int columnIndex = 0
        List<Cell> cells = line.collect { new Cell(this, columnIndex++, it) }
        if (spreadsheet.header) {
            // Make sure that this row has at least as many cells as the header
            while (columnIndex < spreadsheet.header.cells.size()) {
                cells.add(new Cell(this, columnIndex++, ''))
            }
        }
        this.cells = cells.asImmutable()
    }

    /**
     * @return The cell in the specified column or {@code null} if the argument is {@code null}
     */
    Cell getCell(Column column) {
        if (column != null) {
            assert column.spreadsheet == spreadsheet
            return cells.get(column.headerCell.columnIndex)
        }
        return null
    }

    /**
     * @return The cell in the column with the specified title or {@code null} if the spreadsheet does not contain a
     * column with that title
     */
    Cell getCellByColumnTitle(String columnTitle) {
        return getCell(spreadsheet.getColumn(columnTitle))
    }
}

class Column {

    final Cell headerCell

    protected Column(Cell headerCell) {
        this.headerCell = headerCell
    }

    Spreadsheet getSpreadsheet() {
        return headerCell.spreadsheet
    }
}

class Cell {

    private static final char[] LETTERS = 'A'..'Z'

    final Row row
    /**
     * The first column has index 0
     */
    final int columnIndex
    final String text

    protected Cell(Row row, int columnIndex, String text) {
        this.row = row
        this.columnIndex = columnIndex
        this.text = StringUtils.trimAndShortenWhitespace(text)
    }

    Spreadsheet getSpreadsheet() {
        return row.spreadsheet
    }

    int getRowIndex() {
        return row.rowIndex
    }

    String getCellAddress() {
        return "${columnAddress}${rowAddress}"
    }

    String getRowAddress() {
        return "${rowIndex + 1}"
    }

    String getColumnAddress() {
        return getColumnAddress(columnIndex)
    }

    @Override
    String toString() {
        return "${cellAddress}@${Integer.toHexString(hashCode())}"
    }

    /**
     * The column address as shown by spreadsheet software (A, B, ..., Z, AA, AB, ...)
     */
    static String getColumnAddress(int columnIndex) {
        StringBuilder sb = new StringBuilder()
        int col = columnIndex + 1
        while (col > LETTERS.length) {
            sb.insert(0, LETTERS[(col - 1) % LETTERS.length])
            col = (col - 1) / LETTERS.length
        }
        sb.insert(0, LETTERS[col - 1])
    }
}

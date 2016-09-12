package de.dkfz.tbi.util.spreadsheet

/**
 * An immutable in-memory representation of a tab-separated spreadsheet file
 *
 * <p>
 * Each row has at least as many cells as the header. If an input line has less columns than the header line, cells with
 * an empty string as text will be added at the end of the row.
 */
class Spreadsheet {

    final Row header
    final String delimiter
    private final Map<String, Column> columnsByTitle
    private final List<Row> dataRows

    Spreadsheet(String document, String delimiter = '\t') {
        this.delimiter = delimiter
        Map<String, Column> columnsByTitle = [:]
        List<Row> dataRows = []
        int rowIndex = 0
        List<String> lines = document.readLines()
        for (String line : lines) {
            Row row = new Row(this, rowIndex++, line)
            if (!header) {
                header = row
                for (Cell cell : header.cells) {
                    if (columnsByTitle.containsKey(cell.text)) {
                        throw new IllegalArgumentException("Duplicate column '${cell.text}'")
                    }
                    columnsByTitle.put(cell.text, new Column(cell))
                }
            } else {
                dataRows.add(row)
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

    protected Row(Spreadsheet spreadsheet, int rowIndex, String line) {
        this.spreadsheet = spreadsheet
        this.rowIndex = rowIndex
        int columnIndex = 0
        List<Cell> cells = line.split(spreadsheet.delimiter).collect{new Cell(this, columnIndex++, it)}
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
        } else {
            return null
        }
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
        this.text = text
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

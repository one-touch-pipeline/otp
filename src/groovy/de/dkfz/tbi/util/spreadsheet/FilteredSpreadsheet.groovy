package de.dkfz.tbi.util.spreadsheet

class FilteredSpreadsheet extends Spreadsheet {

    private final List<Row> dataRows

    FilteredSpreadsheet(String document, Closure<Boolean> dataRowFilter) {
        super(document)
        dataRows = super.dataRows.findAll(dataRowFilter).asImmutable()
    }

    @Override
    List<Row> getDataRows() {
        return dataRows
    }
}

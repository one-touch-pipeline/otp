package de.dkfz.tbi.util.spreadsheet

import spock.lang.Specification

class FilteredSpreadsheetSpec extends Specification {

    void 'dataRows, when document contains rows to be excluded, does not contain the excluded rows'() {
        when:
        Spreadsheet spreadsheet = new FilteredSpreadsheet("c 0 1 2 3 4".replaceAll(' ', '\n'), { Row row ->
            Integer.parseInt(row.getCellByColumnTitle('c').text) % 2 == 0
        })

        then:
        spreadsheet.dataRows.size() == 3
        spreadsheet.dataRows[0].cells[0].text == '0'
        spreadsheet.dataRows[1].cells[0].text == '2'
        spreadsheet.dataRows[2].cells[0].text == '4'
    }
}

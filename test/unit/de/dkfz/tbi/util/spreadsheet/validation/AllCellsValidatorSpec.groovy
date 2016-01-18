package de.dkfz.tbi.util.spreadsheet.validation

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import spock.lang.Specification

class AllCellsValidatorSpec extends Specification {

    void test() {

        given:
        Spreadsheet spreadsheet = new Spreadsheet('X\tY\nY\tX')
        Set xCells = [spreadsheet.header.cells[0], spreadsheet.dataRows[0].cells[1]] as Set
        Set yCells = [spreadsheet.header.cells[1], spreadsheet.dataRows[0].cells[0]] as Set
        ValidationContext context = new ValidationContext(spreadsheet)
        Collection calledFor = []
        AllCellsValidator validator = [
                validateValue: { ValidationContext ctx, String value, Set<Cell> cells ->
                    assert ctx == context
                    calledFor.add([value, cells])
                },
        ] as AllCellsValidator<ValidationContext>

        when:
        validator.validate(context)

        then:
        containSame(calledFor, [
                ['X', xCells],
                ['Y', yCells],
        ])
    }
}

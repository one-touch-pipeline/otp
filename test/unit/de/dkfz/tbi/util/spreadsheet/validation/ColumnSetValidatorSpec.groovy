package de.dkfz.tbi.util.spreadsheet.validation

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.util.spreadsheet.Column
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import spock.lang.Specification

class ColumnSetValidatorSpec extends Specification {

    Spreadsheet spreadsheet = new Spreadsheet('A1\tB1')
    Column a1 = spreadsheet.getColumn('A1')
    Column b1 = spreadsheet.getColumn('B1')
    ValidationContext context = new ValidationContext(spreadsheet)

    void test_findColumns_WithMissingMandatoryColumn() {

        given:
        ColumnSetValidator<ValidationContext> validator = [
                getColumnTitles: { ValidationContext context -> ['C1', 'B1', 'A1'] },
        ] as ColumnSetValidator<ValidationContext>

        when:
        List<Column> columns = validator.findColumns(context)
        then:
        columns == null
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == Level.ERROR
        problem.message == "Mandatory column 'C1' is missing."
    }

    void test_findColumns_WithMissingOptionalColumn() {

        given:
        ColumnSetValidator<ValidationContext> validator = new ColumnSetValidator<ValidationContext>() {
            @Override
            List<String> getColumnTitles(ValidationContext context) {
                return ['C1', 'B1', 'A1']
            }
            @Override
            void validate(ValidationContext context) {
                assert false : 'should not be called'
            }
            @Override
            boolean columnMissing(ValidationContext context, String columnTitle) {
                optionalColumnMissing(context, columnTitle)
                return true
            }
        }

        when:
        List<Column> columns = validator.findColumns(context)
        then:
        columns == [null, b1, a1]
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == Level.WARNING
        problem.message == "Optional column 'C1' is missing."
    }
}

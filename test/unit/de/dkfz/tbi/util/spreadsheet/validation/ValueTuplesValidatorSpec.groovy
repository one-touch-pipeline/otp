package de.dkfz.tbi.util.spreadsheet.validation

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import spock.lang.Specification

class ValueTuplesValidatorSpec extends Specification {

    Spreadsheet spreadsheet = new Spreadsheet(
            'A\tD\tB\n' +  // note that column 'D' comes before 'B' in the spreadsheet
            '2\tX\tY\n' +
            '3\tY\tX\n' +
            '4\tX\tX\n' +
            '5\tY\tX\n' +
            '6\tX\tX\n' +
            '7\tY\tY')
    Set<Cell> xxCells = [spreadsheet.dataRows[2].cells[1], spreadsheet.dataRows[2].cells[2],
                         spreadsheet.dataRows[4].cells[1], spreadsheet.dataRows[4].cells[2]] as Set
    Set<Cell> xyCells = [spreadsheet.dataRows[1].cells[1], spreadsheet.dataRows[1].cells[2],
                         spreadsheet.dataRows[3].cells[1], spreadsheet.dataRows[3].cells[2]] as Set
    Set<Cell> yxCells = [spreadsheet.dataRows[0].cells[1], spreadsheet.dataRows[0].cells[2]] as Set
    Set<Cell> yyCells = [spreadsheet.dataRows[5].cells[1], spreadsheet.dataRows[5].cells[2]] as Set
    ValidationContext context = new ValidationContext(spreadsheet)

    void test_ValueTuplesValidator_WithMissingMandatoryColumn() {

        given:
        ValueTuplesValidator<ValidationContext> validator = [
                getColumnTitles: { ValidationContext context -> ['B', 'C', 'D'] },
        ] as ValueTuplesValidator<ValidationContext>

        when:
        validator.validate(context)
        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == Level.ERROR
        problem.message == "Mandatory column 'C' is missing."
    }

    void test_ValueTuplesValidator_WithMissingOptionalColumn() {

        given:
        Collection<ValueTuple> calledFor = null
        ValueTuplesValidator<ValidationContext> validator = new ValueTuplesValidator<ValidationContext>() {
            @Override
            List<String> getColumnTitles(ValidationContext context) {
                return ['B', 'C', 'D']
            }
            @Override
            boolean columnMissing(ValidationContext context, String columnTitle) {
                optionalColumnMissing(context, columnTitle)
                return true
            }
            @Override
            void validateValueTuples(ValidationContext ctx, Collection<ValueTuple> valueTuples) {
                assert ctx == context
                assert calledFor == null
                calledFor = valueTuples
            }
        }

        when:
        validator.validate(context)
        then:
        Collection<ValueTuple> expected = [
                new ValueTuple([B: 'X', D: 'X'], xxCells),
                new ValueTuple([B: 'X', D: 'Y'], xyCells),
                new ValueTuple([B: 'Y', D: 'X'], yxCells),
                new ValueTuple([B: 'Y', D: 'Y'], yyCells),
        ]
        containSame(calledFor, expected)
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == Level.WARNING
        problem.message == "Optional column 'C' is missing."
    }

    void test_SingleValueValidator() {

        given:
        Collection calledFor = []
        SingleValueValidator<ValidationContext> validator = [
                getColumnTitle  : { 'B' },
                validateValue: { ValidationContext ctx, String value, Set<Cell> cells ->
                    assert ctx == context
                    calledFor.add([value, cells])
                },
        ] as SingleValueValidator<ValidationContext>

        when:
        validator.validate(context)
        then:
        Collection expected = [
                ['X', [spreadsheet.dataRows[1].cells[2], spreadsheet.dataRows[2].cells[2],
                       spreadsheet.dataRows[3].cells[2], spreadsheet.dataRows[4].cells[2]] as Set],
                ['Y', [spreadsheet.dataRows[0].cells[2], spreadsheet.dataRows[5].cells[2]] as Set],
        ]
        containSame(calledFor, expected)
    }
}

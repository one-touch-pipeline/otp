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
package de.dkfz.tbi.otp.utils.spreadsheet.validation

import spock.lang.Specification

import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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

    void test_ValueTuplesValidator_WithMissingRequiredColumn() {
        given:
        AbstractValueTuplesValidator<ValidationContext> validator = [
                getRequiredColumnTitles: { ValidationContext context -> ['B', 'C', 'D'] },
        ] as AbstractValueTuplesValidator<ValidationContext>

        when:
        validator.validate(context)
        then:
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == LogLevel.ERROR
        problem.message == "Required column 'C' is missing."
    }

    void test_ValueTuplesValidator_WithMissingOptionalColumn() {
        given:
        Collection<ValueTuple> calledFor = null
        AbstractValueTuplesValidator<ValidationContext> validator = new AbstractValueTuplesValidator<ValidationContext>() {
            @Override
            List<String> getRequiredColumnTitles(ValidationContext context) {
                return []
            }
            @Override
            List<String> getOptionalColumnTitles(ValidationContext context) {
                return ['B', 'C', 'D']
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
        problem.level == LogLevel.WARNING
        problem.message == "Optional column 'C' is missing."
    }

    void test_SingleValueValidator() {
        given:
        Collection calledFor = []
        AbstractSingleValueValidator<ValidationContext> validator = [
                getColumnTitle  : { 'B' },
                validateValue: { ValidationContext ctx, String value, Set<Cell> cells ->
                    assert ctx == context
                    calledFor.add([value, cells])
                },
        ] as AbstractSingleValueValidator<ValidationContext>

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

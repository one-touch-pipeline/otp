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
package de.dkfz.tbi.util.spreadsheet.validation

import spock.lang.Specification

import de.dkfz.tbi.util.spreadsheet.Column
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class ColumnSetValidatorSpec extends Specification {

    Spreadsheet spreadsheet = new Spreadsheet('A1\tB1')
    Column a1 = spreadsheet.getColumn('A1')
    Column b1 = spreadsheet.getColumn('B1')
    ValidationContext context = new ValidationContext(spreadsheet)

    void test_findColumns_WithMissingRequiredColumn() {
        given:
        AbstractColumnSetValidator<ValidationContext> validator = [
                getRequiredColumnTitles: { ValidationContext context -> ['C1', 'B1', 'A1'] },
        ] as AbstractColumnSetValidator<ValidationContext>

        when:
        validator.findColumns(context)

        then:
        thrown(ColumnsMissingException)
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == LogLevel.ERROR
        problem.message == "Required column 'C1' is missing."
    }

    void test_findColumns_WithMissingOptionalColumn() {
        given:
        AbstractColumnSetValidator<ValidationContext> validator = new AbstractColumnSetValidator<ValidationContext>() {
            @Override
            List<String> getRequiredColumnTitles(ValidationContext context) {
                return []
            }

            @Override
            List<String> getOptionalColumnTitles(ValidationContext context) {
                return ['C1', 'B1', 'A1']
            }

            @Override
            void validate(ValidationContext context) {
                assert false : 'should not be called'
            }
        }

        when:
        List<Column> columns = validator.findColumns(context)
        then:
        columns == [null, b1, a1]
        Problem problem = exactlyOneElement(context.problems)
        problem.affectedCells.empty
        problem.level == LogLevel.WARNING
        problem.message == "Optional column 'C1' is missing."
    }
}

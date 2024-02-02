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

import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Spreadsheet

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class AllCellsValidatorSpec extends Specification {

    void test() {
        given:
        Spreadsheet spreadsheet = new Spreadsheet('X\tY\nY\tX')
        Set xCells = [spreadsheet.header.cells[0], spreadsheet.dataRows[0].cells[1]] as Set
        Set yCells = [spreadsheet.header.cells[1], spreadsheet.dataRows[0].cells[0]] as Set
        ValidationContext context = new ValidationContext(spreadsheet)
        Collection calledFor = []
        AbstractAllCellsValidator validator = [
                validateValue: { ValidationContext ctx, String value, Set<Cell> cells ->
                    assert ctx == context
                    calledFor.add([value, cells])
                },
        ] as AbstractAllCellsValidator<ValidationContext>

        when:
        validator.validate(context)

        then:
        containSame(calledFor, [
                ['X', xCells],
                ['Y', yCells],
        ])
    }
}

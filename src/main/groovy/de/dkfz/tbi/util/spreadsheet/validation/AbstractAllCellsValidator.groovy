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

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Row

/**
 * Fetches all cells (including header cells), uniquifies their values and validates each of them once
 */
abstract class AbstractAllCellsValidator<C extends ValidationContext> implements Validator<C> {

    @Override
    void validate(C context) {
        Map<String, Set<Cell>> cellsByValue = [:]
        ([context.spreadsheet.header] + context.spreadsheet.dataRows)*.each { Row row ->
            row.cells.each { Cell cell ->
                assert CollectionUtils.getOrPut(cellsByValue, cell.text, new LinkedHashSet<Cell>()).add(cell)
            }
        }
        cellsByValue.each { String value, Set<Cell> cells ->
            validateValue(context, value, cells)
        }
    }

    /**
     * @param cells The cells in which the value appears
     */
    abstract void validateValue(C context, String value, Set<Cell> cells)
}

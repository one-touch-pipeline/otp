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

import groovy.transform.*

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.*

/**
 * Fetches the values in specified columns of all data rows, uniquifies these value tuples and validates the set
 */
abstract class AbstractValueTuplesValidator<C extends ValidationContext> extends AbstractColumnSetValidator<C> {

    @Override
    final void validate(C context) {
        List<Column> columns
        try {
            columns = findColumns(context)
        } catch (ColumnsMissingException ignored) {
            return
        }
        Map<Map<String, String>, Set<Cell>> cellsByValues = [:]
        context.spreadsheet.dataRows.each { Row row ->
            Map<Column, Cell> cellsByColumn = columns.findAll().collectEntries { [it, row.getCell(it)] }
            Map<String, String> valuesByColumnTitle = cellsByColumn.collectEntries { column, cell -> [column.headerCell.text, cell.text] }
            CollectionUtils.getOrPut(cellsByValues, valuesByColumnTitle, new LinkedHashSet<Cell>()).addAll(cellsByColumn.values())
        }
        validateValueTuples(context, cellsByValues.collect { Map<String, String> valuesByColumnTitle, Set<Cell> cells ->
            new ValueTuple(valuesByColumnTitle.asImmutable(), cells.asImmutable())
        }.asImmutable())
    }

    /**
     * Called by {@link #validate(C)} once with all the unique value tuples in the spreadsheet
     */
    abstract void validateValueTuples(C context, Collection<ValueTuple> valueTuples)
}

@TupleConstructor
@EqualsAndHashCode
@ToString
class ValueTuple {

    final Map<String, String> valuesByColumnTitle

    String getValue(String columnTitle) {
        return valuesByColumnTitle.get(columnTitle)
    }

    /**
     * The cells in which the values appear
     */
    final Set<Cell> cells
}

abstract class AbstractSingleValueValidator<C extends ValidationContext> extends AbstractValueTuplesValidator<C> {

    @Override
    final List<String> getRequiredColumnTitles(C context) {
        return [getColumnTitle(context)]
    }

    abstract String getColumnTitle(C context)

    @Override
    final void checkMissingRequiredColumn(C context, String columnTitle) {
        checkColumn(context)
    }

    void checkColumn(C context) {
        super.checkMissingRequiredColumn(context, getColumnTitle(context))
    }

    @Override
    final void validateValueTuples(C context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            validateValue(context, CollectionUtils.exactlyOneElement(it.valuesByColumnTitle.values()), it.cells)
        }
    }

    abstract void validateValue(C context, String value, Set<Cell> cells)
}

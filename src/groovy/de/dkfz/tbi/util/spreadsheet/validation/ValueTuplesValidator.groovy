package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Column
import de.dkfz.tbi.util.spreadsheet.Row
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * Fetches the values in specified columns of all data rows, uniquifies these value tuples and validates the set
 */
abstract class ValueTuplesValidator<C extends ValidationContext> extends ColumnSetValidator<C> {

    @Override
    void validate(C context) {
        List<Column> columns = findColumns(context)
        if (columns != null) {
            Map<Map<String, String>, Set<Cell>> cellsByValues = [:]
            context.spreadsheet.dataRows.each { Row row ->
                Map<Column, Cell> cellsByColumn = columns.findAll().collectEntries { [it, row.getCell(it)] }
                Map<String, String> valuesByColumnTitle = cellsByColumn.collectEntries { column, cell -> [column.headerCell.text, cell.text ] }
                CollectionUtils.getOrPut(cellsByValues, valuesByColumnTitle, new LinkedHashSet<Cell>()).addAll(cellsByColumn.values())
            }
            validateValueTuples(context, cellsByValues.collect { Map<String, String> valuesByColumnTitle, Set<Cell> cells ->
                new ValueTuple(valuesByColumnTitle.asImmutable(), cells.asImmutable())
            }.asImmutable())
        }
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

abstract class SingleValueValidator<C extends ValidationContext> extends ValueTuplesValidator<C> {

    @Override
    List<String> getColumnTitles(C context) {
        return [getColumnTitle(context)]
    }

    abstract String getColumnTitle(C context)

    @Override
    void validateValueTuples(C context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            validateValue(context, CollectionUtils.exactlyOneElement(it.valuesByColumnTitle.values()), it.cells)
        }
    }

    abstract void validateValue(C context, String value, Set<Cell> cells)
}

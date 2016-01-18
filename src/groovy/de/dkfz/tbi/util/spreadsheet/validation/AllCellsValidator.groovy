package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Row

/**
 * Fetches all cells (including header cells), uniquifies their values and validates each of them once
 */
abstract class AllCellsValidator<C extends ValidationContext> implements Validator<C> {

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

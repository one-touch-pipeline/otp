package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.util.spreadsheet.Column

/**
 * A base class for validators which validate values from a specific set of columns
 */
abstract class ColumnSetValidator<C extends ValidationContext> implements Validator<C> {

    /**
     * The titles of the columns which this validator is interested in
     */
    abstract List<String> getColumnTitles(C context)

    /**
     * @return The columns in the same order as returned by {@link #getColumnTitles(C)}. Contains {@code null} in place of
     * missing columns. Or {@code null} if the validator cannot continue because of missing columns
     */
    List<Column> findColumns(C context) {
        List<Column> columns = []
        Collection<String> missingColumns = []
        getColumnTitles(context).each {
            Column column = context.spreadsheet.getColumn(it)
            columns.add(column)
            if (column == null) {
                missingColumns.add(it)
            }
        }
        if (!missingColumns.empty) {
            if (!columnsMissing(context, missingColumns)) {
                return null
            }
        }
        return columns
    }

    /**
     * Called if columns returned by {@link #getColumnTitles(C)} are missing
     *
     * <p>
     * May be overridden.
     *
     * @param columnTitles The titles of the missing columns
     * @return Whether the validator can continue even though the columns are missing
     */
    boolean columnsMissing(C context, Collection<String> columnTitles) {
        boolean canContinue = true
        columnTitles.each {
            if (!columnMissing(context, it)) {
                canContinue = false
            }
        }
        return canContinue
    }

    /**
     * Called by {@link #columnsMissing(C, Collection)} for every missing column
     *
     * <p>
     * May be overridden.
     *
     * @param columnTitle The title of a missing column
     * @return Whether the validator can continue even though the column is missing
     */
    boolean columnMissing(C context, String columnTitle) {
        mandatoryColumnMissing(context, columnTitle)
        return false
    }

    void mandatoryColumnMissing(C context, String columnTitle) {
        context.addProblem(Collections.emptySet(), Level.ERROR, "Mandatory column '${columnTitle}' is missing.")
    }

    void optionalColumnMissing(C context, String columnTitle, String additionalWarningMessage = '') {
        context.addProblem(Collections.emptySet(), Level.WARNING, "Optional column '${columnTitle}' is missing.${additionalWarningMessage}")
    }
}

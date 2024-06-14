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

import groovy.transform.InheritConstructors

import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.utils.spreadsheet.Column

/**
 * A base class for validators which validate values from a specific set of columns
 */
abstract class AbstractColumnSetValidator<C extends ValidationContext> implements Validator<C> {

    /**
     * A List of all Columns that are required for the Validator to run
     */
    abstract List<String> getRequiredColumnTitles(C context)

    /**
     * A List of all Columns that not are required for the Validator to run, but are needed for certain validation
     */
    @SuppressWarnings("UnusedMethodParameter")
    List<String> getOptionalColumnTitles(C context) {
        return []
    }

    /**
     * @return The columns in the same order as returned by {@link #getRequiredColumnTitles(C)} and {@link #getOptionalColumnTitles(C)}. Contains {@code null}
     * in place of missing columns.
     * @throws ColumnsMissingException if the validator cannot continue because of missing columns
     */
    final List<Column> findColumns(C context) {
        List<Column> columns = []
        Collection<String> missingColumns = []
        getRequiredColumnTitles(context).each {
            Column column = context.spreadsheet.getColumn(it)
            columns << column
            if (column == null) {
                missingColumns.add(it)
                checkMissingRequiredColumn(context, it)
            }
        }
        if (!missingColumns.empty) {
            throw new ColumnsMissingException("Can't continue because of missing columns")
        }
        getOptionalColumnTitles(context).each {
            Column column = context.spreadsheet.getColumn(it)
            columns << column
            if (column == null) {
                checkMissingOptionalColumn(context, it)
            }
        }
        return columns
    }

    /**
     * Called for each column returned by {@link #getRequiredColumnTitles(C)} that is missing.
     * Calls {@link #addErrorForMissingRequiredColumn(C, String)}
     *
     * Overwrite when this is not the desired behaviour
     */
    void checkMissingRequiredColumn(C context, String columnTitle) {
        addErrorForMissingRequiredColumn(context, columnTitle)
    }

    static final void addErrorForMissingRequiredColumn(ValidationContext context, String columnTitle) {
        context.addProblem(Collections.emptySet(), LogLevel.ERROR, "Required column '${columnTitle}' is missing.")
    }

    /**
     * Called for each column returned by {@link #getOptionalColumnTitles(C)} that is missing.
     * Calls {@link #addWarningForMissingOptionalColumn(C, String)}
     *
     * Overwrite when this is not the desired behaviour
     */
    void checkMissingOptionalColumn(C context, String columnTitle) {
        addWarningForMissingOptionalColumn(context, columnTitle)
    }

    static final void addWarningForMissingOptionalColumn(ValidationContext context, String columnTitle, String additionalWarningMessage = '') {
        context.addProblem(Collections.emptySet(), LogLevel.WARNING, "Optional column '${columnTitle}' is missing. ${additionalWarningMessage}".trim())
    }
}

@InheritConstructors
class ColumnsMissingException extends OtpRuntimeException {
}

package de.dkfz.tbi.util.spreadsheet.validation

import de.dkfz.tbi.util.spreadsheet.Spreadsheet

/**
 * Validates a {@link Spreadsheet}
 */
interface Validator<C extends ValidationContext> {

    void validate(C context)
}

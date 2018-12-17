package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import java.util.regex.Matcher

@Component
class LibraryValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    final String REGEX = /^(?:lib(?:[1-9]\d*|NA)|)$/

    @Override
    Collection<String> getDescriptions() {
        return ['The Library contains only valid characters.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.CUSTOMER_LIBRARY.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) { }

    @Override
    void validateValue(MetadataValidationContext context, String library, Set<Cell> cells) {
        if (library) {
            Matcher matcher = library =~ REGEX
            if (!OtpPath.isValidPathComponent(library)) {
                context.addProblem(cells, Level.ERROR, "Library '${library}' contains invalid characters.", "At least one library contains invalid characters.")
            } else if (!matcher) {
                context.addProblem(cells, Level.WARNING, "Library '${library}' does not match regular expression '${REGEX}'.", "At least one Library does not match regular expression '${REGEX}'.")
            }
        }
    }
}

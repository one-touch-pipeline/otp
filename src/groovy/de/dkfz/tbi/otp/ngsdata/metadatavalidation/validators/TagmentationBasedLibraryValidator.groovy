package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_BASED_LIBRARY

@Component
class TagmentationBasedLibraryValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The tagmentation based library value is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return TAGMENTATION_BASED_LIBRARY.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
        optionalColumnMissing(context, TAGMENTATION_BASED_LIBRARY.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String tagmentationBasedLibrary, Set<Cell> cells) {
        if (!(tagmentationBasedLibrary.toLowerCase() in ["", "1", "true", "false"])) {
           context.addProblem(cells, Level.ERROR, "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of '${tagmentationBasedLibrary}'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
        if (!(tagmentationBasedLibrary.toLowerCase() in ["", "1", "true"])) {
           context.addProblem(cells, Level.ERROR, "The tagmentation based library column value should be '1' or an empty string instead of '${tagmentationBasedLibrary}'")
        }
    }
}

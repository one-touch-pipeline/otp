package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.LibraryLayout
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class LibraryLayoutValidator extends SingleValueValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The library layout is registered in OTP.']
    }

    @Override
    String getColumnTitle(AbstractMetadataValidationContext context) {
        return MetaDataColumn.LIBRARY_LAYOUT.name()
    }

    @Override
    void validateValue(AbstractMetadataValidationContext context, String libraryLayoutName, Set<Cell> cells) {
        LibraryLayout libraryLayout = LibraryLayout.findByName(libraryLayoutName)
        if (!libraryLayout) {
            context.addProblem(cells, Level.ERROR, "Library layout '${libraryLayoutName}' is not registered in OTP.", "At least one library layout is not registered in OTP.")
        }
    }
}

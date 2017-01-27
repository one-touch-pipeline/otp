package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

@Component
class LibraryLayoutValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The library layout is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.LIBRARY_LAYOUT.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String libraryLayoutName, Set<Cell> cells) {
        if (!SeqType.findByLibraryLayout(libraryLayoutName)) {
            context.addProblem(cells, Level.ERROR, "Library layout '${libraryLayoutName}' is not registered in the OTP database.")
        }
    }
}

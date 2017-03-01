package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

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
        if (!SeqType.findByLibraryLayout(libraryLayoutName)) {
            context.addProblem(cells, Level.ERROR, "Library layout '${libraryLayoutName}' is not registered in OTP.")
        }
    }
}

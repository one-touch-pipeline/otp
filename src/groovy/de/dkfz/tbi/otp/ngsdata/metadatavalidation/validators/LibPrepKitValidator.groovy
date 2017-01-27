package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibPrepKitValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Override
    Collection<String> getDescriptions() {
        return ["The library preparation kit is registered in the OTP database or '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' or empty."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return LIB_PREP_KIT.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
        optionalColumnMissing(context, LIB_PREP_KIT.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if (!(value == "" ||
                value == InformationReliability.UNKNOWN_VERIFIED.rawValue ||
                libraryPreparationKitService.findLibraryPreparationKitByNameOrAlias(value))) {
            context.addProblem(cells, Level.ERROR, "The library preparation kit '${value}' is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.")
        }
    }
}

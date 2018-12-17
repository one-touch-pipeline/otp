package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKitService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LIB_PREP_KIT

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
        if (value == "" || value == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
            context.addProblem(cells, Level.WARNING, "The library preparation kit column is ${value ?: 'empty'}", "At least one library preparation kit is ${value ?: 'empty'}")
        } else if (!libraryPreparationKitService.findByNameOrImportAlias(value)) {
            context.addProblem(cells, Level.ERROR, "The library preparation kit '${value}' is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.", "At least one library preparation kit is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.")
        }
    }
}

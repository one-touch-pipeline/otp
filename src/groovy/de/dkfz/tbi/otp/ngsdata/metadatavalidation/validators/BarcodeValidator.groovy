package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BARCODE

@Component
class BarcodeValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    final static String REGEX = /^[ACGT]{6,8}|[0-9]{3}$/

    @Override
    Collection<String> getDescriptions() {
        return ["Barcodes should match the regular expression '${REGEX}'."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return BARCODE.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
        optionalColumnMissing(context, BARCODE.name(), " OTP will try to parse the barcodes from the filenames.")
    }

    @Override
    void validateValue(MetadataValidationContext context, String barcode, Set<Cell> cells) {
        if (!(barcode ==~ /^[0-9a-zA-Z]*$/)) {
            context.addProblem(cells, Level.ERROR, "'${barcode}' is not a well-formed barcode. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should match the regular expression '${REGEX}'.")
        } else if (!(barcode ==~ REGEX) && !barcode.empty) {
            context.addProblem(cells, Level.WARNING, "The barcode '${barcode}' has an unusual format. It should match the regular expression '${REGEX}'.")
        }
    }
}

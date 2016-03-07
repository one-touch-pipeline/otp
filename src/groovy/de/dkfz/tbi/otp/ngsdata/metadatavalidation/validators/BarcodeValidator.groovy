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
        return ['The barcode is empty or matches the regular expression ${REGEX}.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return BARCODE.name()
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        optionalColumnMissing(context, columnTitle, " OTP will try to parse the barcodes from the filenames.")
        return false
    }

    @Override
    void validateValue(MetadataValidationContext context, String barcode, Set<Cell> cells) {
        if (!(barcode == "" || barcode ==~ REGEX )) {
            context.addProblem(cells, Level.WARNING, "The barcode '${barcode}' does not match the usually used regular expression: '${REGEX}.'")
        }
    }
}

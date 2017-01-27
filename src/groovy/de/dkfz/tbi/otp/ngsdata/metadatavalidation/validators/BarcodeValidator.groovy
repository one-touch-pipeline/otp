package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class BarcodeValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    final static String MUST_REGEX = /^[0-9a-zA-Z\-\+\.]*$/
    final static String SHOULD_REGEX = /^[ACGT]{6,8}|[0-9]{3}$/

    @Override
    Collection<String> getDescriptions() {
        return ["Barcodes should match the regular expression '${SHOULD_REGEX}'."]
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
        if (!(barcode ==~ MUST_REGEX)) {
            context.addProblem(cells, Level.ERROR, "'${barcode}' is not a well-formed barcode. It must match the regular expression '${MUST_REGEX}'. It should match the regular expression '${SHOULD_REGEX}'.")
        } else if (!(barcode ==~ SHOULD_REGEX) && !barcode.empty) {
            context.addProblem(cells, Level.WARNING, "The barcode '${barcode}' has an unusual format. It should match the regular expression '${SHOULD_REGEX}'.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.WITHDRAWN

@Component
class WithdrawnValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    static final String NONE = 'NONE'

    @Override
    Collection<String> getDescriptions() {
        return ["If a '${WITHDRAWN}' column exists, it must only contain cells which are empty or contain '0' or contain 'None'."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return WITHDRAWN.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        final String uppercaseValue = value.toUpperCase(Locale.ENGLISH)
        if (value != "" && value != "0" && uppercaseValue != NONE) {
            context.addProblem(cells, Level.ERROR, "'${value}' is not an acceptable '${WITHDRAWN}' value. It must be empty or '0' or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP.")
        }
    }
}

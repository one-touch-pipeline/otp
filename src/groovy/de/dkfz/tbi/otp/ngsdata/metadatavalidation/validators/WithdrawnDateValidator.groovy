package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*


@Component
class WithdrawnDateValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    static final String NONE = 'NONE'

    @Override
    Collection<String> getDescriptions() {
        return ["If a '${WITHDRAWN_DATE}' column exists, it must only contain cells which are empty or contain 'None'."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return WITHDRAWN_DATE.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        final String uppercaseValue = value.toUpperCase(Locale.ENGLISH)
        if (value != "" && uppercaseValue != NONE) {
            context.addProblem(cells, Level.ERROR, "'${value}' is not an acceptable '${WITHDRAWN_DATE}' value. It must be empty or 'None'. Withdrawn data cannot be imported into OTP.")
        }
    }
}

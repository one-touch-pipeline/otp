package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*


@Component
class WithdrawnDateValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["If a '${WITHDRAWN_DATE}' column exists, it must be empty."]
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
        if(value != "") {
            context.addProblem(cells, Level.ERROR, "Withdrawn data cannot be imported into OTP.")
        }
    }
}

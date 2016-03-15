package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.AntibodyTarget
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET
import org.springframework.stereotype.Component


@Component
class AntibodyTargetValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {


    @Override
    Collection<String> getDescriptions() {
        return ["The antibody target is registered in the OTP database (case-insensitive) or empty."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return ANTIBODY_TARGET.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {}

    @Override
    void validateValue(MetadataValidationContext context, String antibodyTarget, Set<Cell> cells) {

        if (antibodyTarget && !AntibodyTarget.findByNameIlike(antibodyTarget.replaceAll(/([\\_%])/, /\\$1/))) {
            context.addProblem(cells, Level.ERROR, "The antibody target '${antibodyTarget}' is not registered in OTP.")
        }
    }
}

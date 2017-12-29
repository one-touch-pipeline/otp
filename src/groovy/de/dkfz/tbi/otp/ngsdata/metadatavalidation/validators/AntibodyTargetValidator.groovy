package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.StringUtils.*

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

        if (antibodyTarget && !AntibodyTarget.findByNameIlike(escapeForSqlLike(antibodyTarget))) {
            context.addProblem(cells, Level.ERROR, "The antibody target '${antibodyTarget}' is not registered in OTP.", "At least one antibody target is not registered in OTP.")
        }
    }
}

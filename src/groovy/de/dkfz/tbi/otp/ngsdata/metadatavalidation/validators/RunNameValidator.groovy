package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID


class RunNameValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The run name is a valid directory name."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return RUN_ID.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String runName, Set<Cell> cells) {
        if (!OtpPath.isValidPathComponent(runName)) {
            context.addProblem(cells, Level.ERROR, "The run name '${runName}' is not a valid directory name.")
        }
    }
}

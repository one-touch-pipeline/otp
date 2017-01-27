package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
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

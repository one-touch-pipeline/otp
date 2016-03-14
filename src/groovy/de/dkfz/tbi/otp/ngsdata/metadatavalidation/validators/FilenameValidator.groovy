package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE

@Component
class FilenameValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The filename ends with '.gz'.",
                "The filename contains 'fastq'.",
                "The filename contains only legal characters."
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return FASTQ_FILE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String filename, Set<Cell> cells) {
        if (!filename.endsWith('.gz')) {
            context.addProblem(cells, Level.ERROR, "Filename must end with '.gz'.")
        }
        if (!filename.contains('fastq')) {
            context.addProblem(cells, Level.WARNING, "Filename should contain 'fastq'.")
        }
        if (!OtpPath.isValidPathComponent(filename)) {
            context.addProblem(cells, Level.ERROR, "Filename contains invalid characters.")
        }
    }
}

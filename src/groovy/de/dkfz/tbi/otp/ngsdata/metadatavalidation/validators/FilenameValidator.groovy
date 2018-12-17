package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE

@Component
class FilenameValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The filename must end with '.gz'.",
                "The filename must contain '_fastq' or '.fastq'.",
                "The filename must contain only legal characters.",
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return FASTQ_FILE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String filename, Set<Cell> cells) {
        if (!filename.endsWith('.gz')) {
            context.addProblem(cells, Level.ERROR, "Filename '${filename}' does not end with '.gz'.", "At least one filename does not end with '.gz'.")
        }
        try {
            FileTypeService.getFileType(filename, FileType.Type.SEQUENCE)
        } catch (FileTypeUndefinedException e) {
            context.addProblem(cells, Level.ERROR, "Filename '${filename}' contains neither '_fastq' nor '.fastq'.", "At least one filename contains neither '_fastq' nor '.fastq'.")
        }
        if (!(OtpPath.isValidPathComponent(filename) || OtpPath.isValidAbsolutePath(filename))) {
            context.addProblem(cells, Level.ERROR, "Filename '${filename}' contains invalid characters.", "At least one filename contains invalid characters.")
        }
    }
}

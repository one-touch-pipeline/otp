package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.*

@Component
class BamFilePathValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The bam file must be an absolute path",
                "The bam file path must end with '.bam'.",
                "The bam file path must contain only legal characters."
        ]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.BAM_FILE_PATH.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String filePath, Set<Cell> cells) {
        if (!filePath.endsWith(".bam")) {
            context.addProblem(cells, Level.ERROR, "Filename '${filePath}' does not end with '.bam'.")
        }
        if (!OtpPath.isValidAbsolutePath(filePath)) {
            context.addProblem(cells, Level.ERROR, "The path '${filePath}' is not an absolute path.")
        } else {
            try {
                File bamFile = new File(filePath)
                if (!bamFile.isFile()) {
                    if (!bamFile.exists()) {
                        context.addProblem(cells, Level.ERROR, "'${filePath}' does not exist or cannot be accessed by OTP.")
                    } else {
                        context.addProblem(cells, Level.ERROR, "'${filePath}' is not a file.")
                    }
                } else if (!bamFile.canRead()) {
                    context.addProblem(cells, Level.ERROR, "'${filePath}' is not readable.")
                }
            } catch (Exception e) {
                context.addProblem(Collections.emptySet(), Level.ERROR, e.message)
            }
        }
    }
}

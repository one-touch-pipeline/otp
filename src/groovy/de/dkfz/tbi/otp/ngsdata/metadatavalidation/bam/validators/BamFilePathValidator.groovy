package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import java.nio.file.*

@Component
class BamFilePathValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The bam file must be an absolute path",
                "The bam file path must end with '.bam'.",
                "The bam file path must contain only legal characters.",
        ]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.BAM_FILE_PATH.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String filePath, Set<Cell> cells) {
        if (!filePath.endsWith(".bam")) {
            context.addProblem(cells, Level.ERROR, "Filename '${filePath}' does not end with '.bam'.", "At least one filename does not end with '.bam'.")
        }
        if (!OtpPath.isValidAbsolutePath(filePath)) {
            context.addProblem(cells, Level.ERROR, "The path '${filePath}' is not an absolute path.", "At least one path is not an absolute path.")
        } else {
            try {
                Path bamFile = context.fileSystem.getPath(filePath)
                if (!Files.isRegularFile(bamFile)) {
                    if (!Files.exists(bamFile)) {
                        context.addProblem(cells, Level.ERROR, "'${filePath}' does not exist or cannot be accessed by OTP.","At least one file does not exist or cannot be accessed by OTP.")
                    } else {
                        context.addProblem(cells, Level.ERROR, "'${filePath}' is not a file.", "At least one file is not a file.")
                    }
                } else if (!Files.isReadable(bamFile)) {
                    context.addProblem(cells, Level.ERROR, "'${filePath}' is not readable.", "At least one file is not readable.")
                }
            } catch (Exception e) {
                context.addProblem(Collections.emptySet(), Level.ERROR, e.message)
            }
        }
    }
}

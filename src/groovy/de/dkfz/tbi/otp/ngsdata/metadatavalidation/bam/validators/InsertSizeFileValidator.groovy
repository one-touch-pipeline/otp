package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.BAM_FILE_PATH
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.INSERT_SIZE_FILE

@Component
class InsertSizeFileValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The insert size file must be an absolute path"
        ]
    }

    @Override
    List<String> getColumnTitles(BamMetadataValidationContext context) {
        return [INSERT_SIZE_FILE.name(), BAM_FILE_PATH.name()]
    }

    @Override
    void mandatoryColumnMissing(BamMetadataValidationContext context, String columnTitle) {
        context.addProblem(Collections.emptySet(), Level.WARNING, "'${INSERT_SIZE_FILE.name()}' has to be set for Sophia", "'${INSERT_SIZE_FILE.name()}' has to be set for Sophia")
    }

    @Override
    boolean columnMissing(BamMetadataValidationContext context, String columnTitle) {
        if (columnTitle != BAM_FILE_PATH.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {

        valueTuples.each {
            String bamFile = it.getValue(BAM_FILE_PATH.name())
            String insertSizeFile = it.getValue(INSERT_SIZE_FILE.name())

            if (insertSizeFile.isEmpty()) {
                context.addProblem(it.cells, Level.WARNING, "${INSERT_SIZE_FILE} has to be set for Sophia",
                        "${INSERT_SIZE_FILE} file has to be set for Sophia")
            } else {
                if (!OtpPath.isValidRelativePath(insertSizeFile)) {
                    context.addProblem(it.cells, Level.ERROR, "The path '${insertSizeFile}' is not a relative path.",
                            "At least one path is not a relative path.")
                } else {
                    try {
                        if (bamFile != null && !bamFile.isEmpty()) {
                            Path bamFilePath = context.fileSystem.getPath(bamFile)
                            if (bamFilePath) {
                                if (OtpPath.isValidAbsolutePath(bamFile)) {
                                    Path insertSizeFilePath = bamFilePath.resolveSibling(insertSizeFile)

                                    if (!Files.isRegularFile(insertSizeFilePath)) {
                                        if (!Files.exists(insertSizeFilePath)) {
                                            context.addProblem(it.cells, Level.ERROR,
                                                    "'${insertSizeFile}' does not exist or cannot be accessed by OTP.",
                                                    "At least one file does not exist or cannot be accessed by OTP.")
                                        } else {
                                            context.addProblem(it.cells, Level.ERROR,
                                                    "'${insertSizeFile}' is not a file.", "At least one file is not a file.")
                                        }
                                    } else if (!Files.isReadable(insertSizeFilePath)) {
                                        context.addProblem(it.cells, Level.ERROR,
                                                "'${insertSizeFile}' is not readable.", "At least one file is not readable.")
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        context.addProblem(Collections.emptySet(), Level.ERROR, e.message)
                    }
                }
            }
        }
    }
}

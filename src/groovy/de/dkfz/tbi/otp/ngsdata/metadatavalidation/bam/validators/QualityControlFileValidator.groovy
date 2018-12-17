package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import groovy.json.JsonSlurper
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.BAM_FILE_PATH
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.QUALITY_CONTROL_FILE

@Component
class QualityControlFileValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The quality control file must be an relative path",
        ]
    }

    @Override
    List<String> getColumnTitles(BamMetadataValidationContext context) {
        return [QUALITY_CONTROL_FILE.name(), BAM_FILE_PATH.name()]
    }

    @Override
    void optionalColumnMissing(BamMetadataValidationContext context, String columnTitle) {
        context.addProblem(Collections.emptySet(), Level.WARNING, "'${QUALITY_CONTROL_FILE.name()}' has to be set for Sophia",
                "'${QUALITY_CONTROL_FILE.name()}' has to be set for Sophia")
    }

    @Override
    boolean columnMissing(BamMetadataValidationContext context, String columnTitle) {
        if (columnTitle == BAM_FILE_PATH.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
        if (columnTitle == QUALITY_CONTROL_FILE.name()) {
            optionalColumnMissing(context, columnTitle)
        }
        return true
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {

        valueTuples.each {
            String bamFile = it.getValue(BAM_FILE_PATH.name())
            String qualityControlFile = it.getValue(QUALITY_CONTROL_FILE.name())

            if (!qualityControlFile) {
                context.addProblem(it.cells, Level.WARNING,
                        "${QUALITY_CONTROL_FILE} has to be set for Sophia",
                        "${QUALITY_CONTROL_FILE} file has to be set for Sophia")
                return
            }
            if (!OtpPath.isValidRelativePath(qualityControlFile)) {
                context.addProblem(it.cells, Level.ERROR,
                        "The path '${qualityControlFile}' is not a relative path.",
                        "At least one path is not a relative path.")
                return
            }
            if (!bamFile || !OtpPath.isValidAbsolutePath(bamFile)) {
                return
            }

            Path qualityControlFilePath = context.fileSystem.getPath(bamFile).resolveSibling(qualityControlFile)
            if (!Files.exists(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' does not exist or cannot be accessed by OTP.",
                        "At least one file does not exist or cannot be accessed by OTP.")
            } else if (!Files.isRegularFile(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' is not a file.",
                        "At least one file is not a file.")
            } else if (!Files.isReadable(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' is not readable.",
                        "At least one file is not readable.")
            } else {
                Object qcValues
                try {
                    qcValues = new JsonSlurper().parse(qualityControlFilePath.bytes)
                } catch (IOException ex) {
                    context.addProblem(it.cells, Level.ERROR, ex.message)
                    return
                } catch (Exception ex) {
                    context.addProblem(it.cells, Level.ERROR,
                            "'${qualityControlFile}' has no valid JSON structure.",
                            "At least one file has no valid JSON structure.")
                    return
                }
                try {
                    assert qcValues.all
                    assert qcValues.all.properlyPaired
                    assert qcValues.all.pairedInSequencing
                    assert qcValues.all.insertSizeMedian
                    assert qcValues.all.insertSizeCV
                } catch (AssertionError | Exception ex) {
                    context.addProblem(it.cells, Level.ERROR,
                            "'${qualityControlFile}' has not all needed values.",
                            "At least one value is missing.")
                }
            }
        }
    }
}

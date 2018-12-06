package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import groovy.json.*
import org.springframework.stereotype.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*

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

            if (qualityControlFile.isEmpty()) {
                context.addProblem(it.cells, Level.WARNING, "${QUALITY_CONTROL_FILE} has to be set for Sophia",
                        "${QUALITY_CONTROL_FILE} file has to be set for Sophia")
            }else{
                if (OtpPath.isValidRelativePath(qualityControlFile)) {
                    try {
                        if (bamFile != null && !bamFile.isEmpty()) {
                            Path bamFilePath = context.fileSystem.getPath(bamFile)
                            if (bamFilePath && OtpPath.isValidAbsolutePath(bamFile)) {
                                Path qualityControlFilePath = bamFilePath.resolveSibling(qualityControlFile)

                                if (!Files.isRegularFile(qualityControlFilePath)) {
                                    if (Files.exists(qualityControlFilePath)) {
                                        context.addProblem(it.cells, Level.ERROR,
                                                "'${qualityControlFile}' is not a file.", "At least one file is not a file.")
                                    } else {
                                        context.addProblem(it.cells, Level.ERROR,
                                                "'${qualityControlFile}' does not exist or cannot be accessed by OTP.",
                                                "At least one file does not exist or cannot be accessed by OTP.")
                                    }
                                } else if (!Files.isReadable(qualityControlFilePath)) {
                                    context.addProblem(it.cells, Level.ERROR,
                                            "'${qualityControlFile}' is not readable.", "At least one file is not readable.")
                                } else {
                                    try {
                                        Object qcValues = new JsonSlurper().parse(qualityControlFilePath.bytes)
                                        assert qcValues.all.properlyPaired
                                        assert qcValues.all.pairedInSequencing
                                        assert qcValues.all.insertSizeMedian
                                        assert qcValues.all.insertSizeCV
                                    } catch (IOException ex) {
                                        context.addProblem(it.cells, Level.ERROR, ex.message)
                                    } catch (AssertionError ex) {
                                        context.addProblem(it.cells, Level.ERROR,
                                                "'${qualityControlFile}' has not all needed values or has no valid JSON structure.",
                                                "At least one value is missing or has no valid JSON structure.")
                                    } catch (Exception ex) {
                                        context.addProblem(it.cells, Level.ERROR,
                                                "'${qualityControlFile}' has not all needed values or has no valid JSON structure.",
                                                "At least one value is missing or has no valid JSON structure.")
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        context.addProblem(it.cells, Level.ERROR, e.message)
                    }
                } else {
                    context.addProblem(it.cells, Level.ERROR, "The path '${qualityControlFile}' is not a relative path.",
                            "At least one path is not a relative path.")
                }
            }
        }
    }
}

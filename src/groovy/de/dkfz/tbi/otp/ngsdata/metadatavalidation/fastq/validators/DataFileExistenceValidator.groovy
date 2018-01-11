package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import groovyx.gpars.*
import org.springframework.stereotype.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext.*

@Component
class DataFileExistenceValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'The referenced FASTQ files must exist on the file system.',
                'Different rows should not reference the same FASTQ file.',
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return context.directoryStructure.columnTitles
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {

        boolean directoryStructureInfoAdded = false
        Closure addDirectoryStructureInfo = {
            if (!directoryStructureInfoAdded) {
                context.addProblem(Collections.emptySet(), Level.INFO,
                        "Using directory structure '${context.directoryStructure.description}'. If this is incorrect, please select the correct one.")
                directoryStructureInfoAdded = true
            }
        }

        Object sync = new Object()

        GParsPool.withPool(16) {
            allValueTuples.groupBy { context.directoryStructure.getDataFilePath(context, it)
            }.findAll { it.key != null }.eachParallel { Path path, List<ValueTuple> valueTuples ->
                if (valueTuples*.cells.sum()*.row.unique().size() != 1) {
                    synchronized (sync) {
                        addDirectoryStructureInfo()
                        context.addProblem((Set<Cell>)valueTuples*.cells.sum(), Level.WARNING,
                                "Multiple rows reference the same file '${path}'.", "Multiple rows reference the same file.")
                    }
                }
                String message = null
                if (!Files.isRegularFile(path)) {
                    if (!Files.exists(path)) {
                        message = "${pathForMessage(path)} does not exist or cannot be accessed by OTP."
                    } else {
                        message = "${pathForMessage(path)} is not a file."
                    }
                } else if (Files.size(path) == 0L) {
                    message = "${pathForMessage(path)} is empty."
                }
                if (message) {
                    synchronized (sync) {
                        addDirectoryStructureInfo()
                        context.addProblem((Set<Cell>)valueTuples*.cells.sum(), Level.ERROR, message, "At least one file can not be access by OTP, does not exist, is empty or is not a file.")
                    }
                }
            }
        }
    }
}

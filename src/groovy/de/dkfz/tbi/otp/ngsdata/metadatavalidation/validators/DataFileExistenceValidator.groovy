package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext.*

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

        allValueTuples.groupBy { context.directoryStructure.getDataFilePath(context, it)
        }.findAll { it.key != null }.each { File path, List<ValueTuple> valueTuples ->
            if (valueTuples*.cells.sum()*.row.unique().size() != 1) {
                addDirectoryStructureInfo()
                context.addProblem((Set<Cell>)valueTuples*.cells.sum(), Level.WARNING,
                        "Multiple rows reference the same file '${path}'.")
            }
            String message = null
            if (!path.exists()) {
                message = "${pathForMessage(path)} could not be found by OTP."
            } else if (!path.isFile()) {
                message = "${pathForMessage(path)} is not a file."
            } else if (path.length() == 0L) {
                message = "${pathForMessage(path)} is empty."
            }
            if (message) {
                addDirectoryStructureInfo()
                context.addProblem((Set<Cell>)valueTuples*.cells.sum(), Level.ERROR, message)
            }
        }
    }
}

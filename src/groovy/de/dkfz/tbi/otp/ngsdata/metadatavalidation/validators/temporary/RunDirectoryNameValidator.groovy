package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators.temporary

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class RunDirectoryNameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'The metadata file contains entries for exactly one run. (This restriction is planned to be removed in the future.)',
                'The name of the directory containing the metadata file must be the run name. (This restriction is planned to be removed in the future.)',
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [RUN_ID.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        if (valueTuples.size() != 1) {
            context.addProblem(valueTuples*.cells.sum(), Level.ERROR, "The metadata file contains information about more than one run. This is currently not supported.")
        } else {
            String runName = exactlyOneElement(valueTuples).getValue(RUN_ID.name())
            Collection<String> allowedDirectoryNames = [runName, "run${runName}"]
            String actualDirectoryName = context.metadataFile.parentFile.name
            if (!allowedDirectoryNames.any { it == actualDirectoryName }) {
                context.addProblem(exactlyOneElement(valueTuples).cells, Level.ERROR, "The directory containing the metadata file must be named '${allowedDirectoryNames.join("' or '")}', not '${actualDirectoryName}'.")
            } else {
                allowedDirectoryNames.each {
                    if (it != actualDirectoryName && new File(context.metadataFile.parentFile.parentFile, it).exists()) {
                        context.addProblem(exactlyOneElement(valueTuples).cells, Level.ERROR, "Cannot import '${context.metadataFile}' when '${it}' exists in '${context.metadataFile.parentFile.parentFile}'.")
                    }
                }
            }
        }
    }
}

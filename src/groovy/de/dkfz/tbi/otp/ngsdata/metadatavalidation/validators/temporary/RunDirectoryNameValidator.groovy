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
        }
    }
}

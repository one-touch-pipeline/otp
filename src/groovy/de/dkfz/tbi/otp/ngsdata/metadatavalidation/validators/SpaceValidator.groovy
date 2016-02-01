package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class SpaceValidator extends AllCellsValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return []  // Nothing worth mentioning
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if (value.startsWith(' ')) {
            context.addProblem(cells, Level.WARNING, "'${value}' starts with a space character.")
        }
        if (value.endsWith(' ')) {
            context.addProblem(cells, Level.WARNING, "'${value}' ends with a space character.")
        }
        if (value.contains('  ')) {
            context.addProblem(cells, Level.WARNING, "'${value}' contains subsequent space characters.")
        }
    }
}

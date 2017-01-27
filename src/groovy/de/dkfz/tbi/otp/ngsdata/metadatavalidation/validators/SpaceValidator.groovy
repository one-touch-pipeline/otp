package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LaneNumberValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'The lane number should be a single digit in the range of 1 to 8.',
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return LANE_NO.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String laneNumber, Set<Cell> cells) {
        if (laneNumber.empty) {
            context.addProblem(cells, Level.ERROR, "The lane number must not be empty.")
        } else if (!(laneNumber ==~ /^[0-9a-zA-Z]+$/)) {
            context.addProblem(cells, Level.ERROR, "'${laneNumber}' is not a well-formed lane number. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed.")
        } else if (!(laneNumber ==~ /^[1-8]$/)) {
            context.addProblem(cells, Level.WARNING, "'${laneNumber}' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed.")
        }
    }
}

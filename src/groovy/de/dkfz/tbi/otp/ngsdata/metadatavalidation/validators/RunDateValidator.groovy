package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class RunDateValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The run date has the yyyy-MM-dd format.",
                "The run date must not be from the future.",
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return RUN_DATE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String runDate, Set<Cell> cells) {
        try {
            LocalDate date = ISODateTimeFormat.date().parseLocalDate(runDate)
            if (date > LocalDate.now().plusDays(1)) {
                context.addProblem(cells, Level.ERROR, "The run date '${runDate}' must not be from the future.", "No run date may be from the future.")
            }
        } catch (IllegalFieldValueException | IllegalArgumentException e) {
            context.addProblem(cells, Level.ERROR, "The format of the run date '${runDate}' is invalid, it must match yyyy-MM-dd.", "The format of at least one run date is invalid, it must match yyyy-MM-dd.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

@Component
class MateNumberValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The mate number is a positive integer (value >=1).']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.MATE.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String mateNumber, Set<Cell> cells) {
        if (!mateNumber) {
            context.addProblem(cells, Level.ERROR, "The mate number must be provided and must be a positive integer (value >= 1).")
        } else if (!mateNumber.isInteger() || mateNumber.toInteger() < 1) {
            context.addProblem(cells, Level.ERROR, "The mate number ('${mateNumber}') must be a positive integer (value >= 1).","At least one mate number is not a positive integer number.")
        }
    }
}

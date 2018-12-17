package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class CoverageValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {
    @Override
    Collection<String> getDescriptions() {
        return ["Coverage must be a double number"]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.COVERAGE.name()
    }

    @Override
    void columnMissing(BamMetadataValidationContext context) {
        optionalColumnMissing(context, BamMetadataColumn.COVERAGE.name())
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String coverage, Set<Cell> cells) {
        if (!coverage.empty) {
            if (!coverage.isDouble()) {
                context.addProblem(cells, Level.ERROR, "The coverage '${coverage}' should be a double number.", "At least one coverage is not a double number.")
            }
        }
    }
}

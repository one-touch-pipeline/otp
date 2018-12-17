package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class IndividualValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The individual is registered in OTP"]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.INDIVIDUAL.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String individual, Set<Cell> cells) {
        if (!Individual.findByPidOrMockPidOrMockFullName(individual, individual, individual)) {
            context.addProblem(cells, Level.ERROR, "The individual '${individual}' is not registered in OTP.", "At least one individual is not registered in OTP.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.*

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
        if (!(Individual.findByPid(individual) || Individual.findByMockFullName(individual) || Individual.findByMockPid(individual))) {
            context.addProblem(cells, Level.ERROR, "The individual '${individual}' is not registered in OTP.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.*

@Component
class ReferenceGenomeValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The reference genome is registered in OTP"]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.REFERENCE_GENOME.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String refGen, Set<Cell> cells) {
        if (!ReferenceGenome.findByName(refGen)) {
            context.addProblem(cells, Level.ERROR, "The reference genome '${refGen}' is not registered in OTP.")
        }
    }
}

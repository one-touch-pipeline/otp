package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

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
            context.addProblem(cells, Level.ERROR, "The reference genome '${refGen}' is not registered in OTP.", "At least one reference genome is not registered in OTP.")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class SeqTypeBamValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The sequencing type is registered in OTP"]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.SEQUENCING_TYPE.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String seqType, Set<Cell> cells) {
        if (!SeqType.findByName(seqType)) {
            context.addProblem(cells, Level.ERROR, "The sequencing type '${seqType}' is not registered in OTP.", "At least one sequencing type is not registered in OTP.")
        }
    }
}

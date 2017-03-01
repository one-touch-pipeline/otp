package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.*

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
            context.addProblem(cells, Level.ERROR, "The sequencing type '${seqType}' is not registered in OTP.")
        }
    }
}

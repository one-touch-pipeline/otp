package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.*

@Component
class SampleTypeValidator extends SingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The sample type is registered in OTP"]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.SAMPLE_TYPE.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String sampleType, Set<Cell> cells) {
        if (!SampleType.findByName(sampleType)) {
            context.addProblem(cells, Level.ERROR, "The sample type '${sampleType}' is not registered in OTP.", "At least one sample type is not registered in OTP.")
        }
    }
}

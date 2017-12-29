package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

@Component
class InstrumentPlatformValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The instrument platform is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.INSTRUMENT_PLATFORM.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String seqPlatformName, Set<Cell> cells) {
        if (!SeqPlatform.findByName(seqPlatformName)) {
            context.addProblem(cells, Level.ERROR, "Instrument platform '${seqPlatformName}' is not registered in the OTP database.", "At least one instrument platform is not registered in the OTP database.")
        }
    }
}

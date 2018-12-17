package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import org.springframework.beans.factory.annotation.*

@Component
class InstrumentModelValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqPlatformModelLabelService seqPlatformModelLabelService

    @Override
    Collection<String> getDescriptions() {
        return ['The instrument model is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.INSTRUMENT_MODEL.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String seqPlatformModelLabelNameOrAlias, Set<Cell> cells) {
        if (!seqPlatformModelLabelService.findByNameOrImportAlias(seqPlatformModelLabelNameOrAlias)) {
            context.addProblem(cells, Level.ERROR, "Instrument model '${seqPlatformModelLabelNameOrAlias}' is not registered in the OTP database.", "At least one instrument model is not registered in the OTP database.")
        }
    }
}

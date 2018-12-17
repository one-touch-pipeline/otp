package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabelService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

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
        if (!seqPlatformModelLabelNameOrAlias) {
            context.addProblem(cells, Level.ERROR, "Instrument model must not be empty.", "At least one Instrument model is empty.")
        } else if (!seqPlatformModelLabelService.findByNameOrImportAlias(seqPlatformModelLabelNameOrAlias)) {
            context.addProblem(cells, Level.ERROR, "Instrument model '${seqPlatformModelLabelNameOrAlias}' is not registered in the OTP database.", "At least one instrument model is not registered in the OTP database.")
        }
    }
}

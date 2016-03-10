package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component

@Component
class SeqTypeValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing type is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.SEQUENCING_TYPE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String seqTypeName, Set<Cell> cells) {
        if (!SeqType.findByName(seqTypeName)) {
            context.addProblem(cells, Level.ERROR, "Sequencing type '${seqTypeName}' is not registered in the OTP database.")
        }
    }
}

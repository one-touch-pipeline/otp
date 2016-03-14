package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SequencingKitLabelService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator
import org.springframework.stereotype.Component

@Component
class SequencingKitValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing kit, if provided, is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.SEQUENCING_KIT.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
        optionalColumnMissing(context, MetaDataColumn.SEQUENCING_KIT.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String sequencingKitLabelNameOrAlias, Set<Cell> cells) {
        if (!sequencingKitLabelNameOrAlias.empty && !SequencingKitLabelService.findSequencingKitLabelByNameOrAlias(sequencingKitLabelNameOrAlias)) {
            context.addProblem(cells, Level.ERROR, "Sequencing kit '${sequencingKitLabelNameOrAlias}' is not registered in the OTP database.")
        }
    }
}

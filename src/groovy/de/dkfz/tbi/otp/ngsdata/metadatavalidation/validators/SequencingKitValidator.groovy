package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SequencingKitLabelService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class SequencingKitValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SequencingKitLabelService sequencingKitLabelService

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
        if (!sequencingKitLabelNameOrAlias.empty && !sequencingKitLabelService.findByNameOrImportAlias(sequencingKitLabelNameOrAlias)) {
            context.addProblem(cells, Level.ERROR, "Sequencing kit '${sequencingKitLabelNameOrAlias}' is not registered in the OTP database.","At least one sequencing kit is not registered in the OTP database.")
        }
    }
}

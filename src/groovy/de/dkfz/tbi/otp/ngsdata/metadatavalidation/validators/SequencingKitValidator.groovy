package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

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

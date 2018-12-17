package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class SeqCenterValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing center is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return CENTER_NAME.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String centerName, Set<Cell> cells) {
        if (!atMostOneElement(SeqCenter.findAllByName(centerName))) {
            context.addProblem(cells, Level.ERROR, "Sequencing center '${centerName}' is not registered in the OTP database.", "At least one sequencing center is not registered in the OTP database.")
        }
    }
}

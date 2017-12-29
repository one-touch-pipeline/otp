package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

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

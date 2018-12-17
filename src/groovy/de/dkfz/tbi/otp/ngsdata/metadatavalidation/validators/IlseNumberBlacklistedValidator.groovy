package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.IlseSubmission
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ILSE_NO

@Component
class IlseNumberBlacklistedValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The ILSe number is not blacklisted"]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return ILSE_NO.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if (value && value.isInteger()) {
            IlseSubmission ilseSubmission = CollectionUtils.atMostOneElement(IlseSubmission.findAllByIlseNumberAndWarning(value.toInteger(), true))
            if (ilseSubmission) {
                context.addProblem(cells, Level.WARNING, "The ilse ${value} is blacklisted:\n${ilseSubmission.comment.displayString()}.", "At least one ilse number is blacklisted.")
            }
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
                context.addProblem(cells, Level.WARNING, "The ilse ${value} is blacklisted:\n${ilseSubmission.comment.displayString()}.")
            }
        }
    }
}

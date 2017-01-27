package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class Md5sumFormatValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The format of the MD5 sum is valid.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MD5.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if (!(value ==~ /^[0-9a-fA-F]{32}$/)) {
            context.addProblem(cells, Level.ERROR, "Not a well-formatted MD5 sum: '${value}'")
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*

import de.dkfz.tbi.otp.ngsdata.*

@Component
class Md5sumFormatValidator extends SingleValueValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The format of the MD5 sum is valid.']
    }

    @Override
    String getColumnTitle(AbstractMetadataValidationContext context) {
        return MetaDataColumn.MD5.name()
    }

    @Override
    void columnMissing(AbstractMetadataValidationContext context) {
        if (context instanceof BamMetadataValidationContext) {
            optionalColumnMissing(context, BamMetadataColumn.MD5.name())
        }
    }

    @Override
    void validateValue(AbstractMetadataValidationContext context, String value, Set<Cell> cells) {
        if (context instanceof BamMetadataValidationContext) {
            if (!value.empty) {
                checkMd5Sum(context, value, cells)
            }
        } else {
            checkMd5Sum(context, value, cells)
        }

    }

    static void checkMd5Sum(AbstractMetadataValidationContext context, String value, Set<Cell> cells) {
        if (!(value ==~ /^[0-9a-fA-F]{32}$/)) {
            context.addProblem(cells, Level.ERROR, "Not a well-formatted MD5 sum: '${value}'.", "At least one md5sum is not well formatted.")
        }
    }
}

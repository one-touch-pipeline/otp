package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

@Component
class Md5sumUniqueValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'Each MD5 sum is unique.'
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [MetaDataColumn.MD5.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.groupBy {
            it.getValue(MetaDataColumn.MD5.name()).toLowerCase(Locale.ENGLISH)
        }.each { String md5sum, Collection<ValueTuple> valueTuplesOfMd5sum ->
            if (valueTuplesOfMd5sum*.cells.sum().size() > 1) {
                context.addProblem(valueTuplesOfMd5sum*.cells.sum(), Level.WARNING, "The MD5 sum '${md5sum}' is not unique in the metadata file.")
            }
            if (DataFile.findByMd5sum(md5sum)) {
                context.addProblem(valueTuplesOfMd5sum*.cells.sum(), Level.WARNING, "A fastq file with the MD5 sum '${md5sum}' is already registered in OTP.")
            }
        }
    }
}

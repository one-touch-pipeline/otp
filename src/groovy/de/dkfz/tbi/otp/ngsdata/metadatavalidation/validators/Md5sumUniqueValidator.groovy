package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class Md5sumUniqueValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'Each MD5 sum is unique.',
        ]
    }

    @Override
    List<String> getColumnTitles(AbstractMetadataValidationContext context) {
        return [MetaDataColumn.MD5.name()]
    }

    @Override
    boolean columnMissing(AbstractMetadataValidationContext context, String columnTitle) {
        if (columnTitle == MetaDataColumn.MD5.name()) {
            if (context instanceof BamMetadataValidationContext) {
                optionalColumnMissing(context, columnTitle)
            } else {
                mandatoryColumnMissing(context, columnTitle)
            }
            return false
        }
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.groupBy {
            it.getValue(MetaDataColumn.MD5.name()).toLowerCase(Locale.ENGLISH)
        }.each { String md5sum, Collection<ValueTuple> valueTuplesOfMd5sum ->
            if (valueTuplesOfMd5sum*.cells.sum().size() > 1) {
                context.addProblem(valueTuplesOfMd5sum*.cells.sum(), Level.WARNING, "The MD5 sum '${md5sum}' is not unique in the metadata file.", "At least one MD5 sum is not unique in the metadata file.")
            }
            if (context instanceof BamMetadataValidationContext) {
                if (ExternallyProcessedMergedBamFile.findByMd5sum(md5sum)) {
                    context.addProblem(valueTuplesOfMd5sum*.cells.sum(), Level.WARNING, "A bam file with the MD5 sum '${md5sum}' is already registered in OTP.", "At least one bam file has a MD5 sum is already registered in OTP.")
                }
            } else {
                if (DataFile.findByMd5sum(md5sum)) {
                    context.addProblem(valueTuplesOfMd5sum*.cells.sum(), Level.WARNING, "A fastq file with the MD5 sum '${md5sum}' is already registered in OTP.", "At least one fastq file has a MD5 sum which is already registered in OTP.")
                }
            }
        }
    }
}

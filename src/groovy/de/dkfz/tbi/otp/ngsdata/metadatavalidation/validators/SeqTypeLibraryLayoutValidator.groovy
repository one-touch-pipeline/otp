package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqTypeLibraryLayoutValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sequencing type and library layout is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(AbstractMetadataValidationContext context) {
        List<String> columns = [SEQUENCING_TYPE.name(), LIBRARY_LAYOUT.name()]
        if (context instanceof BamMetadataValidationContext) {
            return columns
        } else {
            columns.add(TAGMENTATION_BASED_LIBRARY.name())
            return columns
        }
    }

    @Override
    boolean columnMissing(AbstractMetadataValidationContext context, String columnTitle) {
        if (columnTitle != TAGMENTATION_BASED_LIBRARY.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String seqTypeName
            if (context instanceof BamMetadataValidationContext) {
                seqTypeName = it.getValue(SEQUENCING_TYPE.name())
            } else {
                seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(it)
            }
            String libraryLayoutName = it.getValue(LIBRARY_LAYOUT.name())
            if (!SeqType.findByNameAndLibraryLayout(seqTypeName, libraryLayoutName)) {
                context.addProblem(it.cells, Level.ERROR, "The combination of sequencing type '${seqTypeName}' and library layout '${libraryLayoutName}' is not registered in the OTP database.")
            }
        }
    }
}

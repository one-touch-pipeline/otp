package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqTypeLibraryLayoutValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sequencing type and library layout is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIBRARY_LAYOUT.name(), TAGMENTATION_BASED_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle != TAGMENTATION_BASED_LIBRARY.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(it)
            String libraryLayoutName = it.getValue(MetaDataColumn.LIBRARY_LAYOUT.name())
            if (!SeqType.findByNameAndLibraryLayout(seqTypeName, libraryLayoutName)) {
                context.addProblem(it.cells, Level.ERROR, "The combination of sequencing type '${seqTypeName}' and library layout '${libraryLayoutName}' is not registered in the OTP database.")
            }
        }
    }
}

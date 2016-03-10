package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

@Component
class SeqTypeLibraryLayoutValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sequencing type and library layout is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [MetaDataColumn.SEQUENCING_TYPE.name(),
                MetaDataColumn.LIBRARY_LAYOUT.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String seqTypeName = it.getValue(MetaDataColumn.SEQUENCING_TYPE.name())
            String libraryLayoutName = it.getValue(MetaDataColumn.LIBRARY_LAYOUT.name())
            if (!SeqType.findByNameAndLibraryLayout(seqTypeName, libraryLayoutName)) {
                context.addProblem(it.cells, Level.ERROR, "The combination of sequencing type '${seqTypeName}' and library layout '${libraryLayoutName}' is not registered in the OTP database.")
            }
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LIB_PREP_KIT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE

@Component
class LibPrepKitSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator{

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}', the library preparation kit is not empty."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIB_PREP_KIT.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            if((valueTuple.getValue(SEQUENCING_TYPE.name()) == SeqTypeNames.EXOME.seqTypeName) &&
                    (valueTuple.getValue(LIB_PREP_KIT.name()) == "")) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}', the library preparation kit must not be empty.")
            }
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

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
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_TYPE.name()) {
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            if ((valueTuple.getValue(SEQUENCING_TYPE.name()) == SeqTypeNames.EXOME.seqTypeName) &&
                    (!valueTuple.getValue(LIB_PREP_KIT.name()))) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}', the library preparation kit must be given.")
            }
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*

@Component
class LibPrepKitSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator{

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit is not empty."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), LIB_PREP_KIT.name(), TAGMENTATION_BASED_LIBRARY.name()]
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
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            if ((seqType in [SeqTypeNames.EXOME, SeqTypeNames.RNA]*.seqTypeName) &&
                    (!valueTuple.getValue(LIB_PREP_KIT.name()))) {
                context.addProblem(valueTuple.cells, Level.ERROR, "If the sequencing type is '${SeqTypeNames.EXOME.seqTypeName}' or '${SeqTypeNames.RNA.seqTypeName}', the library preparation kit must be given.")
            }
        }
    }
}

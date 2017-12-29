package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibrarySeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}', the library should be given."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [CUSTOMER_LIBRARY.name(), SEQUENCING_TYPE.name(), TAGMENTATION_BASED_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            if (seqType == SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName && !valueTuple.getValue(CUSTOMER_LIBRARY.name())) {
                context.addProblem(valueTuple.cells, Level.WARNING, "For sequencing type '${seqType}' there should be a value in the ${CUSTOMER_LIBRARY} column.", "For specific sequencing types there should be a value in the ${CUSTOMER_LIBRARY} column.")
            }
        }
    }
}

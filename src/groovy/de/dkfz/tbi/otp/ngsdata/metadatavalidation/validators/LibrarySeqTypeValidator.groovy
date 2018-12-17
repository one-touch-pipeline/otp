package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LibrarySeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    static final String TAGMENTATION_WITHOUT_LIBRARY = "For tagmentation sequencing types there should be a value in the ${CUSTOMER_LIBRARY} column."
    static final String LIBRARY_WITHOUT_TAGMENTATION = "At least one library is given in ${CUSTOMER_LIBRARY} for a non tagmentation seqtype"

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
            String library = valueTuple.getValue(CUSTOMER_LIBRARY.name())
            if (seqType.endsWith(SeqType.TAGMENTATION_SUFFIX)) {
                if (!library) {
                    context.addProblem(valueTuple.cells, Level.WARNING,
                            "For the tagmentation sequencing type '${seqType}' there should be a value in the ${CUSTOMER_LIBRARY} column.",
                            TAGMENTATION_WITHOUT_LIBRARY)
                }
            } else {
                if (library) {
                    context.addProblem(valueTuple.cells, Level.WARNING,
                            "The library '${library}' in column ${CUSTOMER_LIBRARY} indicates tagmentation, " +
                                    "but the seqtype '${seqType}' is without tagmentation",
                            LIBRARY_WITHOUT_TAGMENTATION)
                }
            }
        }
    }
}

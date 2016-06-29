package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*


@Component
class AdapterFileSampleValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    AdapterFileService adapterFileService

    final static String SAMPLE = "Sample"

    @Override
    Collection<String> getDescriptions() {
        return [
                "If the sequencing type is one of '${SeqType.WGBS_SEQ_TYPE_NAMES*.seqTypeName.join("', '")}' and " +
                        "the ${SAMPLE_SUBMISSION_TYPE} is not '${SAMPLE}', " +
                        "there should be an entry in the ${ADAPTER_FILE} column.",
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [
                SEQUENCING_TYPE.name(),
                SAMPLE_SUBMISSION_TYPE.name(),
                ADAPTER_FILE.name(),
                TAGMENTATION_BASED_LIBRARY.name(),
        ]
    }

    @Override
    boolean columnsMissing(MetadataValidationContext context, Collection<String> columnTitles) {
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple tuple ->
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(tuple)
            if (SeqType.WGBS_SEQ_TYPE_NAMES*.seqTypeName.contains(seqType) &&
                    tuple.getValue(SAMPLE_SUBMISSION_TYPE.name()) != SAMPLE &&
                    !tuple.getValue(ADAPTER_FILE.name())
            ) {
                context.addProblem(tuple.cells, Level.WARNING, "There should be an entry in the ${ADAPTER_FILE} column " +
                        "for sequencing type '${seqType}'.")
            }
        }
    }
}

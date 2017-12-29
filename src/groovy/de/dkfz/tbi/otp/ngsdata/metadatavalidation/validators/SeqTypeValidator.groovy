package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing type is registered in the OTP database.']
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE.name(), TAGMENTATION_BASED_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_TYPE.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            if (!SeqTypeService.findSeqTypeByNameOrAlias(seqType)) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Sequencing type '${seqType}' is not registered in the OTP database.", "At least one sequencing type is not registered in OTP.")
            }
        }
    }
}

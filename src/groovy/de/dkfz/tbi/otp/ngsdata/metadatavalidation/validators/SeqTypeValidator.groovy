package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetadataImportService
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_BASED_LIBRARY

@Component
class SeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

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
            if (!seqType) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Sequencing type must not be empty.", "At least one sequencing type is empty.")
            }
            else if (!seqTypeService.findByNameOrImportAlias(seqType)) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Sequencing type '${seqType}' is not registered in the OTP database.", "At least one sequencing type is not registered in OTP.")
            }
        }
    }
}

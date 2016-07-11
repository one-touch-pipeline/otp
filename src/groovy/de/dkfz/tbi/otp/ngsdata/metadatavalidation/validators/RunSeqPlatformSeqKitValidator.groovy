package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class RunSeqPlatformSeqKitValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The dependency between the identifier in the ${RUN_ID} name and the combination of ${INSTRUMENT_PLATFORM} and ${SEQUENCING_KIT} for runs from the DKFZ sequencing center must be valid.",
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [RUN_ID.name(),
                INSTRUMENT_PLATFORM.name(),
                INSTRUMENT_MODEL.name(),
                SEQUENCING_KIT.name(),
                CENTER_NAME.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_KIT.name()) {
            optionalColumnMissing(context, columnTitle)
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            SeqPlatform seqPlatform = SeqPlatformService.findSeqPlatform(valueTuple.getValue(INSTRUMENT_PLATFORM.name()), valueTuple.getValue(INSTRUMENT_MODEL.name()), valueTuple.getValue(SEQUENCING_KIT.name()) ?: null)
            String runName = valueTuple.getValue(RUN_ID.name())
            if (valueTuple.getValue(CENTER_NAME.name()) == "DKFZ" && seqPlatform?.identifierInRunName && !runName.contains(seqPlatform.identifierInRunName)) {
                context.addProblem(valueTuple.cells, Level.WARNING, "The run name ${runName} does not contain the sequencing kit and sequencing platform specific run identifier ${seqPlatform.identifierInRunName}.")

            }
        }
    }
}

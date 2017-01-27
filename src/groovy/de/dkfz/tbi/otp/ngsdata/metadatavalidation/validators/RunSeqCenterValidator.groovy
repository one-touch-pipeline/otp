package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class RunSeqCenterValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "All entries for a run have the same value in the column '${CENTER_NAME}'.",
                'If a run is already registered in the OTP database, the sequencing center matches the one in the database.',
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [RUN_ID.name(), CENTER_NAME.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.groupBy { it.getValue(RUN_ID.name()) }.each { String runName, Collection<ValueTuple> valueTuplesOfRun ->
            if (valueTuplesOfRun.size() == 1) {
                ValueTuple valueTuple = CollectionUtils.exactlyOneElement(valueTuplesOfRun)
                Run run = CollectionUtils.atMostOneElement(Run.findAllByName(runName))
                String seqCenterName = valueTuple.getValue(CENTER_NAME.name())
                if (run && run.seqCenter.name != seqCenterName) {
                    context.addProblem(valueTuple.cells, Level.ERROR, "Run '${runName}' is already registered in the OTP database with sequencing center '${run.seqCenter.name}', not with '${seqCenterName}'.")
                }
            } else {
                context.addProblem(valueTuplesOfRun*.cells.sum(), Level.ERROR, "All entries for run '${runName}' must have the same value in the column '${CENTER_NAME}'.")
            }
        }
    }
}

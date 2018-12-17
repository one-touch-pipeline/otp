package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.RunDateParserService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_DATE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID

@Component
class RunRunDateValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    RunDateParserService runDateParserService

    @Override
    Collection<String> getDescriptions() {
        return [
                "All entries for a run have the same value in the column '${RUN_DATE}'.",
                'If a run is already registered in the OTP database, the run date matches the one in the database.',
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [RUN_ID.name(), RUN_DATE.name()]
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.groupBy { it.getValue(RUN_ID.name()) }.each { String runName, Collection<ValueTuple> valueTuplesOfRun ->
            if (valueTuplesOfRun.size() == 1) {
                ValueTuple valueTuple = CollectionUtils.exactlyOneElement(valueTuplesOfRun)
                Run run = CollectionUtils.atMostOneElement(Run.findAllByName(runName))
                String runDate = valueTuple.getValue(RUN_DATE.name())
                Date runDateFromRunName = runDateParserService.parseDateFromRunName(runName)
                if (runDateFromRunName && runDateFromRunName.format("yyyy-MM-dd") != runDate) {
                    context.addProblem(valueTuple.cells, Level.WARNING, "Run date parsed from run name '${runName}' is not the same as '${runDate}'. OTP will use the run date from the '${RUN_DATE}' column.", "At least one run date parsed from run name is not the same as in the '${RUN_DATE}' column.")
                }
                if (run && run.dateExecuted.format("yyyy-MM-dd") != runDate) {
                    context.addProblem(valueTuple.cells, Level.ERROR, "Run '${runName}' is already registered in the OTP database with run date '${run.dateExecuted.format("yyyy-MM-dd")}', not with '${runDate}'.", "At least one run is already registered in the OTP database but with another date.")
                }
            } else {
                context.addProblem(valueTuplesOfRun*.cells.sum(), Level.ERROR, "All entries for run '${runName}' must have the same value in the column '${RUN_DATE}'.", "All entries of one run must have the same value in the column '${RUN_DATE}'.")
            }
        }
    }
}

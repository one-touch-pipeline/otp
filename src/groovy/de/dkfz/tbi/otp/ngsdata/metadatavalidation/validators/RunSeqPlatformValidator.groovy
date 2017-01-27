package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class RunSeqPlatformValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "All entries for a run have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'",
                "If a run is already registered in the OTP database, the combination of instrument platform, instrument model and sequencing kit matches the one in the database.",
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [RUN_ID.name(),
                INSTRUMENT_PLATFORM.name(),
                INSTRUMENT_MODEL.name(),
                SEQUENCING_KIT.name()]
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
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.groupBy { it.getValue(RUN_ID.name()) }.each { String runName, List<ValueTuple> valueTuplesOfRun ->
            if (valueTuplesOfRun.size() == 1) {
                ValueTuple valueTuple = CollectionUtils.exactlyOneElement(valueTuplesOfRun)
                Run run = CollectionUtils.atMostOneElement(Run.findAllByName(runName))
                SeqPlatform seqPlatform = SeqPlatformService.findSeqPlatform(valueTuple.getValue(INSTRUMENT_PLATFORM.name()), valueTuple.getValue(INSTRUMENT_MODEL.name()), valueTuple.getValue(SEQUENCING_KIT.name()) ?: null)
                if (run && seqPlatform && run.seqPlatform.id != seqPlatform.id) {
                    context.addProblem(valueTuple.cells, Level.ERROR, "Run '${runName}' is already registered in the OTP database with sequencing platform '${run.seqPlatform.fullName()}', not with '${seqPlatform.fullName()}'.")
                }
            } else {
                context.addProblem((Set)valueTuplesOfRun*.cells.sum(), Level.ERROR, "All entries for run '${runName}' must have the same combination of values in the columns '${INSTRUMENT_PLATFORM}', '${INSTRUMENT_MODEL}' and '${SEQUENCING_KIT}'.")
            }
        }
    }
}

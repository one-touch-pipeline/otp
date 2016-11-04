package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

import java.util.regex.*

abstract class AbstractExecutePanCanJob<R extends RoddyResult> extends AbstractRoddyJob<R> {

    public static final Pattern READ_GROUP_PATTERN = Pattern.compile(/^@RG\s+ID:([^\s]+)\s/)

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService

    @Autowired
    ExecutionService executionService


    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(R roddyResult, Realm realm) throws Throwable {
        assert roddyResult: "roddyResult must not be null"
        assert realm: "realm must not be null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyResult)
        String nameInConfigFile = roddyResult.config.getNameUsedInConfig()

        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyResult.config.configFilePath))

        return executeRoddyCommandService.defaultRoddyExecutionCommand(roddyResult, nameInConfigFile, analysisIDinConfigFile, realm) +
                prepareAndReturnWorkflowSpecificParameter(roddyResult) +
                prepareAndReturnCValues(roddyResult)
    }


    public String prepareAndReturnCValues(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"

        List<String> cValues = prepareAndReturnWorkflowSpecificCValues(roddyResult)
        if (roddyResult.processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) {
            cValues.add("PBS_AccountName:FASTTRACK")
        }

        return "--cvalues=\"${cValues.join(',')}\""
    }


    public String getChromosomeIndexParameter(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        return "CHROMOSOME_INDICES:( ${chromosomeNames.join(' ')} )"
    }


    protected abstract List<String> prepareAndReturnWorkflowSpecificCValues(R roddyResult)

    protected abstract String prepareAndReturnWorkflowSpecificParameter(R roddyResult)
}

package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

abstract class AbstractExecutePanCanJob<R extends RoddyResult> extends AbstractRoddyJob<R> {

    @Autowired
    ReferenceGenomeService referenceGenomeService

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    BedFileService bedFileService

    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    ChromosomeIdentifierSortingService chromosomeIdentifierSortingService


    @Override
    protected String prepareAndReturnWorkflowSpecificCommand(R roddyResult, Realm realm) throws Throwable {
        assert roddyResult: "roddyResult must not be null"
        assert realm: "realm must not be null"

        String analysisIDinConfigFile = executeRoddyCommandService.getAnalysisIDinConfigFile(roddyResult)
        String nameInConfigFile = roddyResult.config.getNameUsedInConfig()

        LsdfFilesService.ensureFileIsReadableAndNotEmpty(new File(roddyResult.config.configFilePath))

        return [
                executeRoddyCommandService.defaultRoddyExecutionCommand(roddyResult, nameInConfigFile, analysisIDinConfigFile, realm),
                prepareAndReturnAdditionalImports(roddyResult),
                prepareAndReturnWorkflowSpecificParameter(roddyResult),
                prepareAndReturnCValues(roddyResult),
        ].join(" ")
    }

    public String prepareAndReturnAdditionalImports(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"

        String fasttrack = (roddyResult.processingPriority == ProcessingPriority.FAST_TRACK_PRIORITY) ?
                "-fasttrack"
                : ""
        String pluginVersion = roddyResult.config.pluginVersion
        String seqType = roddyResult.seqType.roddyName.toLowerCase()
        return "--additionalImports=${pluginVersion}-${seqType}${fasttrack}"
    }


    public String prepareAndReturnCValues(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"
        List<String> cValues = prepareAndReturnWorkflowSpecificCValues(roddyResult)
        return "--cvalues=\"${cValues.join(',').replace('$', '\\$')}\""
    }


    public String getChromosomeIndexParameterWithMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }


    public String getChromosomeIndexParameterWithoutMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }




    protected abstract List<String> prepareAndReturnWorkflowSpecificCValues(R roddyResult)

    protected abstract String prepareAndReturnWorkflowSpecificParameter(R roddyResult)
}

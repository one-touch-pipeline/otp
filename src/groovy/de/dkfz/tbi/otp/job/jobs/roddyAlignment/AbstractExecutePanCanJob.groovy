package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.ChromosomeIdentifierSortingService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService

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

    String prepareAndReturnAdditionalImports(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"

        String fasttrack = (roddyResult.processingPriority >= ProcessingPriority.FAST_TRACK.priority) ?
                "-fasttrack"
                : ""
        String pluginVersion = roddyResult.config.pluginVersion
        String seqType = roddyResult.seqType.roddyName.toLowerCase()
        return "--additionalImports=${pluginVersion}-${seqType}${fasttrack}"
    }


    String prepareAndReturnCValues(R roddyResult) {
        assert roddyResult: "roddyResult must not be null"
        List<String> cValues = prepareAndReturnWorkflowSpecificCValues(roddyResult)
        return "--cvalues=\"${cValues.join(',').replace('$', '\\$')}\""
    }


    String getChromosomeIndexParameterWithMitochondrium(ReferenceGenome referenceGenome) {
        assert referenceGenome

        List<String> chromosomeNames = ReferenceGenomeEntry.findAllByReferenceGenomeAndClassificationInList(referenceGenome,
                [ReferenceGenomeEntry.Classification.CHROMOSOME, ReferenceGenomeEntry.Classification.MITOCHONDRIAL])*.name
        assert chromosomeNames: "No chromosome names could be found for reference genome ${referenceGenome}"

        List<String> sortedList = chromosomeIdentifierSortingService.sortIdentifiers(chromosomeNames)

        return "CHROMOSOME_INDICES:( ${sortedList.join(' ')} )"
    }


    String getChromosomeIndexParameterWithoutMitochondrium(ReferenceGenome referenceGenome) {
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

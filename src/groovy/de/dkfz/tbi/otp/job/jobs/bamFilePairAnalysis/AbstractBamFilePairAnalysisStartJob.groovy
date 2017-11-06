package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.beans.factory.annotation.*
import org.springframework.scheduling.annotation.*

abstract class AbstractBamFilePairAnalysisStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionService executionService

    @Scheduled(fixedDelay = 60000l)
    void execute() {
        doWithPersistenceInterceptor {
            short minPriority = minimumProcessingPriorityForOccupyingASlot
            if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
                return
            }
            SamplePair.withTransaction {
                SamplePair samplePair = findSamplePairToProcess(minPriority)
                if (samplePair) {

                    ConfigPerProject config = getConfig(samplePair)

                    AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                    AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                    BamFilePairAnalysis analysis = getInstanceClass().newInstance(
                            samplePair: samplePair,
                            instanceName: getInstanceName(config),
                            config: config,
                            sampleType1BamFile: sampleType1BamFile,
                            sampleType2BamFile: sampleType2BamFile,
                            latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
                    )
                    analysis.save(flush: true)
                    prepareCreatingTheProcessAndTriggerTracking(analysis)
                    createProcess(analysis)
                    log.debug "Analysis started for: ${analysis.toString()}"
                }
            }
        }
    }


    @Override
    Process restart(Process process) {
        assert process

        BamFilePairAnalysis failedAnalysis = process.getProcessParameterObject()

        withdrawSnvJobResultsIfAvailable(failedAnalysis)

        tryToDeleteResultFilesOfFailedInstance(failedAnalysis)

        BamFilePairAnalysis.withTransaction {

            failedAnalysis.withdrawn = true
            assert failedAnalysis.save(flush: true)

            BamFilePairAnalysis newAnalysis = getInstanceClass().newInstance(
                    samplePair: failedAnalysis.samplePair,
                    instanceName: getInstanceName(failedAnalysis.config),
                    config: failedAnalysis.config,
                    sampleType1BamFile: failedAnalysis.sampleType1BamFile,
                    sampleType2BamFile: failedAnalysis.sampleType2BamFile,
                    latestDataFileCreationDate: failedAnalysis.latestDataFileCreationDate,
            )
            assert newAnalysis.save(flush: true)

            return createProcess(newAnalysis)
        }
    }


    private void tryToDeleteResultFilesOfFailedInstance(BamFilePairAnalysis analysis) {
        final Realm realm = configService.getRealmDataManagement(analysis.project)

        String deleteFiles = "rm -rf ${analysis.getWorkDirectory()} ${analysis.instancePath.absoluteStagingPath}"

        executionService.executeCommandReturnProcessOutput(realm, deleteFiles)
    }

    protected void withdrawSnvJobResultsIfAvailable(BamFilePairAnalysis bamFilePairAnalysis) {}


    protected String getInstanceName(ConfigPerProject config) {
        String date = DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").withZone(ConfigService.getDateTimeZone()).print(Instant.now())
        return "results_${config.pluginVersion.replaceAll(":", "-")}_${config.configVersion}_${date}"
    }


    protected abstract ConfigPerProject getConfig(SamplePair samplePair)
    protected abstract Class<? extends ConfigPerProject> getConfigClass()
    protected abstract Class<? extends BamFilePairAnalysis> getInstanceClass()
    protected abstract SamplePair findSamplePairToProcess(short minPriority)
    protected abstract void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis)


}

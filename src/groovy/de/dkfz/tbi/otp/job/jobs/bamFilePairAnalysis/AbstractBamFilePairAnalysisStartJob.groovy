package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.springframework.beans.factory.annotation.*
import org.springframework.scheduling.annotation.*

import java.time.*
import java.time.format.*

abstract class AbstractBamFilePairAnalysisStartJob extends AbstractStartJobImpl implements RestartableStartJob, BamFilePairAnalysisStartJobTrait {

    @Autowired
    RemoteShellHelper remoteShellHelper

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

                    ConfigPerProjectAndSeqType config = getConfig(samplePair)

                    AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                    AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                    BamFilePairAnalysis analysis = getBamFileAnalysisService().getAnalysisClass().newInstance(
                            samplePair: samplePair,
                            instanceName: getInstanceName(config),
                            config: config,
                            sampleType1BamFile: sampleType1BamFile,
                            sampleType2BamFile: sampleType2BamFile,
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

        tryToDeleteResultFilesOfFailedInstance(failedAnalysis)

        BamFilePairAnalysis.withTransaction {

            failedAnalysis.withdrawn = true
            assert failedAnalysis.save(flush: true)

            BamFilePairAnalysis newAnalysis = getBamFileAnalysisService().getAnalysisClass().newInstance(
                    samplePair: failedAnalysis.samplePair,
                    instanceName: getInstanceName(failedAnalysis.config),
                    config: failedAnalysis.config,
                    sampleType1BamFile: failedAnalysis.sampleType1BamFile,
                    sampleType2BamFile: failedAnalysis.sampleType2BamFile,
            )
            assert newAnalysis.save(flush: true)

            return createProcess(newAnalysis)
        }
    }


    private void tryToDeleteResultFilesOfFailedInstance(BamFilePairAnalysis analysis) {
        final Realm realm = analysis.project.realm

        String deleteFiles = "rm -rf ${analysis.getWorkDirectory()}"

        remoteShellHelper.executeCommandReturnProcessOutput(realm, deleteFiles)
    }

    SamplePair findSamplePairToProcess(short minPriority) {
        return getBamFileAnalysisService().samplePairForProcessing(minPriority)
    }

    String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm")
        ZonedDateTime time = ZonedDateTime.of(LocalDateTime.now(), ConfigService.getTimeZoneId())
        return time.format(formatter).replaceAll('/', '_')
    }
}

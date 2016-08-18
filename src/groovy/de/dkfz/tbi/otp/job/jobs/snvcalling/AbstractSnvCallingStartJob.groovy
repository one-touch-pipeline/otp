package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.tracking.*
import org.springframework.beans.factory.annotation.*
import org.springframework.scheduling.annotation.*

abstract class AbstractSnvCallingStartJob extends AbstractStartJobImpl {

    @Autowired
    SnvCallingService snvCallingService

    @Scheduled(fixedDelay = 60000l)
    void execute() {

        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }
        SnvConfig.withTransaction {
            SamplePair samplePair = snvCallingService.samplePairForSnvProcessing(minPriority, getConfigClass())
            if (samplePair) {

                ConfigPerProject config = getConfig(samplePair)

                AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                        samplePair: samplePair,
                        instanceName: getInstanceName(config),
                        config: config,
                        sampleType1BamFile: sampleType1BamFile,
                        sampleType2BamFile: sampleType2BamFile,
                        latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
                )
                snvCallingInstance.save(flush: true)
                trackingService.setStartedForSeqTracks(snvCallingInstance.getContainedSeqTracks(), OtrsTicket.ProcessingStep.SNV)
                samplePair.processingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                samplePair.save()
                createProcess(snvCallingInstance)
                log.debug "SnvCallingStartJob started for: ${snvCallingInstance.toString()}"
            }
        }
    }

    protected abstract ConfigPerProject getConfig(SamplePair samplePair)
    protected abstract String getInstanceName(ConfigPerProject config)
    protected abstract Class<? extends ConfigPerProject> getConfigClass()
}

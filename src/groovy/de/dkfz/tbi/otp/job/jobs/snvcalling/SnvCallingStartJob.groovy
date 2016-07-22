package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.tracking.*
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component("snvStartJob")
@Scope("singleton")
class SnvCallingStartJob extends AbstractStartJobImpl {

    @Autowired
    SnvCallingService snvCallingService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 60000l)
    void execute() {

        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }

        SnvConfig.withTransaction {
            SamplePair samplePair = snvCallingService.samplePairForSnvProcessing(minPriority)
            if (samplePair) {
                SnvConfig config = SnvConfig.getLatest(
                        samplePair.project,
                        samplePair.seqType
                        )

                AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                        samplePair: samplePair,
                        instanceName: DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm").withZone(DateTimeZone.getDefault()).print(Instant.now()),
                        config: config,
                        sampleType1BamFile: sampleType1BamFile,
                        sampleType2BamFile: sampleType2BamFile,
                        latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
                        )
                snvCallingInstance.save(flush: true)
                trackingService.setStartedForSeqTracks(snvCallingInstance.getContainedSeqTracks(), OtrsTicket.ProcessingStep.SNV)
                samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
                samplePair.save()

                createProcess(snvCallingInstance)
                log.debug "SnvCallingStartJob started for: ${snvCallingInstance.toString()}"
            }
        }
    }


    @Override
    String getJobExecutionPlanName() {
        return "SnvWorkflow"
    }
}

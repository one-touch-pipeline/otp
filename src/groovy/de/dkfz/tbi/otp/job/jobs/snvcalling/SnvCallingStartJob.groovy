package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.tracking.*
import org.joda.time.*
import org.joda.time.format.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

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
                        instanceName: DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").withZone(ConfigService.getDateTimeZone()).print(Instant.now()),
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

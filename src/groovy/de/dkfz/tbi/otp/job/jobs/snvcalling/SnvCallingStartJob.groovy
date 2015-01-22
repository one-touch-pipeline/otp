package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair.ProcessingStatus
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter

@Component("snvStartJob")
@Scope("singleton")
class SnvCallingStartJob extends AbstractStartJobImpl {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    SnvCallingService snvCallingService

    final int MAX_RUNNING = 4

    @Scheduled(fixedDelay = 60000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        SnvConfig.withTransaction {
            SamplePair samplePair = snvCallingService.samplePairForSnvProcessing()
            if (samplePair) {
                SnvConfig config = SnvConfig.getLatest(
                        samplePair.project,
                        samplePair.seqType
                        )

                ProcessedMergedBamFile sampleType1BamFile = samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(
                        samplePair.sampleType1
                        )

                ProcessedMergedBamFile sampleType2BamFile = samplePair.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(
                        samplePair.sampleType2
                        )

                SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                        samplePair: samplePair,
                        instanceName: DateTimeFormat.forPattern("yyyy-MM-dd_HH'h'mm_Z").withZone(DateTimeZone.getDefault()).print(Instant.now()),
                        config: config,
                        sampleType1BamFile: sampleType1BamFile,
                        sampleType2BamFile: sampleType2BamFile,
                        latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
                        )
                snvCallingInstance.save(flush: true)

                samplePair.processingStatus = ProcessingStatus.NO_PROCESSING_NEEDED
                samplePair.save()

                createProcess(new ProcessParameter(value: snvCallingInstance.id.toString(), className: SnvCallingInstance.class.name))
                log.debug "SnvCallingStartJob started for: ${snvCallingInstance.toString()}"
            }
        }
    }


    private boolean hasFreeSlot() {
        JobExecutionPlan jep = getExecutionPlan()
        if (!jep || !jep.enabled) {
            return false
        }
        int n = Process.countByFinishedAndJobExecutionPlan(false, jep)
        long maxRunning = optionService.findOptionAsNumber("numberOfJobs", "SnvWorkflow", null, MAX_RUNNING)
        return (n < maxRunning)
    }

    @Override
    protected String getJobExecutionPlanName() {
        return "SnvWorkflow"
    }
}

package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.ConfigPerProjectAndSeqType.Purpose
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.AbstractStartJobImpl
import de.dkfz.tbi.otp.job.processing.ProcessParameter

@Component("snvStartJob")
@Scope("singleton")
class SnvCallingStartJob extends AbstractStartJobImpl {


    @Autowired
    SnvCallingService snvCallingService

    final int MAX_RUNNING = 4

    @Scheduled(fixedRate = 60000l)
    void execute() {
        if (!hasFreeSlot()) {
            return
        }
        ConfigPerProjectAndSeqType.withTransaction {
            SampleTypeCombinationPerIndividual sampleTypeCombinationPerIndividual = snvCallingService.samplePairForSnvProcessing()
            if (sampleTypeCombinationPerIndividual) {
                ConfigPerProjectAndSeqType configPerProjectAndSeqType = exactlyOneElement(
                        ConfigPerProjectAndSeqType.findAllByProjectAndSeqTypeAndPurpose(
                        sampleTypeCombinationPerIndividual.individual.project,
                        sampleTypeCombinationPerIndividual.seqType,
                        Purpose.SNV
                        )
                        )

                ProcessedMergedBamFile tumorBamFile = sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(
                        sampleTypeCombinationPerIndividual.sampleType1
                        )

                ProcessedMergedBamFile controlBamFile = sampleTypeCombinationPerIndividual.getLatestProcessedMergedBamFileForSampleTypeIfNotWithdrawn(
                        sampleTypeCombinationPerIndividual.sampleType2
                        )

                SnvCallingInstance snvCallingInstance = new SnvCallingInstance(
                        config: configPerProjectAndSeqType,
                        tumorBamFile: tumorBamFile,
                        controlBamFile: controlBamFile
                        )
                snvCallingInstance.save(flush: true)

                sampleTypeCombinationPerIndividual.needsProcessing = false
                sampleTypeCombinationPerIndividual.save()

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

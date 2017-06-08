package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.jobs.merging.MergingJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import org.junit.Test

class ProcessingStepTests  {

    @Test
    void testBelongsToMultiJob_WhenJobIsMultiJob_ShouldReturnTrue() {
        // AbstractOtpJob as dummy for Multijob
        Class testMultiJob = AbstractOtpJob
        ProcessingStep p = ProcessingStep.build([jobClass: testMultiJob.getName()])
        assert p.belongsToMultiJob()
    }

    @Test
    void testBelongsToMultiJob_WhenJobIsNoMultiJob_ShouldReturnFalse() {
        // MergingJob as dummy for non-Multijob
        Class testJob = MergingJob
        ProcessingStep p = ProcessingStep.build([jobClass: testJob.getName()])
        assert !p.belongsToMultiJob()
    }

    @Test
    public void testFindTopMostProcessingStep_WhenRestartedProcessingStep_ShouldReturnOriginalProcessingStep() {
        ProcessingStep originalStep = DomainFactory.createAndSaveProcessingStep()
        RestartedProcessingStep step1 = DomainFactory.createAndSaveRestartedProcessingStep(originalStep)
        RestartedProcessingStep step2 = DomainFactory.createAndSaveRestartedProcessingStep(step1)

        assert originalStep == ProcessingStep.findTopMostProcessingStep(step2)
    }

    @Test
    public void testFindTopMostProcessingStep_WhenProcessingStep_ShouldReturnProcessingStep() {
        ProcessingStep step = DomainFactory.createAndSaveProcessingStep()

        assert step == ProcessingStep.findTopMostProcessingStep(step)
    }


    @Test
    void testPbsJobDescription() {
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan(
                name: "testWorkFlow"
        )
        Process process = DomainFactory.createProcess(
                jobExecutionPlan: jobExecutionPlan
        )
        ProcessingStep step = DomainFactory.createProcessingStep(
                jobClass: "foo",
                process: process,
        )
        String expected = "otp_test_testWorkFlow_${step.id}_foo"

        assert expected == step.getClusterJobName()
    }

    @Test
    void testPbsJobDescriptionWithIndividual() {
        JobExecutionPlan jobExecutionPlan = DomainFactory.createJobExecutionPlan(
                name: "testWorkFlow"
        )
        Process process = DomainFactory.createProcess(
                jobExecutionPlan: jobExecutionPlan
        )
        ProcessingStep step = DomainFactory.createProcessingStep(
                jobClass: "foo",
                process: process,
        )
        SeqTrack seqTrack = DomainFactory.createSeqTrack([
                sample: DomainFactory.createSample([
                        individual: DomainFactory.createIndividual([
                                pid: 'pid',
                        ])
                ]),
        ])

        DomainFactory.createProcessParameter([
                process: process,
                value: seqTrack.id.toString(),
                className: seqTrack.getClass().getName(),
        ])
        String expected = "otp_test_pid_testWorkFlow_${step.id}_foo"

        assert expected == step.getClusterJobName()
    }
}

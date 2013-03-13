import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*


plan("mergingWorkflow") {
    start("start", "createMergingSetStartJob")
    job("createMergingSetJob", "createMergingSetJob")
}

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*


plan("createMergingSetWorkflow") {
    start("start", "createMergingSetStartJob")
    job("createMergingSetJob", "createMergingSetJob")
}

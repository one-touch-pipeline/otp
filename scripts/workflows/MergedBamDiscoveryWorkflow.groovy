import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("mergedBamDiscoveryWorkflow") {
    start("start", "mergedBamDiscoveryStartJob")
}

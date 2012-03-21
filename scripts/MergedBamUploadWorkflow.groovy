import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("mergedBamUploadWorkflow") {
    start("start", "mergedBamUploadStartJob")
}

import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("FastqcSummaryWorkflow") {
    start("start", "fastqcSummaryStartJob")
    job("calculateFastqcSummaryJob", "calculateFastqcSummaryJob")
}

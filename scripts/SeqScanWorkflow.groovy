import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("createSeqScan") {
    start("start", "seqScanStartJob")
    job("createSeqScan", "createSeqScanJob")
}

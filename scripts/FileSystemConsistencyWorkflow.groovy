import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("FileSystemConsistencyWorkflow") {
    start("start", "dataFileStatusStartJob")
    job("checkDataFileStatus", "checkDataFileStatusJob")
}

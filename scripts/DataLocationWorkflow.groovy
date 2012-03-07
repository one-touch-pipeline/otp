import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("DataLocationWorkflow") {
    start("start", "dataLocationStartJob")
    job("checkFinalLocation", "checkFinalLocationJob")
    job("checkViewByPid", "checkViewByPidJob")
}

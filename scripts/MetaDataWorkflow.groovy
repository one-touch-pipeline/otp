import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("loadMetaData") {
    start("start", "metaDataStartJob")
    job("registerInputFiles", "registerInputFilesJob")
    job("loadMetaData", "metaDataJob")
    job("blackmailRun", "blackmailRunJob")
    job("validateMetadata", "validateMetadataJob")
    job("validatePlatform", "validatePlatformJob")
    job("buildExecutionDate", "buildExecutionDateJob")
    job("buildSequenceTracks", "buildSequenceTracksJob")
    job("checkSequenceTracks", "checkSequenceTracksJob")
    job("setCompleted", "setCompletedJob")
}

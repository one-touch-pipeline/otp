import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("loadMetaData") {
    start("start", "metaDataStartJob")
    job("registerInputFiles", "registerInputFilesJob")
    job("loadMetaData", "metaDataJob")
    job("validateMetadata", "validateMetadataJob")
    job("buildExecutionDate", "buildExecutionDateJob")
    job("buildSequenceTracks", "buildSequenceTracksJob")
    job("checkSequenceTracks", "checkSequenceTracksJob")
    job("setCompleted", "setCompletedJob")
}

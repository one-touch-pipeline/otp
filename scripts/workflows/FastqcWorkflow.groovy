import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*


plan("FastqcWorkflow") {

    /**
     * Loads an unprocessed SeqTrack object from database and stores it as a process parameter
     */
    start("start", "fastqcStartJob")
    job("createOutputDirectories", "createFastqcOutputDirectoryJob")
    job("createFastqcArchive", "fastqcJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("createFastqcArchiveWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "createFastqcArchive", "__pbsIds")
        inputParameter("__pbsRealm", "createFastqcArchive", "__pbsRealm")
    }
    //job("createFastqcProcessedFile", "")
    job("uploadFastQCToDatabase", "uploadFastQCToDatabaseJob")
}


ctx.processingOptionService.createOrUpdate(
        "fastqcCommand",
        null,
        null,
        "fastqc --java /path/to/programs/jdk/jdk1.6.0_45/bin/java",
        "command for fastqc with java"
)

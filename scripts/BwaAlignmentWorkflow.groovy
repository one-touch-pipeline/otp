import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

plan("BwaAlignmentWorkflow") {
    start("start", "BwaAlignmentStartJob")
    job("createOutputDirectories", "createAlignmentOutputDirectoryJob")
    job("checkQualityEncoding", "checkQualityEncodingJob")
    job("bwaAlignment", "bwaAlignmentJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("bwaAlignmentWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "bwaAlignment", "__pbsIds")
        inputParameter("__pbsRealm", "bwaAlignment", "__pbsRealm")
    }
    job("bwaAlignmentValidation", "bwaAlignmentValidationJob")
    job("bwaPairingAndSorting", "bwaPairingAndSortingJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("bwaPairingAndSortingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "bwaPairingAndSorting", "__pbsIds")
        inputParameter("__pbsRealm", "bwaPairingAndSorting", "__pbsRealm")
    }
    job("bwaPairingValidation", "bamFileValidationJob") {
        constantParameter("BamType", "SORTED")
    }
    job("sortedBamIndexing", "bamFileIndexingJob") {
        constantParameter("BamType", "SORTED")
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("sortedBamIndexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "sortedBamIndexing", "__pbsIds")
        inputParameter("__pbsRealm", "sortedBamIndexing", "__pbsRealm")
    }
    job("sortedBamIndexingValidation", "bamFileIndexValidationJob") {
        constantParameter("BamType", "SORTED")
    }
    job("duplicateRemoval", "duplicateRemovalJob") {
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("duplicateRemovalWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "duplicateRemoval", "__pbsIds")
        inputParameter("__pbsRealm", "duplicateRemoval", "__pbsRealm")
    }
    job("rmdupValidation", "bamFileValidationJob") {
        constantParameter("BamType", "RMDUP")
    }
    job("rmdupBamIndexing", "bamFileIndexingJob") {
        constantParameter("BamType", "RMDUP")
        outputParameter("__pbsIds")
        outputParameter("__pbsRealm")
    }
    job("rmdupBamIndexingWatchdog", "myPBSWatchdogJob") {
        inputParameter("__pbsIds", "rmdupBamIndexing", "__pbsIds")
        inputParameter("__pbsRealm", "rmdupBamIndexing", "__pbsRealm")
    }
    job("rmdupBamIndexingValidation", "bamFileIndexValidationJob") {
        constantParameter("BamType", "RMDUP")
    }
    job("bwaAlignmentComplete", "bwaAlignmentCompleteJob")
}

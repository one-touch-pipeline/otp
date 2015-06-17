import de.dkfz.tbi.otp.dataprocessing.*

println "\n\n create transfer: "
def d = ProcessedMergedBamFile.withCriteria {
    //eq("fileOperationStatus", AbstractBamFile.FileOperationStatus.DECLARED)
    eq("qualityAssessmentStatus", AbstractBamFile.QaProcessingStatus.FINISHED)
    'in'("id", [
            // ids of PMBF

    ] as long[] )
}

//show all
d.each { println "${it}" }
println d.size()

/*
d.each {
    it.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
    it.md5sum = null
    println it.save(flush: true)
}
println d.size()
// */

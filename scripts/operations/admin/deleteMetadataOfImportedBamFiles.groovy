import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ImportProcess

int importId = 0// set it to delete the import process and related wrong metadata

ImportProcess importProcess = ImportProcess.findById(importId)

if (importProcess) {
    ExternallyProcessedMergedBamFile.withTransaction {
        Set<ExternallyProcessedMergedBamFile> bamFiles = importProcess.externallyProcessedMergedBamFiles
        println "ImportProcess ${importProcess.id} deleted"
        importProcess.delete()
        bamFiles.each {
            ExternalMergingWorkPackage workPackage = it.workPackage
            println "${it} deleted"
            it.delete()
            workPackage.delete()
        }
        it.flush()
        assert false
    }
}

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvJobResult


AbstractMergedBamFileService abstractMergedBamFileService = ctx.abstractMergedBamFileService

/**
 * rename existing withdrawn result files in the project folder
 *
 *
 * Withdrawn result files (ProcessedMergedBamFiles, RoddyBamFiles)
 * in the project folder are renamed by appending "-withdrawn",
 * for SNV results complete directories are renamed.
 *
 */


List<File> renameFiles = []

MergingWorkPackage.list().each { MergingWorkPackage mergingWorkPackage ->
    AbstractMergedBamFile bamFile = mergingWorkPackage.bamFileInProjectFolder
    if (bamFile && bamFile.withdrawn) {
        final File file = new File(abstractMergedBamFileService.destinationDirectory(bamFile), bamFile.bamFileName)
        renameFiles.add(file)

        List<SnvJobResult> snvJobResult = findSnvJobResultForBamFile(bamFile)
        snvJobResult.each { SnvJobResult result ->
            assert result.withdrawn
            // rename folder containing results
            renameFiles.add(result.snvCallingInstance.instancePath.absoluteDataManagementPath)
        }
    }
}

new File("$SCRIPT_ROOT_PATH/withdraw/renameWithdrawnFiles.sh").withPrintWriter { writer ->
    writer.write("#!/bin/bash\n")

    renameFiles.each {
        writer.write("mv ${it} ${it}-withdrawn\n")
    }
}



List<SnvJobResult> findSnvJobResultForBamFile(AbstractMergedBamFile bamFile) {
    return SnvJobResult.createCriteria().list {
        snvCallingInstance {
            or {
                eq('sampleType1BamFile', bamFile)
                eq('sampleType2BamFile', bamFile)
            }
        }
    }
}

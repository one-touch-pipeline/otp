import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*

/**
 * rename existing withdrawn result files in the project folder
 *
 *
 * Withdrawn result files (ProcessedMergedBamFiles, RoddyBamFiles)
 * in the project folder are renamed by appending "-withdrawn",
 * for analysis instances complete directories are renamed.
 */


File generated_script_to_run_manually = new File(ConfigService.getInstance().getScriptOutputPath(), "/withdraw/renameWithdrawnFiles.sh")
List<File> renameFiles = []

MergingWorkPackage.list().each { MergingWorkPackage mergingWorkPackage ->
    AbstractMergedBamFile bamFile = mergingWorkPackage.bamFileInProjectFolder
    if (bamFile && bamFile.withdrawn) {
        final File file = new File(bamFile.baseDirectory, bamFile.bamFileName)
        renameFiles.add(file)

        List<BamFilePairAnalysis> analysisInstances = findAnalysisInstanceForBamFile(bamFile)
        analysisInstances.each { BamFilePairAnalysis result ->
            assert result.withdrawn
            // rename folder containing results
            renameFiles.add(result.instancePath.absoluteDataManagementPath)
        }
    }
}


generated_script_to_run_manually.withPrintWriter { writer ->
    writer.write("#!/bin/bash\n")

    renameFiles.each {
        writer.write("mv ${it} ${it}-withdrawn\n")
    }
}


List<BamFilePairAnalysis> findAnalysisInstanceForBamFile(AbstractMergedBamFile bamFile) {
    return BamFilePairAnalysis.createCriteria().list {
        or {
            eq('sampleType1BamFile', bamFile)
            eq('sampleType2BamFile', bamFile)
        }
    }
}

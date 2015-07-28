// This script compares the sizes of BAM files on the file system to the file sizes stored in the database.
// Expect inconsistencies to be reported if you run this script while the transfer workflow is running.

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage

PrintWriter out = new PrintWriter(new FileOutputStream(
        "$SCRIPT_ROOT_PATH/script_output/consistencyChecking/BamFileSizeConsistency_" + new Date().format("yyyy-MM-dd_HH.mm.ss.SSS_Z") + ".tsv"), true)
try {
    Collection<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.list()
    out.println("Found ${mergingWorkPackages.size()} MergingWorkPackages")
    out.println()
    out.println("MWP ID\tConsistent\tBAM file ID\tFileOpStat\tWithdrawn\tDB size\tExists on FS\tFile system size\tPath")
    mergingWorkPackages.each { MergingWorkPackage mergingWorkPackage ->
        out.print("${mergingWorkPackage.id}\t")
        AbstractMergedBamFile bamFile = mergingWorkPackage.bamFileInProjectFolder
        if (bamFile) {

            final File file = new File(ctx.abstractMergedBamFileService.destinationDirectory(bamFile), bamFile.bamFileName)

            boolean exists = file.exists()
            long length = file.length()
            try {
                assert exists == (length > 0L)
            } catch (AssertionError e) {
                throw new Error("Assertion failed for ${file}", e)
            }

            boolean consistent
            if (bamFile.withdrawn) {
                consistent = !exists || length == bamFile.fileSize
            } else {
                consistent = exists && length == bamFile.fileSize
            }

            out.println("${consistent}\t${bamFile.id}\t${bamFile.fileOperationStatus}\t${bamFile.withdrawn}\t${bamFile.fileSize}\t${exists}\t${length}\t${file}")
        } else {
            out.println("true\tnone")
        }
    }
} finally {
    out.close()
}
''

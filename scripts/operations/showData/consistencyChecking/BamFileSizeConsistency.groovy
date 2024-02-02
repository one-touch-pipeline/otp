/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

// This script compares the sizes of BAM files on the file system to the file sizes stored in the database.
// Expect inconsistencies to be reported if you run this script while the transfer workflow is running.

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage

ConfigService configService = ctx.configService

PrintWriter out = new PrintWriter(new File(
        configService.scriptOutputPath,
        "consistencyChecking/BamFileSizeConsistency_${ new Date().format("yyyy-MM-dd_HH.mm.ss.SSS_Z") }.tsv"
))
try {
    Collection<MergingWorkPackage> mergingWorkPackages = MergingWorkPackage.list()
    out.println("Found ${mergingWorkPackages.size()} MergingWorkPackages")
    out.println()
    out.println("MWP ID\tConsistent\tBAM file ID\tFileOpStat\tWithdrawn\tDB size\tExists on FS\tFile system size\tPath")
    mergingWorkPackages.each { MergingWorkPackage mergingWorkPackage ->
        out.print("${mergingWorkPackage.id}\t")
        AbstractBamFile bamFile = mergingWorkPackage.bamFileInProjectFolder
        if (bamFile) {
            final File file = new File(bamFile.baseDirectory, bamFile.bamFileName)

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

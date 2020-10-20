/*
 * Copyright 2011-2019 The OTP authors
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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * rename existing withdrawn result files in the project folder
 *
 *
 * Withdrawn result files (ProcessedMergedBamFiles, RoddyBamFiles)
 * in the project folder are renamed by appending "-withdrawn",
 * for analysis instances complete directories are renamed.
 */

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

Path generated_script_to_run_manually = fileService.toPath(configService.getScriptOutputPath(), fileSystem).resolve("withdraw").resolve("renameWithdrawnFiles.sh")
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(generated_script_to_run_manually.parent, realm)
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

println "create file: ${generated_script_to_run_manually}"

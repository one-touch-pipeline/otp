/*
 * Copyright 2011-2023 The OTP authors
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
import de.dkfz.tbi.otp.dataswap.SampleSwapService
import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Template to move sample or change the sample type. It can also rename the file names.
 *
 * Therefore the following input are needed:
 * - OldProjectName: The name of the project the patient is in currently
 * - NewProjectName: The new project name, may the same
 * - OldPid: The old patient
 * - NewPid: The new patient pid, may the same if the project is differ
 * - OldSampleTypeName: The old sample type name
 * - NewSampleTypeName: The new sample type name or the same as the old. If changed, the patient may not have a sample of that sampleType
 * - Map of all fastq file names:
 *   - OldFileName: The old name of the fastq file
 *   - NewFileName: The new name of the fastq file. May be empty if it should not change
 * - uniqueScriptName: The name used for the generated scripts. That are a bash script to change the file system and a groovy script for trigger the alignment.
 *
 * You can use multiple 'dataSwapService.moveSample' calls in the script, duplicate therefore the call.
 */

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService
SampleSwapService sampleSwapService = ctx.sampleSwapService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

StringBuilder outputStringBuilder = new StringBuilder()

final Path scriptOutputDirectory = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap')
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(scriptOutputDirectory, realm)
fileService.setPermission(scriptOutputDirectory, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

/** have we manually checked yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean linkedFilesVerified = false

/** are missing fastq files an error? (usually yes, since we must repeat most analyses steps after a swap) */
boolean failOnMissingFiles = true

try {
    Individual.withTransaction {

        sampleSwapService.swap(
                new SampleSwapParameters([
                        projectNameSwap      : new Swap('oldProjectName', 'newProjectName'),
                        pidSwap              : new Swap('oldPid', substitute: 'newPid'),
                        sampleTypeSwap       : new Swap('oldSampleTypeName', 'newSampleTypeName'),
                        rawSequenceFileSwaps: [
                                new Swap('oldFileName1', 'newFileName1'),
                                new Swap('oldFileName2', 'newFileName2'),
                                new Swap('oldFileName3', ''),
                                new Swap('oldFileName4', ''),
                        ],
                        bashScriptName       : 'uniqueScriptName',
                        log                  : outputStringBuilder,
                        failOnMissingFiles   : failOnMissingFiles,
                        scriptOutputDirectory: scriptOutputDirectory,
                        linkedFilesVerified  : linkedFilesVerified,
                ])
        )
        assert false
    }
} finally {
    println outputStringBuilder
}

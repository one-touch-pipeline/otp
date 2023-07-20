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
import de.dkfz.tbi.otp.dataswap.IndividualSwapService
import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.IndividualSwapParameters
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Template to move/rename a patient. It can also change the SampleTypes of the patient and rename the file names.
 *
 * Therefore the following input are needed:
 * - OldProject: The name of the project the patient is in currently
 * - NewProject: The new project name, can be the same if no change is necessary
 * - OldPid: The patient to move or rename
 * - NewPid: the new patient pid, can be the same if the new project is different
 * - Map of all SampleTypes names for the patient.
 *   - OldSampleTypeName: The old sample type name
 *   - NewSampleTypeName: The new sample type name or the same as the old. It may not another existing sampleType Name (Creates an Conflict)
 * - Map of all fastq file names:
 *   - OldFileName: The old name of the fastq file
 *   - NewFileName: The new name of the fastq file. May be empty if it should not change
 * - uniqueScriptName: The name used for the generated scripts. That are a bash script to change the file system and a groovy script for trigger the alignment.
 *
 * You can use multiple 'dataSwapService.moveIndividual' calls in the script, duplicate therefore the call.
 */

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService
IndividualSwapService individualSwapService = ctx.individualSwapService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

StringBuilder log = new StringBuilder()

final Path scriptOutputDirectory = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap')
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(scriptOutputDirectory, realm)
fileService.setPermission(scriptOutputDirectory, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

/** have we manually checked yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean linkedFilesVerified = false

/** are missing fastq files an error? (usually yes, since we must repeat most analyses steps after a swap) */
boolean failOnMissingFiles = true

try {
    Individual.withTransaction {
        individualSwapService.swap(
                new IndividualSwapParameters(
                        projectNameSwap: new Swap('oldProjectName', 'newProjectName'),
                        pidSwap: new Swap('oldPid', 'newPid'),
                        sampleTypeSwaps: [
                                new Swap('oldSampleType1', 'newSampleType1'),
                                new Swap('oldSampleType2', 'newSampleType2'),
                        ],
                        rawSequenceFileSwaps: [
                                new Swap('oldFileName1', 'newFileName1'),
                                new Swap('oldFileName2', 'newFileName2'),
                                new Swap('oldFileName3', ''),
                                new Swap('oldFileName4', ''),
                        ],
                        bashScriptName: 'uniqueScriptName',
                        log: log,
                        failOnMissingFiles: failOnMissingFiles,
                        scriptOutputDirectory: scriptOutputDirectory,
                        linkedFilesVerified: linkedFilesVerified,
                )
        )

        assert false: "DEBUG: intentionally failed transaction"
    }
} finally {
    println log
}

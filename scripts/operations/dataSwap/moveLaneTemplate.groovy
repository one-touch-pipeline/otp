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

import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Template to move/rename a patient call. It can also change the SampleTypes of the patient and rename the file names.
 *
 * Therefore the following input are needed:
 * - OldProject: The name of the project the patient is in currently
 * - NewProject: The new project name, may the same
 * - OldPid: The old patient
 * - NewPid: The new patient pid, may the same if the project is differ
 * - OldSampleTypeName: The old sample type name
 * - NewSampleTypeName: The new sample type name or the same as the old.
 * - OldSeqTypeName: The old seqType name
 * - NewSeqTypeName: The new seqType name
 * - OldLibraryLayout: The old libraryLayout
 * - NewLibraryLayout: The new libraryLayout. Should have the same file count, otherwise the swap to not work.
 * - RunName: the name of the run the lanes are on.
 * - list of otp lanes (lane with barcode): The lane ids of the run for the migration
 * - Map of all fastq file names for given run and laneids:
 *   - OldFileName: The old name of the fastq file
 *   - NewFileName: The new name of the fastq file. May be empty if it should not change
 * - sampleNeedsToBeCreated: true or false, indicating whether the new sample needs to be created (true) or is already existent (false)
 * - uniqueScriptName: The name used for the generated scripts. That are a bash script to change the file system and a groovy script for trigger the alignment.
 *
 * You can use multiple 'dataSwapService.swapLane' calls in the script, duplicate therefore the call.
 */

ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService
DataSwapService dataSwapService = ctx.dataSwapService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

StringBuilder outputStringBuilder = new StringBuilder()

final Path scriptOutputDirectory = fileService.toPath(configService.getScriptOutputPath(), fileSystem).resolve('sample_swap')
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(scriptOutputDirectory, realm)
fileService.setPermission(scriptOutputDirectory, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

/** have we manually checked yet if all (potentially symlinked) fastq datafiles still exist on the filesystem? */
boolean linkedFilesVerified = false

/** are missing fastq files an error? (usually yes, since we must repeat most analyses steps after a swap) */
boolean failOnMissingFiles = true

try {
    Individual.withTransaction {
        dataSwapService.swapLane(
                [
                        'oldProjectName'   : ['oldProject'],
                        'newProjectName'   : ['newProject'],
                        'oldPid'           : ['oldPid'],
                        'newPid'           : ['newPid'],
                        'oldSampleTypeName': ['oldSampleType'],
                        'newSampleTypeName': ['newSampleType'],
                        'oldSeqTypeName'   : ['oldSeqTypeName'],
                        'newSeqTypeName'   : ['newSeqTypeName'],
                        'oldSingleCell'    : ['oldSingleCell'],
                        'newSingleCell'    : ['newSingleCell'],
                        'oldLibraryLayout' : ['oldLibraryLayout'],
                        'newLibraryLayout' : ['newLibraryLayout'],
                        'runName'          : ['runName'],
                        'lane'             : [
                                'lane1',
                                'lane2',
                                //...
                        ],
                        'sampleNeedsToBeCreated': ['sampleNeedsToBeCreated'],
                ],
                [
                        'oldFileName1': 'newFileName1',
                        'oldFileName2': 'newFileName2',
                        'oldFileName3': '',
                        'oldFileName4': '',
                ],
                'uniqueScriptName',
                outputStringBuilder,
                failOnMissingFiles,
                scriptOutputDirectory,
                linkedFilesVerified
        )

        assert false
    }
} finally {
    println outputStringBuilder
}

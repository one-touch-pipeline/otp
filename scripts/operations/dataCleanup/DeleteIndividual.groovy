/*
 * Copyright 2011-2026 The OTP authors
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
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.IndividualService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.workflowExecution.Artefact

import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.Files

/**
 * Script to delete or archive individuals with all associated data.
 *
 * In deletion mode, it deletes all data of the individuals from the OTP database and generates a bash script
 * to delete the corresponding data from the file system.
 *
 * In archiving mode, it deletes all data of the individuals from the OTP database without generating a deletion
 * script. Instead, all affected data folders are written to the specified archive output file and printed to
 * the console. The data itself remains on the file system and can be archived separately.
 *
 * The individuals are specified by their PIDs.
 *
 * The script provides a `tryRun` mode to roll back database changes after processing, allowing the result to be
 * checked before the changes are committed.
 */

// input area
// ----------------------

/**
 * input area for pids, one pid per line.
 * All values are trimmed, empty lines and lines starting with '#' are ignored.
 */
String pids = """
#PID
"""

/**
 * Absolute path of the file containing all directories affected by archiving.
 * Only used if archiving is true.
 */
String archiveFilePath = ""

/**
 * Flag to indicate, if it should be checked for files only linked and for external bam files.
 * If checked and some found, an exception is thrown
 */
boolean check = true

/**
 * Flag to archive the data
 * and generate a list with the location of the archived files.
 */
boolean archiving = false

/**
 * Flag to allow a trial run, which rolls back the changes at the end (if set to `true`),
 * or to execute the changes (if set to `false`).
 */
boolean tryRun = true

// script area
// -----------------------------

List<Individual> individuals = pids.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect {
    CollectionUtils.exactlyOneElement(
            Individual.findAllByPid(it),
            "Could not find pid '${it}'"
    )
}.unique()

assert individuals: "No individuals were defined"

String combinedPids = individuals*.pid.join('__')
String deletionFileName = "Delete_${combinedPids.size() < 110 ? combinedPids : combinedPids.substring(0, 100) + '_and_others'}.sh"

DeletionService deletionService = ctx.deletionService
FileService fileService = ctx.fileService
ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
ProcessingOptionService processingOptionService = ctx.processingOptionService

FileSystem fileSystem = fileSystemService.remoteFileSystem

Path baseOutputDir = fileService.toPath(configService.scriptOutputPath, fileSystem).resolve('sample_swap')

Individual.withTransaction {
    if (archiving) {
        assert archiveFilePath?.trim():
                "No archive output file was given"

        if (!archiveFilePath.endsWith(".txt")) {
            archiveFilePath = archiveFilePath.concat(".txt")
        }

        Path archiveFile = fileSystem.getPath(archiveFilePath)

        assert archiveFile.isAbsolute():
                "Archive output file has to be an absolute path"

        Set<Path> archiveDirectories = [] as Set<Path>

        individuals.each { Individual individual ->
            println "Archive: ${individual} of project ${individual.project}"

            String deletionCommands = deletionService.deleteIndividual(
                    individual,
                    check
            )

            deletionCommands.readLines()*.trim().findAll { String line ->
                        line.startsWith('rm -rf ')
                    }.collect { String line ->
                        line.substring('rm -rf '.length())
                    }.findAll().each { String pathString ->
                        Path path = fileSystem.getPath(pathString)

                        if (!Files.isDirectory(path)) {
                            archiveDirectories << path.parent
                        } else {
                            archiveDirectories << path
                        }
                    }
        }

        Set<Path> filteredArchiveDirectories = archiveDirectories.findAll { Path path ->
            !archiveDirectories.any { Path other ->
                other != path && path.startsWith(other)
            }
        }

        List<String> archiveDirectoryStrings = filteredArchiveDirectories*.toString().sort()

        println ''
        println "Folders affected by archiving:"
        archiveDirectoryStrings.each { String path ->
            println path
        }

        String unixGroup = processingOptionService.findOptionAsString(
                ProcessingOption.OptionName.OTP_USER_LINUX_GROUP
        )

        Path archiveList = fileService.createOrOverwriteScriptOutputFile(
                archiveFile.parent,
                archiveFile.fileName.toString(),
                unixGroup
        )

        archiveList << archiveDirectoryStrings.join('\n')

        println ''
        println "Archive folder list is written to:"
        println archiveList
    } else {
        List<String> allFilesToRemove = [
                "#!/bin/bash",
                "",
                "set -evx",
        ]

        individuals.each { Individual individual ->
            println "Delete: ${individual} of project ${individual.project}"

            allFilesToRemove << "\n\n#${individual}"
            allFilesToRemove << deletionService.deleteIndividual(
                    individual,
                    check
            )
        }

        String unixGroup = processingOptionService.findOptionAsString(
                ProcessingOption.OptionName.OTP_USER_LINUX_GROUP
        )

        Path deleteFileCmd = fileService.createOrOverwriteScriptOutputFile(
                baseOutputDir,
                deletionFileName,
                unixGroup
        )

        deleteFileCmd << allFilesToRemove.join('\n')

        println ''
        println "Deletion file is written to:"
        println deleteFileCmd
    }

    assert !tryRun: "Rollback, since it was only a tryRun."
}

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
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.TimeFormats

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path

/**
 *
 * This script addresses the change in withdraw behavior where UUID structure links are no longer deleted
 * but their group is changed. For existing withdrawn data that had their links deleted, this script
 * recreates those links and sets the appropriate group.
 *
 * This script creates a bash script that:
 * - Creates missing links for withdrawn RawSequenceFiles
 * - Sets the group of the links to the withdrawn group from processing option
 * - Handles both view-by-PID links and single cell well links
 */

ConfigService configService = ctx.configService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
ProcessingOptionService processingOptionService = ctx.processingOptionService
RawSequenceDataWorkFileService rawSequenceDataWorkFileService = ctx.rawSequenceDataWorkFileService
RawSequenceDataViewFileService rawSequenceDataViewFileService = ctx.rawSequenceDataViewFileService
RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService = ctx.rawSequenceDataAllWellFileService

String withdrawnGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.WITHDRAWN_UNIX_GROUP)
String otpUserGroup = processingOptionService.findOptionAsString(ProcessingOption.OptionName.OTP_USER_LINUX_GROUP)
FileSystem fileSystem = fileSystemService.remoteFileSystem

// Find all withdrawn RawSequenceFiles
List<RawSequenceFile> withdrawnFiles = RawSequenceFile.findAllByFileWithdrawn(true)

println "Found ${withdrawnFiles.size()} withdrawn RawSequenceFiles"

// Generate bash script content
List<String> bashScript = [
        FileService.BASH_HEADER,
        "",
        "# Migration script for OTP-2907: Create missing UUID structure links for withdrawn data",
        "# Generated on: ${new Date()}",
        "",
]

List<String> linksToCreate = []
List<String> groupsToChange = []
List<String> missingFiles = []
List<String> failedFiles = []
int processedCount = 0
int missingLinksCount = 0

withdrawnFiles.each { RawSequenceFile rawSequenceFile ->
    try {
        Path sourceFile = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
        if (!Files.exists(sourceFile)) {
            missingFiles << "RawSequenceFile ${rawSequenceFile.id}: Source file missing: ${sourceFile}".toString()
            return
        }

        Path viewByPidLink = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
        if (!Files.exists(viewByPidLink)) {
            linksToCreate << "mkdir -p ${LocalShellHelper.shellEscape(viewByPidLink.parent.toString())}".toString()
            linksToCreate << "ln -sr ${LocalShellHelper.shellEscape(sourceFile.toString())} ${LocalShellHelper.shellEscape(viewByPidLink.toString())}".toString()
            groupsToChange << "chgrp --no-dereference --verbose ${LocalShellHelper.shellEscape(withdrawnGroup)} ${LocalShellHelper.shellEscape(viewByPidLink.toString())}".toString()
            missingLinksCount++
        } else {
            groupsToChange << "chgrp --no-dereference --verbose ${LocalShellHelper.shellEscape(withdrawnGroup)} ${LocalShellHelper.shellEscape(viewByPidLink.toString())}".toString()
        }

        if (rawSequenceFile.seqType.singleCell && rawSequenceFile.seqTrack.singleCellWellLabel) {
            Path wellLink = rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile)
            if (!Files.exists(wellLink)) {
                linksToCreate << "mkdir -p ${LocalShellHelper.shellEscape(wellLink.parent.toString())}".toString()
                linksToCreate << "ln -sr ${LocalShellHelper.shellEscape(sourceFile.toString())} ${LocalShellHelper.shellEscape(wellLink.toString())}".toString()
                groupsToChange << "chgrp --no-dereference --verbose ${LocalShellHelper.shellEscape(withdrawnGroup)} ${LocalShellHelper.shellEscape(wellLink.toString())}".toString()
                missingLinksCount++
            } else {
                groupsToChange << "chgrp --no-dereference --verbose ${LocalShellHelper.shellEscape(withdrawnGroup)} ${LocalShellHelper.shellEscape(wellLink.toString())}".toString()
            }
        }

        processedCount++
        if (processedCount % 100 == 0) {
            println "Processed ${processedCount} files..."
        }
    } catch (Exception e) {
        String errorMessage = "RawSequenceFile ${rawSequenceFile.id}: ${e.class.simpleName}: ${e.message}"
        println "Error processing ${rawSequenceFile.id}: ${e.message}"
        failedFiles << errorMessage
    }
}

bashScript << "echo 'Creating missing links...'"
bashScript.addAll(linksToCreate)
bashScript << ""
bashScript << "echo 'Setting groups on links...'"
bashScript.addAll(groupsToChange)
bashScript << ""

if (missingFiles.size() > 0) {
    bashScript << "echo 'NOTE: ${missingFiles.size()} files with missing source files were skipped:'".toString()
    missingFiles.each { missingFile ->
        bashScript << "# SKIPPED: ${missingFile}".toString()
    }
    bashScript << ""
}

if (failedFiles.size() > 0) {
    bashScript << "echo 'WARNING: ${failedFiles.size()} files failed to process due to errors:'".toString()
    failedFiles.each { failedFile ->
        bashScript << "# FAILED: ${failedFile}".toString()
    }
    bashScript << ""
}

bashScript << "echo 'Migration completed. Created ${missingLinksCount} missing links and processed ${processedCount} withdrawn files.'".toString()

String timestamp = TimeFormats.DATE_TIME_SECONDS_DASHES.getFormattedDate(new Date())
String scriptFileName = "otp-2907-restore-withdrawn-links-${timestamp}.sh"
Path outputFile = fileService.toPath(configService.scriptOutputPath, fileSystem)
        .resolve('migrations')
        .resolve(scriptFileName)

fileService.createFileWithContent(outputFile, bashScript.join('\n'),
        otpUserGroup, FileService.OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION)

println """
Migration script created: ${outputFile}

Summary:
- Total withdrawn RawSequenceFiles found: ${withdrawnFiles.size()}
- Successfully processed: ${processedCount}
- Missing source files (skipped): ${missingFiles.size()}
- Processing failures: ${failedFiles.size()}
- Found ${missingLinksCount} missing links that need to be created
- Script will create missing links and set group to: ${withdrawnGroup}

${missingFiles.size() > 0 ? "\nMissing files that were skipped:" : ""}
${missingFiles.size() > 0 ? missingFiles.join('\n') : ""}

${failedFiles.size() > 0 ? "\nFiles that failed to process:" : ""}
${failedFiles.size() > 0 ? failedFiles.join('\n') : ""}

Please review the script before executing it on the file system.
"""

/*
 * Copyright 2011-2020 The OTP authors
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

package operations.dataCorrection

import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.FileSystem
import java.nio.file.Path

/**
 * Script to delete all Processed Data of the selected SeqTracks and withdraw the DataFiles.
 *
 * Deletes:
 *   - Alignments
 *   - Analyses
 *   - view-by-pid links of datafiles
 *
 * The script works in two sections: overview and deletion.
 * Provide the input and select the SeqTracks. Then execute the script for an overview of
 * what SeqTracks and DataFiles are affected.
 * If this is fine, comment out the first DEBUG assert.
 *
 * Execute the script again to get an overview of what will be done with the data.
 * If it does not throw an exception before the second DEBUG assert and what will be done
 * is fine, you can remove the second DEBUG assert and actually delete the data.
 *
 * Execute the generated script after looking over it. It is located in the typical sample
 * swap location, but the path is also printed out at the end.
 *
 * Input:
 *   - a swap label for the deletion script on the filesystem
 *   - the dataFileComment is shown in the comment box on the DataFile details view
 *   - the withdrawnComment is shown in the status table on the DataFile details view and in exports
 */

// INPUT
String swapLabel = swapLabel = 'OTRS-________________-something-descriptive'

String dataFileComment = """\
""".trim()

String withdrawnComment = """\
""".trim() ?: null

Collection<SeqTrack> seqTracks = SeqTrack.withCriteria {
    or {
        'in'('id', [
                -1,
        ].collect { it.toLong() })
        sample {
            individual {
                or {
                    'in'('mockPid', [
                            '',
                    ])
                    project {
                        'in'('name', [
                                '',
                        ])
                    }
                }
            }
        }
        ilseSubmission(JoinType.LEFT_OUTER_JOIN.getJoinTypeValue()) {
            'in'('ilseNumber', [
                    -1,
            ])
        }
    }
}

// WORK
ConfigService configService = ctx.configService
FileSystemService fileSystemService = ctx.fileSystemService
FileService fileService = ctx.fileService

Realm realm = configService.defaultRealm
FileSystem fileSystem = fileSystemService.getRemoteFileSystem(realm)

final Path SCRIPT_OUTPUT = fileService.toPath(configService.getScriptOutputPath(), fileSystem).resolve('sample_swap').resolve("${swapLabel}.sh")
fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(SCRIPT_OUTPUT.parent, realm)
SCRIPT_OUTPUT.text = ""
fileService.setPermission(SCRIPT_OUTPUT, fileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)

void assertSeqTracks(List<SeqTrack> seqTracks) {
    seqTracks.each { SeqTrack seqTrack ->
        seqTrack.dataFiles.each { DataFile dataFile ->
            assert dataFile.fileExists: "SeqTrack ${seqTrack} contains non-existing DataFiles, data should not be deleted"
        }
    }
}

String appendOrSet(String target, String input) {
    return target ? "${target}\n\n${input}" : input
}

void withdrawDataFile(DataFile dataFile, String withdrawnComment, String dataFileComment) {
    assert !dataFile.fileWithdrawn: "DataFile is already withdrawn"
    assert !dataFile.withdrawnDate: "DataFile already has a withdrawnDate: ${dataFile.withdrawnDate}"

    dataFile.fileWithdrawn = true
    dataFile.withdrawnDate = new Date()
    dataFile.withdrawnComment = appendOrSet(dataFile.withdrawnComment, withdrawnComment)

    ctx.commentService.saveComment(dataFile, appendOrSet(dataFile.comment?.comment, dataFileComment))
    assert dataFile.save(flush: true)
}

List<String> getPathsToDelete(SeqTrack seqTrack) {
    List<String> paths = []
    paths.addAll(seqTrack.dataFiles.collect { DataFile dataFile ->
        ctx.lsdfFilesService.getFileViewByPidPath(dataFile)
    })
    paths.addAll(ctx.deletionService.deleteAllProcessingInformationAndResultOfOneSeqTrack(seqTrack)['dirsToDelete']*.absolutePath)
    return paths
}

SeqTrack.withTransaction {
    println """\
    |# Comment for DataFile.comment:  (shown in the comment box on the DataFile details view)
    |${dataFileComment}
    |###\n""".stripMargin()

    println """\
    |# Comment for DataFile.withdrawnComment:  (shown in the status table on the DataFile details view and in exports)
    |${withdrawnComment}
    |###\n""".stripMargin()

    println "# Overview of affected SeqTracks:"
    seqTracks.each { SeqTrack seqTrack ->
        println seqTrack
        DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
            println "  - DF ${dataFile.id}: ${dataFile}"
        }
    }

    assertSeqTracks(seqTracks)

    assert false: "DEBUG 1, remove to continue with overview of deletion"

    println "\n# Deletion: \n"

    List<String> dirsToDelete = []
    seqTracks.each { SeqTrack seqTrack ->
        println "deleting processing information of ${seqTrack}"
        dirsToDelete << getPathsToDelete(seqTrack)

        DataFile.findAllBySeqTrack(seqTrack).each { DataFile dataFile ->
            println "  - withdrawing: DF ${dataFile.id}: ${dataFile}"
            withdrawDataFile(dataFile, withdrawnComment, dataFileComment)
        }
    }
    println "\n\n"

    String script = """\
    |#!/bin/bash
    |
    |set -e
    |set -v
    |
    |${dirsToDelete.flatten().collect { "rm -rf ${it}" }.join("\n")}
    |""".stripMargin()

    println "# Created the following script for deletion on the filesystem:"
    println "# Script can be found here: ${SCRIPT_OUTPUT.toAbsolutePath()}\n"
    println "${script}"

    SCRIPT_OUTPUT.text = "${script}"

    assert false: "DEBUG 2, remove to continue with actual deletion"
}

''

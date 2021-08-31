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

import de.dkfz.tbi.otp.dataprocessing.FastqcDataFilesService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ScriptInputHelperService

import java.nio.file.FileSystem
import java.nio.file.Files

/**
 * Script to remove the mark as withdrawn.
 *
 * It fetches all seqTracks for all samples given by PID, SAMPLE_TYPE SEQ_TYPE and LIBRARY_LAYOUT, removes the withdrawn flag and adds a withdrawn comment.
 * It does not consider alignment or analysis, since they could also for other reason marked as withdrawn.
 *
 * Also the withdraw MetadataEntries are adapted.
 */

/**
 * Multi selector using:
 * - pid
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 * - withdrawn comment: comment in quote marks 'withdrawn Comment'
 *
 * The columns can be separated by comma, semicolon or tab. Each value is also trimmed.
 * # indicates commentaries, that will be ignored in the script.
 */
String multiColumnInputSample = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false, 'withdraw comment'
#pid3,control,WES,PAIRED,false, 'long withdrawn comment
with multiple lines'
#pid5,control,RNA,SINGLE,true,sampleName2, 'withdrawn comment'
"""


//service
ScriptInputHelperService scriptInputHelperService = ctx.scriptInputHelperService

assert (scriptInputHelperService.isCommentInMultiLineDefinition(multiColumnInputSample)):
        "Please provide comments for every inputSample in the multiColumnInputSample"

List<SeqTrack> seqTracks = scriptInputHelperService.seqTracksBySampleDefinition(multiColumnInputSample)
List<String> comments = scriptInputHelperService.getCommentsFromMultiLineDefinition(multiColumnInputSample)

Map<SeqTrack,String> seqTracksWithComments = [seqTracks, comments].transpose().collectEntries {[it[0],it[1]]}

List dirsToLink = []
final String TRIM_LINE = "----------------------------------------"

SeqTrack.withTransaction {
    UnWithdrawer.ctx = ctx
    seqTracksWithComments.each {seqTrackWithComment ->
        UnWithdrawer.unwithdraw(seqTrackWithComment.key, seqTrackWithComment.value, dirsToLink)
    }
    println("\n" + TRIM_LINE + "\n")
    println dirsToLink.join("\n")

    assert false: 'Fail for debug reason, remove if the output is okay'
}

class UnWithdrawer {

    static ctx

    static void unwithdraw(final SeqTrack seqTrack, String comment, List dirsToLink) {
        println "\n\nUnwithdraw $seqTrack"

        DataFile.findAllBySeqTrack(seqTrack).each { unwithdraw(it, comment, dirsToLink) }
    }

    static void unwithdraw(final DataFile dataFile, String comment, List dirsToLink) {

        FileSystemService fileSystemService = ctx.fileSystemService
        LsdfFilesService lsdfFilesService = ctx.lsdfFilesService
        FastqcDataFilesService fastqcDataFilesService = ctx.fastqcDataFilesService
        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        println "Unwithdrawing DataFile ${dataFile}"
        dirsToLink.add("ln -rs ${lsdfFilesService.getFileFinalPath(dataFile)} ${lsdfFilesService.getFileViewByPidPath(dataFile)}")
        dirsToLink.add("chrgrp ${dataFile.project.unixGroup} ${lsdfFilesService.getFileViewByPidPathAsPath(dataFile, fileSystem)}")
        [lsdfFilesService.getFileFinalPathAsPath(dataFile, fileSystem),
         lsdfFilesService.getFileMd5sumFinalPathAsPath(dataFile, fileSystem),
         fastqcDataFilesService.fastqcOutputPath(dataFile, fileSystem),
         fastqcDataFilesService.fastqcOutputMd5sumPath(dataFile, fileSystem),
         fastqcDataFilesService.fastqcHtmlPath(dataFile, fileSystem)].findAll { path ->
            Files.exists(path)
        }.collect { filePath ->
            dirsToLink.add("chgrp ${dataFile.project.unixGroup} ${filePath}")
        }
        dataFile.withdrawnDate = null
        if (!dataFile.withdrawnComment?.contains(comment)) {
            dataFile.withdrawnComment = "${dataFile.withdrawnComment ? "${dataFile.withdrawnComment}\n" : ""}${comment}"
        }
        dataFile.fileWithdrawn = false
        assert dataFile.save(flush: true)
    }
}

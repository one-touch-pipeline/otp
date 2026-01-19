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

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackWithComment
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.ScriptInputHelperService
import de.dkfz.tbi.otp.utils.exceptions.FileAccessForProjectNotAllowedException
import de.dkfz.tbi.otp.withdraw.UnwithdrawService
import de.dkfz.tbi.otp.withdraw.UnwithdrawStateHolder

/**
 * Script to revert the withdrawal of data (removes the "withdrawn" flag).
 *
 * For all file changes a bash script is created, which needs to be executed manually.
 *
 * It fetches all seqTracks for the samples for the given PID, SAMPLE_TYPE SEQ_TYPE and LIBRARY_LAYOUT,
 * removes the withdrawn flag for these samples, and adds a withdrawn comment.
 * It does not consider alignment or analysis files, since they could also have been marked as withdrawn for other reasons.
 *
 * The script provides a `tryRun` mode to see what would be changed.
 * If everything works correctly, change `tryRun` to "false" to fully run the script.
 *
 * Execute the generated bash script after looking over it. It is located in the usual sample swap location,
 * but the path will also be printed out at the end.
 *
 * Input: See descriptions of the input variables.
 */

// --------------------------------------------------------
// input

/**
 * Multi selector using:
 * - PID
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...)
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: can be empty
 * - withdrawn comment: comment in single quotes 'withdrawn comment'
 *
 * The columns can be separated by comma, semicolon, or tab. Each value will also be trimmed.
 * A '#' indicates comments that will be ignored in the script.
 */
String multiColumnInputSample = """
#pid1,tumor,WGS,PAIRED,false,sampleName1, 'withdrawn comment'
#pid3,control,WES,PAIRED,false,, 'withdrawn comment'
#pid3,control,WES,PAIRED,false,,'long withdrawn comment
with multiple lines'
#pid5,control,RNA,SINGLE,true,sampleName2,'withdrawn comment
dfgdg
dfghsdf
'

"""

/**
 * Multi selector using:
 * - project
 * - run
 * - lane (including the barcode)
 * - well label: if it is single cell data including the file per well
 * - withdrawn comment: comment in single quotes 'withdrawn comment'
 *
 * The columns can be separated by comma, semicolon, or tab. Each value will also be trimmed.
 */
String multiColumnInputSeqTrack = """
#project1,run3,6,,'withdrawn comment'
#project3,run7,1_TTAGGC,4J01,'long withdrawn
comment'
#project2,run78,2_TTAGGC,6J01,'withdrawn comment'

"""

/**
 * List of seqTracks, one per line:
 * Multi selector using:
 * - seqTrackId
 * - withdrawn comment: comment in single quotes 'withdrawn comment'
 */
String seqTracksIds = """
#123456, 'long withdraw
comment' 
#987, 'withdrawn comment'

"""

/**
 * Name of the generated bash file.
 * The file is created in the default script directory in the withdrawn folder.
 * It is also possible to provide an absolute path.
 *
 * If the file does not end with '.sh', the file ending is added.
 */
String fileName = ''

/**
 * Revert the withdrawal of the BAM files.
 * A BAM file can only be unwithdrawn if the processing was finished, the file was not deleted,
 * and all FASTQ files it has been generated from are available (neither withdrawn nor deleted).
 */
boolean unwithdrawBamFiles = true

/**
 * Revert the withdrawal of the analysis results.
 * An analysis result can only be unwithdrawn if the processing was finished, the result folder was not deleted,
 * and all BAM files it has been generated from are available (neither withdrawn nor deleted).
 */
boolean unwithdrawAnalysis = true

/**
 * A flag to allow a trial run with a rollback of the changes at the end (if it is set to "true")
 */
boolean tryRun = true

// --------------------------------------------------------
// WORK
assert fileName?.trim(): "No file names were given"

// services
ScriptInputHelperService scriptInputHelperService = ctx.scriptInputHelperService
UnwithdrawService unwithdrawService = ctx.unwithdrawService

assert (scriptInputHelperService.checkIfExactlyOneMultiLineStringContainsContent(
        [multiColumnInputSample, multiColumnInputSeqTrack, seqTracksIds])): "Please use exactly one multiColumnInput option for the input"

// load data
List<SeqTrackWithComment> seqTracksWithComments = [
        scriptInputHelperService.seqTracksBySampleDefinition(multiColumnInputSample),
        scriptInputHelperService.seqTracksByLaneDefinition(multiColumnInputSeqTrack),
        scriptInputHelperService.seqTrackById(seqTracksIds),
].flatten()

assert seqTracksWithComments: "No seqTracks were defined"

fileName = fileName.trim()
if (!fileName.endsWith(".sh")) {
    fileName = fileName.concat(".sh")
}

UnwithdrawStateHolder unwithdrawStateHolder = new UnwithdrawStateHolder()
unwithdrawStateHolder.seqTracksWithComment = seqTracksWithComments
unwithdrawStateHolder.script = [
        "#!/bin/bash",
        "",
        "set -ev",
        "",
]
unwithdrawStateHolder.scriptFileName = fileName

final String TRIM_LINE = "----------------------------------------"

SeqTrack.withTransaction {
    if (unwithdrawStateHolder.seqTracks.any { it.project.state == Project.State.ARCHIVED }) {
        throw new FileAccessForProjectNotAllowedException("Project is archived, unwithdrawing is not allowed")
    }
    if (unwithdrawStateHolder.seqTracks.any { it.project.state == Project.State.DELETED }) {
        throw new FileAccessForProjectNotAllowedException("Project is deleted, unwithdrawing is not allowed")
    }

    unwithdrawService.unwithdrawSeqTracks(unwithdrawStateHolder)
    if (unwithdrawBamFiles) {
        unwithdrawService.unwithdrawBamFiles(unwithdrawStateHolder)
    }
    if (unwithdrawAnalysis) {
        unwithdrawService.unwithdrawAnalysis(unwithdrawStateHolder)
    }
    unwithdrawService.createBashScript(unwithdrawStateHolder)
    unwithdrawService.writeBashScript(unwithdrawStateHolder)

    println(unwithdrawStateHolder.summary.join("\n"))

    assert !tryRun: "Rollback since it was only a tryRun"
}

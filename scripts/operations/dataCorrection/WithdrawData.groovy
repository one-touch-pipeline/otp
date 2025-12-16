/*
 * Copyright 2011-2025 The OTP authors
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
import de.dkfz.tbi.otp.utils.ScriptInputHelperService
import de.dkfz.tbi.otp.withdraw.WithdrawParameters
import de.dkfz.tbi.otp.withdraw.WithdrawService

/**
 * Script to handle the withdrawing of data.
 *
 * The scripts handles the deletion and withdrawing of bam files and analysis files.
 *
 * For all file changes a bash script is created, which needs to be executed manually.
 *
 * The script does the following:
 * - RawSequenceFile:
 *   - withdraw in OTP
 *   - change the unix group of the file in the run folder
 *   - delete the link from the viewByPidFolder
 *   - delete the link in the well directory, if it exists
 * - BamFile (if deleteBamFiles = false)
 *   - withdraw the bam fle in OTP
 *   - change the unix group in the file system for the bam directory
 * - BamFile (if deleteBamFiles = true)
 *   - delete the bam file in OTP (including the analysis files)
 *   - delete the bam file on the file system (including the analysis files)
 * - Analysis (if deleteBamFiles = false and deleteAnalysis = false)
 *   - withdraw the analysis files in OTP
 *   - change the unix group in the file analysis directory recursively
 * - Analysis (if deleteBamFiles = true or deleteAnalysis = true)
 *   - delete the analysis files in in OTP
 *   - delete the analysis files on the file system
 *
 * The script provides a `tryRun` mode to see what would be changed.
 * If everything works correctly, change `tryRun` to "false" to fully run the script.
 *
 * Execute the generated bash script after looking over it. It is located in the usual sample swap location,
 * but the path will also be printed out at the end.
 *
 * Input: See the descriptions of the input variables.
 */

// --------------------------------------------------------
// input

/**
 * Indicates if the bam files should be deleted (true) or set to withdrawn (false).
 */
boolean deleteBamFile = false

/**
 * Indicates if the analysis files should be deleted (true) or set to withdrawn (false).
 * This selection is only possible, if the bam files have not already been deleted.
 */
boolean deleteAnalysis = false

// Choose exactly one of the following options for selecting the SeqTracks

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
 * Set to "true" if the withdrawing should be stopped if files do not exist on the file system (using the cached value 'sequenceFile.fileExists')
 */
boolean stopOnMissingFiles = true

/**
 * Set to "true" if the withdrawing should be stopped if the data files are already withdrawn
 */
boolean stopOnAlreadyWithdrawnData = true

/**
 * A flag to allow a trial run with a rollback of the changes at the end (if it is set to "true")
 */
boolean tryRun = true

// --------------------------------------------------------
// WORK
assert fileName?.trim(): "No file name was given"

// services
ScriptInputHelperService scriptInputHelperService = ctx.scriptInputHelperService
WithdrawService withdrawService = ctx.withdrawService

assert (
        scriptInputHelperService.checkIfExactlyOneMultiLineStringContainsContent([multiColumnInputSample, multiColumnInputSeqTrack, seqTracksIds])
): "Please use exactly one multiColumnInput option for the input"

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

WithdrawParameters withdrawParameters = new WithdrawParameters([
        seqTracksWithComments     : seqTracksWithComments,
        deleteBamFile             : deleteBamFile,
        deleteAnalysis            : deleteAnalysis,
        fileName                  : fileName,
        stopOnMissingFiles        : stopOnMissingFiles,
        stopOnAlreadyWithdrawnData: stopOnAlreadyWithdrawnData,
])

SeqTrack.withNewTransaction {
    String summary = withdrawService.withdraw(withdrawParameters)
    println summary
    assert !tryRun: "Rollback since it was only a tryRun"
}
''

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


import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ScriptInputHelperService
import de.dkfz.tbi.otp.withdraw.WithdrawParameters
import de.dkfz.tbi.otp.withdraw.WithdrawService

/**
 * Script to handle withdrawing data.
 *
 * The scripts allow to select between deletion and withdrawing of bam files and of analysis.
 *
 * For all file changes a bash script is created, which needs to be executed manually.
 *
 * The script does the following:
 * - DataFile:
 *   - withdraw in OTP
 *   - change unix group for file in run folder
 *   - delete link from viewByPidFolder
 *   - delete link in Well directory, if exist
 * - BamFile (if deleteBamFiles = false)
 *   - withdraw the bam fle in OTP
 *   - change unix group in file system for the bam directory
 * - BamFile (if deleteBamFiles = true)
 *   - delete in in OTP (including analysis)
 *   - delete on file system (including analysis)
 * - Analysis (if deleteBamFiles = false and deleteAnalysis = false)
 *   - withdraw the analysis fle in OTP
 *   - change unix group in file analysis directory recursively
 * - Analysis (if deleteBamFiles = true or deleteAnalysis = true)
 *   - delete in in OTP
 *   - delete on file system
 *
 * The script provide a tryRun mode to see, what would be changed.
 * If this is fine,change TryRun to false
 *
 * Execute the generated script after looking over it. It is located in the typical sample
 * swap location, but the path is also printed out at the end.
 *
 * Input: See description of the input variables.
 */


//--------------------------------------------------------
//input

/**
 * The text to use as withdrawn comment
 */
String withdrawnComment = """\

"""

/**
 * indicate, if the bam files should be deleted (true) or set to withdrawn (false).
 */
boolean deleteBamFile = true

/**
 * indicate, if the analysis files should be deleted (true) or set to withdrawn (false).
 * The selection is only possible, if the bam files are not deleted.
 */
boolean deleteAnalysis = true

/**
 * Multi selector using:
 * - pid
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by comma, semicolon or tab. Each value is also trimmed.
 */
String multiColumnInputSample = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2
"""

/**
 * Multi selector using:
 * - project
 * - run
 * - lane (inclusive barcode)
 * - well label: if single cell data with file per well
 *
 * The columns can be separated by comma, semicolon or tab. Each value is also trimmed.
 */
String multiColumnInputSeqTrack = """
#project1,run3,6,
#project3,run7,1_TTAGGC,4J01
#project2,run78,2_TTAGGC,ATRX,6J01

"""

/**
 * List of seqTracks, one per line:
 */
String seqTracksIds = """
#123456
#987

"""

/**
 * Name of the generated bash file. It should have the extension '.sh'
 * The file is created in the default directory script directory in the folder
 */
String fileName = ''

/**
 * Should withdrawing stop if files are not existing in file system (using cached value 'dataFile.fileExists')
 */
boolean stopOnMissingFiles = true

/**
 * Should withdrawing stop if data files are already withdrawn
 */
boolean stopOnAlreadyWithdrawnData = true

/**
 * flag to allow a try and rollback the changes at the end (true) or do the changes(false)
 */
boolean tryRun = true

//--------------------------------------------------------
// WORK

assert withdrawnComment?.trim() : "no comment were given"
assert fileName : "no file name were given"

//services
ScriptInputHelperService scriptInputHelperService = ctx.scriptInputHelperService
WithdrawService withdrawService = ctx.withdrawService

//load data
List<SeqTrack> seqTrackPerSampleDefinition = scriptInputHelperService.seqTracksBySampleDefinition(multiColumnInputSample)
List<SeqTrack> seqTrackPerLaneDefinition = scriptInputHelperService.seqTracksBySampleDefinition(multiColumnInputSeqTrack)
List<SeqTrack> seqTrackPerId = scriptInputHelperService.seqTrackById(seqTracksIds)

List<SeqTrack> allSeqTracks = [
        seqTrackPerSampleDefinition,
        seqTrackPerLaneDefinition,
        seqTrackPerId,
].flatten().unique()

assert allSeqTracks: "No seqTracks were defined"

WithdrawParameters withdrawParameters = new WithdrawParameters([
        withdrawnComment          : withdrawnComment.trim(),
        deleteBamFile             : deleteBamFile,
        deleteAnalysis            : deleteAnalysis,
        fileName                  : fileName,
        stopOnMissingFiles        : stopOnMissingFiles,
        stopOnAlreadyWithdrawnData: stopOnAlreadyWithdrawnData,
        seqTracks                 : allSeqTracks,
])

SeqTrack.withNewTransaction {
    String summary = withdrawService.withdraw(withdrawParameters)
    println summary

    assert !tryRun: "Rollback, since only tryRun."
}

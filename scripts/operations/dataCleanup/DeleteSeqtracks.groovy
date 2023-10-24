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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.*

// input area
// ----------------------

/**
 * Multi selector using:
 * - pid
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by space, comma, semicolon or tab. Multiple separators are merged together.
 */
String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
#pid5,control,RNA,SINGLE,true,sampleName2
"""

/**
 * You can also provide a comma-separated list of Ilse submission numbers to delete all the seq tracks contained therein
 */
List<Integer> ilseSubmissionNumbers = [

]

/**
 * List of seqTracks.
 *
 * The seqTracks can be seperated comma, semicolon, space, tab or newline
 */
String seqTracksIdList = """
#123456, 456,789 852;987
#987

"""

/**
 * Absolute path of the file to generate.
 */
String pathName = ''

/**
 * should be stopped, if seqTracks are only linked (property) or for the sample an external bam files exist
 */
boolean checkForExternalBamFilesOrLinkedFastqFiles = true

/**
 * flag to allow a try and rollback the changes at the end (true) or do the changes(false)
 */
boolean tryRun = true

// script area
// -----------------------------

ConfigService configService = ctx.configService
DeletionService deletionService = ctx.deletionService
FileService fileService = ctx.fileService
FileSystemService fileSystemService = ctx.fileSystemService
SeqTypeService seqTypeService = ctx.seqTypeService

FileSystem fileSystem = fileSystemService.remoteFileSystem

assert pathName: 'No file name given, but this is required'
assert !pathName.contains(' '): 'File name contains spaces, which is not allowed'

Path outputPath = fileSystem.getPath(pathName)

assert outputPath.absolute: '"The file name is not absolute, but that is required'

if (Files.exists(outputPath)) {
    Files.delete(outputPath)
}

List<SeqTrack> seqTrackPerMultiImport = multiColumnInput.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collectMany { String line ->
    List<String> values = line.split('[ ,;\t]+')*.trim()
    int valueSize = values.size()
    assert valueSize in [5, 6]: "A multi input is defined by 5 or 6 columns"
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(values[0]),
            "Could not find one individual with name ${values[0]}")
    SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByNameIlike(values[1]),
            "Could not find one sampleType with name ${values[1]}")

    SequencingReadType libraryLayout = SequencingReadType.getByName(values[3])
    assert libraryLayout: "${values[3]} is no valid sequencingReadType"
    boolean singleCell = Boolean.parseBoolean(values[4])

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[2], [
            libraryLayout: libraryLayout,
            singleCell   : singleCell,
    ])
    assert seqType: "Could not find seqType with : ${values[2]} ${values[3]} ${values[4]}"

    List<SeqTrack> seqTracks = SeqTrack.withCriteria {
        sample {
            eq('individual', individual)
            eq('sampleType', sampleType)
        }
        eq('seqType', seqType)
        if (values.size() == 6) {
            eq('sampleIdentifier', values[5])
        }
    }
    assert seqTracks: "Could not find any seqtracks for ${values.join(' ')}"
    return seqTracks
}

List<SeqTrack> ilseSeqTracks = []
if (ilseSubmissionNumbers) {
    ilseSeqTracks = SeqTrack.findAllByIlseSubmissionInList(ilseSubmissionNumbers.collect {
        CollectionUtils.exactlyOneElement(IlseSubmission.findAllByIlseNumber(it))
    })
}

List<SeqTrack> seqTrackPerId = seqTracksIdList.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collectMany { String line ->
    line.split('[ ,;\t]+').toList()
}.collect {
    CollectionUtils.exactlyOneElement(SeqTrack.findAllById(it as long))
}

Project.withTransaction {
    List<File> filesToDelete = (seqTrackPerMultiImport + ilseSeqTracks + seqTrackPerId).collect {
        println "deleting ${it}"
        deletionService.deleteSeqTrack(it, checkForExternalBamFilesOrLinkedFastqFiles)
    }.flatten().unique()

    String content = """
#!/bin/bash

set -ve
${filesToDelete.collect { "rm -rf ${it}" }.join('\n')}

"""
    fileService.createFileWithContent(outputPath, content)

    println """
deleted in OTP

please delete it now one the files system.
The script is writen:\n${outputPath}

"""

    assert !tryRun: "Rollback, since only tryRun."
}

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
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.DeletionService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.FileSystem
import java.nio.file.Path

// input area
// ----------------------

/**
 * Name of the project for which all processed files should be deleted.
 *
 * Leave empty when using multiColumnInput to select specific samples.
 */
String projectName = ""

/**
 * Multi selector using:
 * - PID
 * - sample type
 * - seqType name or alias (for example WGS, WES, RNA, ...)
 * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
 * - single cell flag: true = single cell, false = bulk
 * - sampleName: optional
 *
 * The columns can be separated by space, comma, semicolon, or tab.
 * Multiple separators are merged together.
 *
 * All input has to be from a single project!
 *
 * Leave empty when the whole project given by projectName should be processed.
 */

String multiColumnInput = """
#pid1,tumor,WGS,PAIRED,false,sampleName1
#pid3,control,WES,PAIRED,false,
"""

/**
 * Absolute path of the output deletion script.
 */
String scriptPathName = ""

/** false: delete alignment + analysis
/*  true:  delete analysis only
 */
boolean deleteAnalysisOnly = false

/**
 * Flag to perform a trial run, which rolls back the changes at the end (if set to `true`),
 * or execute the changes (if set to `false`).
 */
boolean tryRun = true

// script area
// -----------------------------

DeletionService deletionService = ctx.deletionService
FileSystemService fileSystemService = ctx.fileSystemService
SeqTypeService seqTypeService = ctx.seqTypeService

FileSystem fileSystem = fileSystemService.remoteFileSystem

boolean hasMultiColumnInput = multiColumnInput.split('\n')*.trim().any { String line ->
    line && !line.startsWith('#')
}

assert projectName || hasMultiColumnInput:
        "Either a projectName or a multiColumnInput has to be given"

assert !(projectName && hasMultiColumnInput):
        "projectName and multiColumnInput must not be given at the same time"

assert scriptPathName?.trim():
        "scriptPathName has to be given"

Path scriptPath = fileSystem.getPath(scriptPathName)

assert scriptPath.isAbsolute():
        "scriptPathName has to be an absolute path"

List<SeqTrack> seqTracks = multiColumnInput.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collectMany { String line ->
    List<String> values = line.split('[ ,;\t]+')*.trim()
    int valueSize = values.size()

    assert valueSize in [5, 6]:
            "A multi input is defined by 5 or 6 columns"

    Individual individual = CollectionUtils.exactlyOneElement(
            Individual.findAllByPid(values[0]),
            "Could not find an individual with the name ${values[0]}"
    )

    SampleType sampleType = CollectionUtils.exactlyOneElement(
            SampleType.findAllByNameIlike(values[1]),
            "Could not find a sampleType with the name ${values[1]}"
    )

    SequencingReadType sequencingReadType = SequencingReadType.getByName(values[3])
    assert sequencingReadType:
            "${values[3]} is not a valid sequencingReadType"

    String singleCellValue = values[4].toLowerCase()

    assert singleCellValue in ['true', 'false', 't', 'f']:
            "${values[4]} is not a valid single cell flag"

    boolean singleCell = singleCellValue in ['true', 't']

    SeqType seqType = seqTypeService.findByNameOrImportAlias(values[2], [
            libraryLayout: sequencingReadType,
            singleCell   : singleCell,
    ])

    assert seqType:
            "Could not find a seqType with: '${values[2]}', sequencingReadType: '${values[3]}', singleCellValue: '${values[4]}'"

    List<SeqTrack> foundSeqTracks = SeqTrack.withCriteria {
        sample {
            eq('individual', individual)
            eq('sampleType', sampleType)
        }
        eq('seqType', seqType)

        if (values.size() == 6) {
            eq('sampleIdentifier', values[5])
        }
    }

    assert foundSeqTracks:
            "Could not find any seqtracks for ${values.join(' ')}"

    return foundSeqTracks
}.unique()

Project project

if (seqTracks) {
    Set<Project> projects = seqTracks*.project as Set<Project>

    assert projects.size() == 1:
            "All SeqTracks from the multiColumnInput have to belong to the same project"

    project = projects.first()

    println "Affected SeqTracks:"
    println "  Project: ${project.name}"

    seqTracks.groupBy { it.individual }.each {
        Individual individual, List<SeqTrack> individualSeqTracks ->

            println "    - ${individual}"

            individualSeqTracks.each { SeqTrack seqTrack ->
                println "      * ${seqTrack}"
            }
    }
} else {
    project = CollectionUtils.exactlyOneElement(
            Project.findAllByName(projectName),
            "Could not find a project with the name ${projectName}"
    )

    seqTracksByProject = [(project): []]

    println """\
    |#########################################################################
    |# No restriction on specific SeqTracks, entire project will be removed! #
    |# Project: ${project.name}
    |#########################################################################
    |""".stripMargin()
}

Project.withTransaction {
    deletionService.deleteProcessingFilesOfProject(
            project.name,
            scriptPath.parent,
            false,
            false,
            seqTracks,
            deleteAnalysisOnly,
            scriptPath.fileName.toString()
    )

    assert !tryRun: "Rollback, since it was only a tryRun."
}

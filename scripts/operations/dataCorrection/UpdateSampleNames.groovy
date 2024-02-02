/*
 * Copyright 2011-2024 The OTP authors
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

/**
 * A script to update the sampleNames in the SeqTracks.
 * That is sometimes requested after a data swap.
 * The script does not update the MetaDataEntries.
 *
 * It shows warnings if:
 * - the old SampleName could not be found
 * - the old SampleName found multiple times
 * - the new SampleName already exists
 *
 * Per default, it is in the preview mode, which shows only the message.
 * Change @param preView to false to do the actual update.
 */

import de.dkfz.tbi.otp.ngsdata.SeqTrack

// =============================================
// input area

// The map of old sample name to the new one.
// Two columns, separated by space, tab, comma or semicolon
String mapping = """
#oldSampleName1, newSampleName1
#oldSampleName2, newSampleName2

"""

// flag to indicate whether to show the changes without executing them (true), or show and apply the changes (false).
boolean preView = true

// =============================================
// work area

List<String> oldSampleNameNotFound = []
List<String> oldSampleNameMultipleFound = []
List<String> newSampleNameFound = []
List<String> changes = []

List<Map> workList = mapping.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect { String line ->
    List<String> split = line.split(/ *[,; \t] */)
    assert split.size() == 2: "The line '${split}' consist of ${split.size()} elements instead of two"
    String oldSampleName = split[0]
    String newSampleName = split[1]

    List<SeqTrack> seqTracks = SeqTrack.findAllBySampleIdentifier(oldSampleName)
    if (!seqTracks) {
        oldSampleNameNotFound << oldSampleName
        return [:]
    } else if (seqTracks.size() > 1) {
        oldSampleNameMultipleFound << "${oldSampleName}: ${seqTracks.size()}"
    }
    List<SeqTrack> seqTracksForNewSampleName = SeqTrack.findAllBySampleIdentifier(newSampleName)
    if (seqTracksForNewSampleName.size()) {
        newSampleNameFound << "${newSampleName}: ${seqTracksForNewSampleName.size()}"
    }

    changes << "${oldSampleName} --> ${newSampleName}"

    [
            seqTracks    : seqTracks,
            newSampleName: newSampleName,
    ]
}.findAll()

if (oldSampleNameNotFound) {
    println "\nThe following old SampleNames could not be found:\n${oldSampleNameNotFound.join('\n')}"
}

if (oldSampleNameMultipleFound) {
    println "\nFor the following old SampleNames multiple lanes could be found:\n${oldSampleNameMultipleFound.join('\n')}"
}

if (newSampleNameFound) {
    println "\nFor the following new SampleNames already lanes could be found:\n${newSampleNameFound.join('\n')}"
}

println "\n\nThe following changes will be done:\n${changes.join('\n')}"

if (preView) {
    println "\n\nStop, since preview mode is active. Nothing has changed. Change flag preView to false to do the changes"
} else {
    SeqTrack.withTransaction {
        workList.each { Map map->
            List<SeqTrack> seqTracks = map.seqTracks
            String newSampleName= map.newSampleName
            seqTracks.each {SeqTrack seqTrack->
                seqTrack.sampleIdentifier = newSampleName
                seqTrack.save(flush: true)
            }
        }
    }
    println "\n\nSample names of all effected lanes have been changed"
}

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

import de.dkfz.tbi.otp.ngsdata.*

// =============================================
// input area

// The map of seqTrack id, old sample name and the new one.
// Two columns, separated by space, tab, comma or semicolon
String mapping = """
#seqTrackID1,oldSampleName1,newSampleName1
#seqTrackID2,oldSampleName2,newSampleName2

"""

// flag to indicate whether to show the changes without executing them (true), or show and apply the changes (false).
boolean preView = true

// =============================================
// work area

List<String> seqTrackIdNotFound = []
List<String> seqTrackAndSampleIdentifierDontMatch = []
List<String> newSampleNameFound = []
List<String> changes = []

List<Map> workList = mapping.split('\n')*.trim().findAll { String line ->
    line && !line.startsWith('#')
}.collect { String line ->
    List<String> split = line.split(/ *[,; \t] */)
    assert split.size() == 3: "The line '${split}' consist of ${split.size()} elements instead of three"
    String id = split[0]
    String oldSampleName = split[1]
    String newSampleName = split[2]

    SeqTrack seqTrack = SeqTrack.get(id)
    if (!seqTrack) {
        seqTrackIdNotFound << id
        return [:]
    } else if (seqTrack.sampleIdentifier != oldSampleName) {
        seqTrackAndSampleIdentifierDontMatch << "${id}: ${oldSampleName}"
        return [:]
    }
    List<SeqTrack> seqTracksForNewSampleName = SeqTrack.findAllBySampleIdentifier(newSampleName)
    if (seqTracksForNewSampleName.size()) {
        newSampleNameFound << "${newSampleName}: ${seqTracksForNewSampleName.size()}"
    }

    changes << "${id}: ${oldSampleName} --> ${newSampleName}"

    return [
            seqTrack     : seqTrack,
            newSampleName: newSampleName,
    ]
}.findAll()

if (seqTrackIdNotFound) {
    println "\nThe following seqtrack ids could not be found and will be ignored:\n${seqTrackIdNotFound.join('\n')}"
}

if (seqTrackAndSampleIdentifierDontMatch) {
    println "\nThe following seqtrack ids and old SampleNames do not match and will be ignored:\n${seqTrackAndSampleIdentifierDontMatch.join('\n')}"
}

if (newSampleNameFound) {
    println "\nThe following new SampleNames already exist:\n${newSampleNameFound.join('\n')}"
}

println "\n\nThe following changes will be done:\n${changes.join('\n')}"

if (preView) {
    println "\n\nStop, since preview mode is active. Nothing has changed. Change flag preView to false to do the changes"
} else {
    SeqTrack.withTransaction {
        workList.each { Map map ->
            SeqTrack seqTrack = map.seqTrack
            String newSampleName= map.newSampleName
            seqTrack.sampleIdentifier = newSampleName
            seqTrack.save(flush: true)
        }
    }
    println "\n\nSample names of all effected lanes have been changed"
}

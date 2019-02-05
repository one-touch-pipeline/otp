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

/**
 *  Delete a Run
 *
 *  Delete a Run including:
 *  - RunSegment
 *  - DataFiles
 *  - MetaDataValues
 *  - MetaDataFiles
 *
 *  Other object like SeqTracks or AlignmentLogs or MergingLogs or further processing objects are not deleted.
 *
 *  Therefore it shouldn't be called after buildSequenceTracks of the load meta data workflow has run for any of its run segments.
 */

import de.dkfz.tbi.otp.ngsdata.*

List <String> output = []

String runName = "Name of a run"

Run.withTransaction {
    Run run = Run.findByName(runName)
    List<DataFile> files = DataFile.findAllByRun(run)
    files.each {
        assert !it.seqTrack : "SeqTrack ${it.seqTrack} loaded for ${it}"
    }
    files.each { DataFile file ->
        output << file
        List<MetaDataEntry> entries = MetaDataEntry.findAllByDataFile(file)
        entries.each { MetaDataEntry entry ->
            output << "    " + entry
            entry.delete(flush: true)
        }
        output << "    Are all MetaDataEntries deleted? " + (MetaDataEntry.countByDataFile(file) == 0 ? "true" : "false")
        file.delete(flush: true)
    }
    output << "  Are all DataFiles deleted? " + (DataFile.countByRun(run) == 0 ? "true" : "false")
    output << ""
    List<RunSegment> segments = RunSegment.findAllByRun(run)
    segments.each { RunSegment segment ->
        output << "  " + segment
        List<MetaDataFile> metaDataFiles = MetaDataFile.findAllByRunSegment(segment)
        metaDataFiles.each { MetaDataFile metaDataFile ->
            output << "    " << metaDataFile
            metaDataFile.delete(flush: true)
        }
        output << "    Are all MetaDataFiles deleted? " + (MetaDataFile.countByRunSegment(segment) == 0 ? "true" : "false")
        segment.delete(flush: true)
    }
    output << "  Are all RunSegments deleted? " + (RunSegment.countByRun(run) == 0 ? "true" : "false")
    run.delete(flush: true)
    output << "Is the Run deleted? " + (Run.findByName(runName) == null ? "true" : "false")
    it.setRollbackOnly()
}
println output.join('\n')
''

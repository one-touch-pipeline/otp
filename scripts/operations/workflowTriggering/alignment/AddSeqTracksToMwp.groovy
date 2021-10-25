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
package operations.workflowTriggering.alignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

// Input

// Select Sample:
String pid = ""
String sampleTypeName = ""

List<SeqTrack> seqTracksToInclude = SeqTrack.getAll([])
MergingWorkPackage mwpToAddTo = null //MergingWorkPackage.get()


// Work
Sample sample = Sample.createCriteria().get {
    individual {
        eq("pid", pid)
    }
    sampleType {
        eq("name", sampleTypeName)
    }
}

assert sample: "Could not find any Sample for  ${pid}  ${sampleTypeName}"

println "MWPs of Sample, with SeqPlatformGroup, contained Lanes and associated BAMs:"
MergingWorkPackage.withCriteria {
    eq("sample", sample)
}.each { MergingWorkPackage mwp ->
    println """\
    |${mwp}
    |  SeqPlatformGroup:
    |  ${mwp.seqPlatformGroup}
    |
    |  Lanes:
    |${mwp.seqTracks.collect { SeqTrack seqTracks -> "    - ${seqTracks}" }.join("\n")}
    |
    |  BAMs:
    |${RoddyBamFile.findAllByWorkPackage(mwp).collect { RoddyBamFile rbf -> return "    - ${rbf}" }.join("\n")}
    |
    |""".stripMargin()
}

println "All SeqTracks of ${sample}:"
SeqTrack.findAllBySample(sample).each {
    println "  - ${it}"
}

println "--------"

println "Following SeqTracks will be added:"
seqTracksToInclude.each {
    println "  - ${it}"
}

SeqTrack.withTransaction {
    assert mwpToAddTo: "Select a merging workPackage to which to add the SeqTracks"

    seqTracksToInclude.each { SeqTrack seqTrack ->
        mwpToAddTo.addToSeqTracks(seqTrack)
    }
    mwpToAddTo.save(flush: true)

    assert false: "rollback, remove this to persist changes"
}

println """\
--------
following this, retrigger the already existing BAM with `RetriggerRoddyAlignment.groovy`"
"""

''

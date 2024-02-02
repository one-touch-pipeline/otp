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

import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Script checks if the given sample names are known to OTP.
 *
 * SampleIdentifiers can exist either as 'raw' SampleIdentifier, meant as a preparation for import,
 * or as label on a SeqTrack.
 * This script will find both types, and let you know which it was.
 *
 * All input lines are trimmed of whitespace.
 * Empty lines and comments (starting with '#') are skipped.
 */

// -------------------------------
// work area
String input = """
#sample id 1
#sample id 2

"""

// -------------------------------
// work area

input.split('\n')*.trim().findAll {
    it && !it.startsWith('#')
}.each {
    SampleIdentifier sampleIdentifier = CollectionUtils.atMostOneElement(SampleIdentifier.findAllByName(it))
    List<SeqTrack> seqTracks = SeqTrack.findAllBySampleIdentifier(it)

    if (sampleIdentifier) {
        println "found SampleIdentifier ${sampleIdentifier}: ${sampleIdentifier.sample} (${sampleIdentifier.project})"
    }
    if (seqTracks) {
        println "found SeqTracks: ${seqTracks.size()} SeqTrack(s): ${seqTracks*.sample.unique()} (in project(s) ${seqTracks*.project})"
    }
    if (!sampleIdentifier && ! seqTracks) {
        println "unknown: ${it}"
    }
}

''

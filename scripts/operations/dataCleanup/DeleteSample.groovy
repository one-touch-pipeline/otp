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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

/*
Sample name input
The following sample name shall be deleted in case no data is connected ("identifier1", "identifier2", "identifier3", ...).
The connected sample shall be deleted
 */
def sampleIdentifierToDelete = []
def deleteConnectedSample = false

/*
Sample input
The following sample shall be deleted in case no data is connected ("pid1#sampleType1", "pid2#sampleType2", ...).
 */
def sampleToDelete = []

def deleteSample(Sample sample) {
    if (!SampleIdentifier.findBySample(sample) && !SeqTrack.findBySample(sample)) {
        sample.delete(flush: true)
        println "The following sample was deleted: ${sample}"
    } else {
        println "The sample ${sample} can not be deleted since either sampleIdentifier or data is connected"
    }
}

//script
SampleIdentifier.withTransaction {
    assert (sampleIdentifierToDelete || sampleToDelete) : "Please provide sample name or sample to delete"

    def notFoundSampleIdentifier = []
    def samplesToDelete = []
    sampleIdentifierToDelete.each { sitd ->
        def si = SampleIdentifier.findByName(sitd)
        if (!si) {
            notFoundSampleIdentifier << sitd
        } else {
            samplesToDelete << si.sample
            si.delete(flush: true)
            println "The following sample name was deleted: ${si}"
        }
    }

    if (!notFoundSampleIdentifier) {
        if (deleteConnectedSample) {
            samplesToDelete.each { s ->
                deleteSample(s)
            }
        }
    }

    if (notFoundSampleIdentifier) {
        println "The following sample name could not be found in OTP: "
    }
    notFoundSampleIdentifier.each {
        println it
    }

    sampleToDelete.each { std ->
        def s = CollectionUtils.exactlyOneElement(Sample.createCriteria().list {
            sampleType {
                eq("name", std.split("#")[1])
            }
            individual {
                eq("pid", std.split("#")[0])
            }
        })
        deleteSample(s)
    }

    assert false: "DEBUG mode"
}

''

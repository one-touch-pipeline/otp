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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * Script show for one individual, sampleType and seqType, which thresholds are failing
 */

String pid = ""
String sampleTypeName = ""
String seqTypeName = ""
String libraryLayout = SequencingReadType.PAIRED

//-----------------

def bamFile = CollectionUtils.exactlyOneElement(AbstractMergedBamFile.createCriteria().list {
    workPackage {
        sample {
            individual {
                eq('pid', pid)
            }
            sampleType {
                eq('name', sampleTypeName)
            }
        }
        seqType {
            eq('name', seqTypeName)
            eq('libraryLayout', libraryLayout)
        }
    }
}.findAll {
    it.isMostRecentBamFile()
})
println bamFile

def qa = CollectionUtils.exactlyOneElement(RoddyQualityAssessment.createCriteria().list {
    qualityAssessmentMergedPass {
        eq('abstractMergedBamFile', bamFile)
    }
    eq('chromosome', 'all')
}.findAll {
    (it instanceof RoddyMergedBamQa) || it.roddyBamFile.seqType.isRna()
})
println qa

println "\n\nfailed thresholds"
QcThreshold.getValidQcPropertyForQcClass(qa.class.name).findAll { String property ->
    QcThreshold qcThreshold =
            CollectionUtils.atMostOneElement(QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProject(qa.class.name, bamFile.seqType, property, bamFile.project)) ?:
                    CollectionUtils.atMostOneElement(QcThreshold.findAllByQcClassAndSeqTypeAndQcProperty1AndProjectIsNull(qa.class.name, bamFile.seqType, property))
    return qcThreshold?.qcPassed(qa) == QcThreshold.ThresholdLevel.ERROR
}.each {
    println "    ${it} = ${qa[it]}"
}

''

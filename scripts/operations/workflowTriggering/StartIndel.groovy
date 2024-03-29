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
package operations.startWorkflow

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.indelCalling.RoddyIndelCallingStartJob
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils

// sample pair defined by:
// PID SAMPLETYPE1 SAMPLETYPE2 SEQTYPE_NAME
// the sample pair has to be created already by OTP, the SAMPLETYPE1 has to be a disease sample type, the SAMPLETYPE2 is usually control
List<SamplePair> samplePairs = """

""".split('\n').findAll().collect {
    println "check: '${it}'"
    String[] split = it.split(' ')
    println split as List
    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType1 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[1]))
    SampleType sampleType2 = CollectionUtils.exactlyOneElement(SampleType.findAllByName(split[2]))
    SeqType seqType = CollectionUtils.exactlyOneElement(SeqType.findAllByNameAndLibraryLayout(split[3], SequencingReadType.PAIRED))
    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))
    MergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    MergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))
    SamplePair samplePair = CollectionUtils.exactlyOneElement(SamplePair.findAllByMergingWorkPackage1AndMergingWorkPackage2(mergingWorkPackage1, mergingWorkPackage2))
    return samplePair
}

RoddyIndelCallingStartJob roddyIndelCallingStartJob = ctx.roddyIndelStartJob

SamplePair.withTransaction {
    samplePairs.each { SamplePair samplePair ->
        println "trigger: '${it}'"

        ConfigPerProjectAndSeqType config = roddyIndelCallingStartJob.getConfig(samplePair)

        AbstractBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
        AbstractBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

        BamFilePairAnalysis analysis = new IndelCallingInstance(
                samplePair: samplePair,
                instanceName: roddyIndelCallingStartJob.getInstanceName(config),
                config: config,
                sampleType1BamFile: sampleType1BamFile,
                sampleType2BamFile: sampleType2BamFile,
        )
        analysis.save(flush: true)
        roddyIndelCallingStartJob.prepareCreatingTheProcessAndTriggerTracking(analysis)
        roddyIndelCallingStartJob.createProcess(analysis)
        log.debug "Analysis started for: ${analysis.toString()}"
    }
}

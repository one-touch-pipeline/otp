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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Script to update reference genome for one MergingWorkPackage
 */

String referenceGenomeName = '' // Name of the new Reference Genome
String statSizeFileName = '' // Name of the stat size file name (Only for PANCAN, depends on referenceGenomeName)
String pid = ''
String sampleTypeName = ''
SeqType seqType

// seqType = SeqTypeService.exomePairedSeqType
// seqType = SeqTypeService.wholeGenomePairedSeqType
// seqType = SeqTypeService.wholeGenomeBisulfitePairedSeqType
// seqType = SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
// seqType = SeqTypeService.rnaPairedSeqType
// seqType = SeqTypeService.chipSeqPairedSeqType

// --------------------------

assert referenceGenomeName
assert pid
assert sampleTypeName
assert seqType

ReferenceGenome referenceGenome = CollectionUtils.exactlyOneElement(ReferenceGenome.findAllByName(referenceGenomeName))

Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(pid))
SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByName(sampleTypeName))
Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType))
MergingWorkPackage mergingWorkPackage = CollectionUtils.exactlyOneElement(MergingWorkPackage.findAllBySampleAndSeqType(sample, seqType))

MergingWorkPackage.withTransaction {
    mergingWorkPackage.referenceGenome = referenceGenome
    mergingWorkPackage.statSizeFileName = statSizeFileName
    mergingWorkPackage.save(flush: true)

    assert false
}

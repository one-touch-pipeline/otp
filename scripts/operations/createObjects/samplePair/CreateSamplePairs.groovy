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
 * Create sample pairs for merging work package given by
 * - pid
 * - first sampleType (use as disease)
 * - second sampleType (use as control)
 * - seqType name
 *
 * Notes:
 * - A mergingWorkPackage has to exist (--> produced bam file)
 * - the analyses only support the seqTypes WES and WGS
 * - The libraryLayout is always paired.
 * - Only bulk seq types are supported.
 * - If a sample pair already exists, it will not be created. Thus the processing status is not reset and no analyses will run.
 */

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

//----------------------------------------
//input area

//Create manual SamplePairs defined by pid, sampleType1, sampleType2 and seqType:
//PID SAMPLETYPE1 SAMPLETYPE2 SEQTYPE_NAME
String input = """

"""

//----------------------------------------
//work area
SamplePairDeciderService samplePairDeciderService = ctx.samplePairDeciderService
SeqTypeService seqTypeService = ctx.seqTypeService

List<List<MergingWorkPackage>> samplePairs = input.split('\n').findAll().collect {
    String[] split = it.split()
    println "parsed '${it}' into ${split as List}"

    Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(split[0]))
    SampleType sampleType1 = SampleTypeService.findSampleTypeByName(split[1])
    SampleType sampleType2 = SampleTypeService.findSampleTypeByName(split[2])
    SeqType seqType = seqTypeService.findByNameOrImportAlias(split[3], [libraryLayout: SequencingReadType.PAIRED, singleCell: false])
    Sample sample1 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType1))
    Sample sample2 = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType2))

    // find the merging work packages; use AbstractMWP so it also works with imported bamfiles (ExternalMWP)
    AbstractMergingWorkPackage mergingWorkPackage1 = CollectionUtils.exactlyOneElement(AbstractMergingWorkPackage.findAllBySampleAndSeqType(sample1, seqType))
    AbstractMergingWorkPackage mergingWorkPackage2 = CollectionUtils.exactlyOneElement(AbstractMergingWorkPackage.findAllBySampleAndSeqType(sample2, seqType))
    return [mergingWorkPackage1, mergingWorkPackage2]
}

Individual.withTransaction {
    samplePairs.each {
        samplePairDeciderService.findOrCreateSamplePair(it[0], it[1])
    }
    assert false: "DEBUG: intentionally fail transaction"
}
''

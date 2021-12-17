
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

import de.dkfz.tbi.otp.ngsdata.*

String projectName = ''

//SeqType seqType = SeqTypeService.exomePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomeBisulfitePairedSeqType
//SeqType seqType = SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
//SeqType seqType = SeqTypeService.rnaPairedSeqType
//SeqType seqType = SeqTypeService.rnaSingleSeqType
//SeqType seqType = SeqTypeService.chipSeqPairedSeqType

//---------------------------

List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
    sample {
        individual {
            project {
                eq('name', projectName)
            }
        }
    }
    eq('seqType', seqType)
}

println "found samples for ${projectName} ${seqType}"
println seqTracks.collect {
    "${it.individual.pid} ${it.sampleType.name} ${it.libraryPreparationKit?.name}"
}.unique().sort().join('\n')

println "\n-----------------------------"
println "lanes without libraryPreparationKit\n"
println seqTracks.findAll {
    it.libraryPreparationKit == null
}.collect {
    "${it.individual.pid} ${it.sampleType.name} ${it.run.name} ${it.laneId}"
}.unique().sort().join('\n')

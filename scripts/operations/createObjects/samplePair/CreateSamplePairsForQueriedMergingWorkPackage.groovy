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
 * Creates all possible disease control sample pairs for the selected AbstractMergingWorkPackages,
 * including ExternalMergingWorkPackages with all possible other AbstractMergingWorkPackages.
 *
 */

import de.dkfz.tbi.otp.dataprocessing.AbstractMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTypeService

//----------------------------------------
//input area

Collection<AbstractMergingWorkPackage> mergingWorkPackages = AbstractMergingWorkPackage.withCriteria {
    sample {
        individual {
            or {
                'in'('pid', [
                        '',
                ])
                project {
                    'in'('name', [
                            '',
                    ])
                }
            }
        }
    }
    'in'('seqType', [
            SeqTypeService.wholeGenomePairedSeqType,
            SeqTypeService.exomePairedSeqType,
    ])
}

//----------------------------------------
//selection display

println "        Found Merging workPackage"
println mergingWorkPackages*.toString().sort().join('\n')

//----------------------------------------
//work area
SamplePairDeciderService samplePairDeciderService = ctx.samplePairDeciderService

SeqTrack.withTransaction {
    println "\n\n        finds or creates following sample pairs:"
    println samplePairDeciderService.findOrCreateSamplePairs(mergingWorkPackages)*.toString().sort().join('\n')

    assert false: "DEBUG: transaction intentionally failed to rollback changes"
}

''

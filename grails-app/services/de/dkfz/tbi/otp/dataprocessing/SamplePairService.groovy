/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*

@Transactional
class SamplePairService {

    /**
     * Returns all the SamplePairs given by individual, sampleType and seqType.
     * Only individual is required, others are optional. Missing parameters or null values means without these condition/constrains.
     * For example: if sampleType is null or missing, then all sampleTypes are taken into account.
     * The same is true with seqType.
     *
     * @param individual required
     * @param sampleType all sample types if missing
     * @param seqType all seq types if missing
     * @return all the SamplePairs
     */
    List<SamplePair> findAllByIndividualSampleTypeSeqType(Individual individual, SampleType sampleType = null, SeqType seqType = null) {
        return SamplePair.withCriteria {
            or {
                mergingWorkPackage1 {
                    sample {
                        eq('individual', individual)
                        if (sampleType) {
                            eq('sampleType', sampleType)
                        }
                    }
                    if (seqType) {
                        eq("seqType", seqType)
                    }
                }
                mergingWorkPackage2 {
                    sample {
                        eq('individual', individual)
                        if (sampleType) {
                            eq('sampleType', sampleType)
                        }
                    }
                    if (seqType) {
                        eq("seqType", seqType)
                    }
                }
            }
        }
    }
}

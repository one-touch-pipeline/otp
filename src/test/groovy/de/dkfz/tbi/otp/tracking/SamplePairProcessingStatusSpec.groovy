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
package de.dkfz.tbi.otp.tracking

import spock.lang.Specification
import spock.lang.Unroll

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus.*

class SamplePairProcessingStatusSpec extends Specification {

    @Unroll
    void "getVariantCallingProcessingStatus"() {
        given:
        SamplePairProcessingStatus status = new SamplePairProcessingStatus(null, values.snv, null, values.indel, null,
                values.sophia, null, values.aceseq, null, values.runYapsa, null)
        List<ProcessingStatus.WorkflowProcessingStatus> inputStatuses = [values.snv, values.indel, values.sophia, values.aceseq, values.runYapsa]
        ProcessingStatus.WorkflowProcessingStatus result = NotificationCreator.combineStatuses(inputStatuses, Closure.IDENTITY)

        expect:
        result == status.variantCallingProcessingStatus

        where:
        // generates a Permutation List for indel, snv, aceseq and runYapsa.
        values << {
            List ret = []
            List list = [NOTHING_DONE_WONT_DO, NOTHING_DONE_MIGHT_DO, PARTLY_DONE_WONT_DO_MORE, PARTLY_DONE_MIGHT_DO_MORE, ALL_DONE]

            list.each { snv ->
                list.each { indel ->
                    list.each { aceseq ->
                        list.each { sophia ->
                            list.each { runYapsa ->
                                ret << [
                                        snv     : snv,
                                        indel   : indel,
                                        sophia  : sophia,
                                        aceseq  : aceseq,
                                        runYapsa: runYapsa,
                                ]
                            }
                        }
                    }
                }
            }
            ret
        }.call()
    }
}

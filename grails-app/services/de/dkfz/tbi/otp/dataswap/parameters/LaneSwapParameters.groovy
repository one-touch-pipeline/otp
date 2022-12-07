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
package de.dkfz.tbi.otp.dataswap.parameters

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataswap.Swap

@TupleConstructor
class LaneSwapParameters extends DataSwapParameters {

    static Closure constraints = {
        sampleTypeSwap nullable: false, validator: {
            if (!it.old || !it.new) {
                return "neither the old nor the new sampleType name may be null or blank"
            }
        }
        seqTypeSwap nullable: false, validator: {
            if (!it.old || !it.new) {
                return "neither the old nor the new seqType name may be null or blank"
            }
        }
        singleCellSwap nullable: false, validator: {
            if ((it.old == null) || (it.new == null)) {
                return "neither the old nor the new singleCell may be null"
            }
        }
        sequencingReadTypeSwap nullable: false, validator: {
            if (!it.old || !it.new) {
                return "neither the old nor the new sequencingReadType name may be null or blank"
            }
        }
        wellLabelSwap nullable: true, validator: { val, obj ->
            if (val) {
                if (!val.old && val.new || val.old && !val.new) {
                    return "both old and new well labels should have values or both not"
                }
                if (obj.lanes.size() != 1) {
                    return "exactly one single lane must be specified if its well label should be swapped, but ${obj.lanes.size()} lanes are given"
                }
            }
        }
        runName nullable: false, blank: false
        lanes nullable: false, minSize: 1
    }

    Swap<String> sampleTypeSwap
    Swap<String> seqTypeSwap
    Swap<Boolean> singleCellSwap
    Swap<String> sequencingReadTypeSwap
    Swap<String> wellLabelSwap
    String runName
    List<String> lanes
    boolean sampleNeedsToBeCreated = false
}

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
package de.dkfz.tbi.otp.dataswap.data

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.IndividualSwapParameters
import de.dkfz.tbi.otp.ngsdata.Sample

@TupleConstructor
class IndividualSwapData extends DataSwapData<IndividualSwapParameters> {
    static Closure constraints = {
        individualSwap nullable: false, validator: { individualSwap, obj ->
            if (obj.pidSwap.old != obj.pidSwap.new) {
                if (individualSwap.new) {
                    return "pid ${individualSwap.new.pid} cannot be used for renaming, as it already exists"
                }
            }
        }
        samples nullable: false, minSize: 1, validator: { samples, data ->
            if (samples.size() != data.sampleTypeSwaps.size()) {
                return "Given Sample map different in size than found samples!"
            }
            List<String> sampleTypesGiven = data.sampleTypeSwaps*.old.sort()
            List<String> sampleTypesFound = samples*.sampleType*.name.sort()
            List<String> difference = sampleTypesGiven - sampleTypesFound
            if (sampleTypesGiven != sampleTypesFound) {
                return "DataFiles: ${difference} not found in database, and ${sampleTypesFound} were missed in map"
            }
        }
    }

    List<Sample> samples

    List<Swap<String>> getSampleTypeSwaps() {
        return parameters.sampleTypeSwaps
    }
}

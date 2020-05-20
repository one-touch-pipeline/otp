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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.SessionUtils

class SampleIdentifier implements Entity {

    String name
    Sample sample

    static belongsTo = [
            sample: Sample,
    ]

    static constraints = {
        name unique: true, nullable: false, blank: false, minSize: 3, validator: { String val, SampleIdentifier obj ->
            //should neither start nor end with a space
            if (val.startsWith(' ') || val.endsWith(' ')) {
                return 'untrimmed'
            }
            String regexFromProcessingOption
            // Using a new session prevents Hibernate from trying to auto-flush this object, which would fail
            // because it is still in validation.
            SessionUtils.withNewSession { session ->
                regexFromProcessingOption = ProcessingOptionService.findOptionSafe(OptionName.VALIDATOR_SAMPLE_IDENTIFIER_REGEX, null, obj.sample?.project)
            }
            if (!(val ==~ (regexFromProcessingOption ?: '.+'))) {
                return 'unmatching'
            }
        }
    }

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    @Override
    String toString() {
        name
    }

    static mapping = {
        sample index: "sample_identifier_sample_idx"
    }
}

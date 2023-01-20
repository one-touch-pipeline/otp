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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.INDIVIDUAL
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SAMPLE_TYPE

@CompileDynamic
@Component
class SampleTypeIndividualValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The sample as combination of individual and sample type is registered in OTP.']
    }

    @Override
    List<String> getRequiredColumnTitles(BamMetadataValidationContext context) {
        return [INDIVIDUAL, SAMPLE_TYPE]*.name()
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String individual = it.getValue(INDIVIDUAL.name())
            String sampleType = it.getValue(SAMPLE_TYPE.name())
            if (!Sample.findAllByIndividualAndSampleType(
                    CollectionUtils.atMostOneElement(Individual.findAllByPid(individual)), SampleTypeService.findSampleTypeByName(sampleType))) {
                context.addProblem(it.cells, LogLevel.ERROR,
                        "The sample as combination of the individual '${individual}' and the sample type '${sampleType}' is not registered in OTP.", "At least one sample as combination of the individual and the sample type is not registered in OTP.")
            }
        }
    }
}

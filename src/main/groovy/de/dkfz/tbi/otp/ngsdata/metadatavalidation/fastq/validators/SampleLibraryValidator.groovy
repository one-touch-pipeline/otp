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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CUSTOMER_LIBRARY
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME

@Component
class SampleLibraryValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    final String LIB = "lib"

    @Override
    Collection<String> getDescriptions() {
        return ["If '${LIB}' is contained in the '${SAMPLE_NAME}' then the tagementation library number (CUSTOMER LIBRARY) should be given."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SAMPLE_NAME]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [CUSTOMER_LIBRARY]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String sample = valueTuple.getValue(SAMPLE_NAME.name())
            if (sample.toLowerCase(Locale.ENGLISH).contains(LIB) && !valueTuple.getValue(CUSTOMER_LIBRARY.name())) {
                context.addProblem(valueTuple.cells, Level.WARNING, "For sample '${sample}' which contains 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.")
            }
        }
    }
}

/*
 * Copyright 2011-2024 The OTP authors
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.SeqPlatformService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqPlatformValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqPlatformService seqPlatformService

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of instrument platform, instrument model and sequencing kit is registered in the OTP database.']
    }
    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [INSTRUMENT_PLATFORM, INSTRUMENT_MODEL]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_KIT]*.name()
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.each {
            if (!seqPlatformService.findSeqPlatform(it.getValue(INSTRUMENT_PLATFORM.name()), it.getValue(INSTRUMENT_MODEL.name()), it.getValue(SEQUENCING_KIT.name()) ?: null)) {
                context.addProblem(it.cells, LogLevel.ERROR, "The combination of instrument platform '${it.getValue(INSTRUMENT_PLATFORM.name())}', instrument model '${it.getValue(INSTRUMENT_MODEL.name())}' and sequencing kit '${it.getValue(SEQUENCING_KIT.name()) ?: ''}' is not registered in the OTP database.", "At least one combination of instrument platform, instrument model and sequencing kit is not registered in the OTP database.")
            }
        }
    }
}

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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetadataImportService
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_TYPE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION

@Component
class SeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing type is registered in the OTP database.']
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [TAGMENTATION]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            if (!seqType) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Sequencing type must not be empty.", "At least one sequencing type is empty.")
            }
            else if (!seqTypeService.findByNameOrImportAlias(seqType)) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Sequencing type '${seqType}' is not registered in the OTP database.", "At least one sequencing type is not registered in OTP.")
            }
        }
    }
}

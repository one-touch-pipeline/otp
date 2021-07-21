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

import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_ID

@Component
class RunSeqCenterValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "All entries for a run have the same value in the column '${CENTER_NAME}'.",
                'If a run is already registered in the OTP database, the sequencing center matches the one in the database.',
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [RUN_ID, CENTER_NAME]*.name()
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        allValueTuples.groupBy { it.getValue(RUN_ID.name()) }.each { String runName, Collection<ValueTuple> valueTuplesOfRun ->
            if (valueTuplesOfRun.size() == 1) {
                ValueTuple valueTuple = CollectionUtils.exactlyOneElement(valueTuplesOfRun)
                Run run = CollectionUtils.atMostOneElement(Run.findAllByName(runName))
                String seqCenterName = valueTuple.getValue(CENTER_NAME.name())
                if (run && run.seqCenter.name != seqCenterName) {
                    context.addProblem(valueTuple.cells, LogLevel.ERROR, "Run '${runName}' is already registered in the OTP database with sequencing center '${run.seqCenter.name}', not with '${seqCenterName}'.", "At least one run is already registered in the OTP database with another sequencing center.")
                }
            } else {
                context.addProblem(valueTuplesOfRun*.cells.sum(), LogLevel.ERROR, "All entries for run '${runName}' must have the same value in the column '${CENTER_NAME}'.", "All entries for one run must have the same value in the column '${CENTER_NAME}'.")
            }
        }
    }
}

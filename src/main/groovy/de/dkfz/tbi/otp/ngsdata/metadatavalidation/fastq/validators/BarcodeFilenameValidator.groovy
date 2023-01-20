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

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MultiplexingService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.INDEX

@Component
class BarcodeFilenameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ["If a barcode can be parsed from the fastq filename, it is consistent with the entry in the '${INDEX}' column."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [INDEX]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String barcode = valueTuple.getValue(INDEX.name())
            String fileName = valueTuple.getValue(FASTQ_FILE.name())
            String fileNameBarcode = MultiplexingService.barcode(fileName)

            if (barcode == null && fileNameBarcode) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING, "The ${INDEX} column is missing. OTP will use the barcode '${fileNameBarcode}' parsed from filename '${fileName}'. (For multiplexed lanes the ${INDEX} column should be filled.)", "The ${INDEX} column is missing")
            } else if (barcode == '' && fileNameBarcode) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING, "A barcode can be parsed from the filename '${fileName}', but the ${INDEX} cell is empty. OTP will ignore the barcode parsed from the filename.", "A barcode can be parsed from the filename, but the ${INDEX} cell is empty. OTP will ignore the barcode parsed from the filename.")
            } else if (barcode && fileNameBarcode && barcode != fileNameBarcode) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING, "The barcode parsed from the filename '${fileName}' ('${fileNameBarcode}') is different from the value in the ${INDEX} cell ('${barcode}'). OTP will ignore the barcode parsed from the filename and use the barcode '${barcode}'.", "At least one barcode parsed from the filename is different from the value in the ${INDEX} cell. OTP will ignore the barcode parsed from the filename.")
            }
        }
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.MultiplexingService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuplesValidator
import de.dkfz.tbi.util.spreadsheet.validation.ValueTuple
import org.springframework.stereotype.Component

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.BARCODE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE

@Component
class BarcodeFilenameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["If a barcode can be parsed from the filename, it is consistent with the entry in the '${BARCODE}' column."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE.name(), BARCODE.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == BARCODE.name()) {
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String barcode = valueTuple.getValue(BARCODE.name()) ?: ''
            String fileName = valueTuple.getValue(FASTQ_FILE.name())
            String fileNameBarcode = MultiplexingService.barcode(fileName)

            if (barcode && fileNameBarcode && fileNameBarcode != barcode) {
                context.addProblem(valueTuple.cells, Level.WARNING, "The barcodes in the filename '${fileName}' ('${fileNameBarcode}') and in the '${BARCODE}' column ('${barcode}') are different. OTP will use both barcodes.")
            }
            if (!barcode && fileNameBarcode) {
                context.addProblem(valueTuple.cells, Level.WARNING, "There is no value in the '${BARCODE}' column, but the barcode can be parsed from the filename '${fileName}'. OTP will use the parsed barcode '${fileNameBarcode}'.")
            }
        }
    }
}

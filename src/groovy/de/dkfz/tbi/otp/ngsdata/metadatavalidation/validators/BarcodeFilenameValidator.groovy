package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MultiplexingService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

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
            String barcode = valueTuple.getValue(BARCODE.name())
            String fileName = valueTuple.getValue(FASTQ_FILE.name())
            String fileNameBarcode = MultiplexingService.barcode(fileName)

            if (barcode == null && fileNameBarcode) {
                context.addProblem(valueTuple.cells, Level.WARNING, "The ${BARCODE} column is missing. OTP will use the barcode '${fileNameBarcode}' parsed from filename '${fileName}'. (For multiplexed lanes the ${BARCODE} column should be filled.)", "The ${BARCODE} column is missing")
            } else if (barcode == '' && fileNameBarcode) {
                context.addProblem(valueTuple.cells, Level.WARNING, "A barcode can be parsed from the filename '${fileName}', but the ${BARCODE} cell is empty. OTP will ignore the barcode parsed from the filename.", "A barcode can be parsed from the filename, but the ${BARCODE} cell is empty. OTP will ignore the barcode parsed from the filename.")
            } else if (barcode && fileNameBarcode && barcode != fileNameBarcode) {
                context.addProblem(valueTuple.cells, Level.WARNING, "The barcode parsed from the filename '${fileName}' ('${fileNameBarcode}') is different from the value in the ${BARCODE} cell ('${barcode}'). OTP will ignore the barcode parsed from the filename and use the barcode '${barcode}'.", "At least one barcode parsed from the filename is different from the value in the ${BARCODE} cell. OTP will ignore the barcode parsed from the filename.")
            }
        }
    }
}

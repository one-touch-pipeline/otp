package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class MateNumberFilenameValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["If a mate number can be parsed from the filename, it is consistent with the entry in the '${MATE}' column."]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [FASTQ_FILE.name(), LIBRARY_LAYOUT.name(), MATE.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == FASTQ_FILE.name()) {
            mandatoryColumnMissing(context, columnTitle)
            return false
        } else if (columnTitle == MATE.name()) {
            optionalColumnMissing(context, columnTitle, " OTP will try to guess the mate numbers from the filenames.")
        }
        return true
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        Integer fastqFileColumnIndex = context.spreadsheet.header.getCellByColumnTitle(FASTQ_FILE.name()).columnIndex
        Integer libraryLayoutColumnIndex = context.spreadsheet.header.getCellByColumnTitle(LIBRARY_LAYOUT.name())?.columnIndex
        Integer mateColumnIndex = context.spreadsheet.header.getCellByColumnTitle(MATE.name())?.columnIndex
        valueTuples.each { ValueTuple valueTuple ->
            LibraryLayout libraryLayout = LibraryLayout.findByName(valueTuple.getValue(LIBRARY_LAYOUT.name()))

            String mateNumber = valueTuple.getValue(MATE.name())
            String fileName = valueTuple.getValue(FASTQ_FILE.name())
            Integer mateCount = libraryLayout?.mateCount
            try {
                int fileNameMateNumber = MetaDataService.findOutMateNumber(fileName)
                if (mateCount == 1) {
                    if (!(fileNameMateNumber == 1)) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                                    it.columnIndex == libraryLayoutColumnIndex
                        }), Level.WARNING, "The mate number '${fileNameMateNumber}' parsed from filename '${fileName}' is not viable with library layout '${libraryLayout}'. " +
                                "If you ignore this warning, OTP will ignore the mate number parsed from the filename.", "At least one mate number parsed from filename is not viable with the library layout.")
                    }
                } else {
                    if (mateNumber != null && fileNameMateNumber.toString() != mateNumber) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                            it.columnIndex == mateColumnIndex
                        }), Level.WARNING, "The value '${mateNumber}' in the ${MATE} column is different from the mate number '${fileNameMateNumber}' parsed from the filename '${fileName}'. " +
                                "If you ignore this warning, OTP will use the mate number from the ${MATE} column and ignore the value parsed from the filename.", "At least one value in the ${MATE} column is different from the mate number parsed from the filename.")
                    }
                }
            } catch (RuntimeException e) {
                if (mateNumber == null && libraryLayout != null && mateCount != 1) {
                    if (e.message == "cannot find mateNumber for ${fileName}".toString()) {
                        context.addProblem(valueTuple.cells.findAll({
                            it.columnIndex == fastqFileColumnIndex ||
                            it.columnIndex == libraryLayoutColumnIndex
                        }), Level.ERROR, "Cannot extract mate number, because neither a ${MATE} column exists, nor can a mate number be parsed from filename '${fileName}' using any pattern known to OTP, nor can one be implied from the library layout '${libraryLayout}'.", "Cannot extract mate number")
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}

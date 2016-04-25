package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import groovy.transform.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.ngsdata.MultiplexingService.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static de.dkfz.tbi.otp.utils.StringUtils.*

@Component
class SeqTrackValidator extends ColumnSetValidator<MetadataValidationContext> implements MetadataValidator {

    static final Collection<MetaDataColumn> EQUAL_ATTRIBUTES = [
            SAMPLE_ID,
            ANTIBODY_TARGET,
            ANTIBODY,
            SEQUENCING_TYPE,
            LIBRARY_LAYOUT,
            LIB_PREP_KIT,
            INSERT_SIZE,
            PIPELINE_VERSION,
            ILSE_NO,
    ].asImmutable()

    @Override
    Collection<String> getDescriptions() {
        [
                "For the same combination of run and lane, either all or none of the rows should have a barcode.",
                "For the same combination of run, lane and barcode, there must be the same value in each of the columns '${EQUAL_ATTRIBUTES.join("', '")}'.",
                "For the same combination of run, lane and barcode no data must be registered in OTP yet.",
                "For each combination of run, lane and barcode, there must be exactly one row for each mate.",
                "For the same combination of run, lane and barcode, the filenames must differ in exactly one character which is the mate number.",
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [
                RUN_ID.name(),
                LANE_NO.name(),
        ]
    }

    @Override
    void validate(MetadataValidationContext context) {
        getRowsWithExtractedValues(context).groupBy { it.runName.value }.values().each { List<RowWithExtractedValues> runRows ->
            runRows.groupBy { it.laneNumber.value }.values().each { List<RowWithExtractedValues> laneRows ->
                Map<String, List<RowWithExtractedValues>> laneRowsByBarcode =
                        laneRows.findAll { it.barcode }.groupBy { it.barcode.value }
                validateMultiplexing(context, laneRowsByBarcode)
                laneRowsByBarcode.values().each { List<RowWithExtractedValues> seqTrackRows ->
                    validateSeqTrack(context, seqTrackRows)
                }
            }
        }
    }

    List<RowWithExtractedValues> getRowsWithExtractedValues(MetadataValidationContext context) {
        List<Column> columns = findColumns(context)
        if (!columns) {
            return Collections.emptyList()
        }
        def (Column runColumn, Column laneNumberColumn) = columns
        return context.spreadsheet.dataRows.collect {
            Cell runCell = it.getCell(runColumn)
            Cell laneNumberCell = it.getCell(laneNumberColumn)
            new RowWithExtractedValues(
                    it,
                    new ExtractedValue(runCell.text, [runCell] as Set),
                    new ExtractedValue(laneNumberCell.text, [laneNumberCell] as Set),
                    MetadataImportService.extractBarcode(it),
                    MetadataImportService.extractMateNumber(it),
            )
        }
    }

    static void validateMultiplexing(MetadataValidationContext context, Map<String, List<RowWithExtractedValues>> laneRowsByBarcode) {
        if (laneRowsByBarcode.isEmpty()) {
            return
        }

        RowWithExtractedValues anyLaneRow = laneRowsByBarcode.values().first().first()

        boolean hasRowsWithBarcode = false
        if (laneRowsByBarcode.containsKey(null)) {
            if (laneRowsByBarcode.size() > 1) {
                hasRowsWithBarcode = true
                context.addProblem(seqTrackCells((Collection<RowWithExtractedValues>) laneRowsByBarcode.values().sum()),
                        Level.WARNING, "For ${anyLaneRow.laneString} there are rows with and without barcode.")
            }
            if (SeqTrack.createCriteria().get {
                run {
                    eq 'name', anyLaneRow.runName.value
                }
                like 'laneId', "${escapeForSqlLike("${anyLaneRow.laneNumber.value}${BARCODE_DELIMITER}")}%"
                maxResults 1
            }) {
                List<RowWithExtractedValues> laneRowsWithoutBarcode = laneRowsByBarcode.get(null)
                context.addProblem(seqTrackCells(laneRowsWithoutBarcode),
                        Level.WARNING, "At least one row for ${anyLaneRow.laneString} has no barcode, but for that run and lane there already is data with a barcode registered in OTP.")
            }
        } else {
            hasRowsWithBarcode = true
        }

        if (hasRowsWithBarcode) {
            if (SeqTrack.createCriteria().get {
                run {
                    eq 'name', anyLaneRow.runName.value
                }
                eq 'laneId', anyLaneRow.laneNumber.value
                maxResults 1
            }) {
                List<RowWithExtractedValues> laneRowsWithBarcode =
                        (List<RowWithExtractedValues>)laneRowsByBarcode.findAll { it.key != null }.values().sum()
                context.addProblem(seqTrackCells(laneRowsWithBarcode),
                        Level.WARNING, "At least one row for ${anyLaneRow.laneString} has a barcode, but for that run and lane there already is data without a barcode registered in OTP.")
            }
        }
    }

    static void validateSeqTrack(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        RowWithExtractedValues anySeqTrackRow = seqTrackRows.first()

        for (MetaDataColumn mdColumn : EQUAL_ATTRIBUTES) {
            Column column = context.spreadsheet.getColumn(mdColumn.name())
            if (column && seqTrackRows*.row*.getCell(column)*.text.unique().size() != 1) {
                context.addProblem(seqTrackCells(seqTrackRows) + seqTrackRows*.row*.getCell(column) as Set,
                        Level.ERROR, "All rows for ${anySeqTrackRow.seqTrackString} must have the same value in column '${mdColumn}'.")
            }
        }

        if (SeqTrack.createCriteria().get {
            run {
                eq 'name', anySeqTrackRow.runName.value
            }
            eq 'laneId', combineLaneNumberAndBarcode(anySeqTrackRow.laneNumber.value, anySeqTrackRow.barcode.value)
            maxResults 1
        }) {
            context.addProblem(seqTrackCells(seqTrackRows),
                    Level.ERROR, "For ${anySeqTrackRow.seqTrackString}, data is already registered in OTP.")
        }

        validateMates(context, seqTrackRows)
        validateMateNumbersInFilenames(context, seqTrackRows)
    }

    static void validateMates(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        Map<String, List<RowWithExtractedValues>> seqTrackRowsByMateNumber =
                seqTrackRows.findAll { it.mateNumber }.groupBy { it.mateNumber.value }

        seqTrackRowsByMateNumber.values().each { List<RowWithExtractedValues> mateRows ->
            if (mateRows.size() > 1) {
                context.addProblem(mateCells(mateRows),
                        Level.ERROR, "There must be no more than one row for ${mateRows.first().mateString}."
                )
            }
        }

        Column libraryLayoutColumn = context.spreadsheet.getColumn(LIBRARY_LAYOUT.name())
        Collection<Cell> libraryLayoutCells = seqTrackRows*.row*.getCell(libraryLayoutColumn)
        Collection<String> libraryLayoutNames = libraryLayoutCells*.text.unique()
        String libraryLayoutName = libraryLayoutNames.size() == 1 ? libraryLayoutNames.first() : null
        LibraryLayout libraryLayout = LibraryLayout.values().find {
            it.name() == libraryLayoutName
        }
        if (seqTrackRows.every { it.mateNumber } && libraryLayout) {
            Collection<Integer> missingMateNumbers = (1..libraryLayout.mateCount).findAll {
                !seqTrackRowsByMateNumber.containsKey(Integer.toString(it))
            }
            if (missingMateNumbers.size() == 1) {
                context.addProblem(mateCells(seqTrackRows) + libraryLayoutCells,
                        Level.ERROR, "Mate ${exactlyOneElement(missingMateNumbers)} is missing for ${seqTrackRows.first().seqTrackString} with library layout '${libraryLayoutName}'."
                )
            } else if (missingMateNumbers) {
                context.addProblem(mateCells(seqTrackRows) + libraryLayoutCells,
                        Level.ERROR, "The following mates are missing for ${seqTrackRows.first().seqTrackString} with library layout '${libraryLayoutName}': ${missingMateNumbers.join(', ')}"
                )
            }
        }
    }

    static void validateMateNumbersInFilenames(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        Column filenameColumn = context.spreadsheet.getColumn(FASTQ_FILE.name())
        if (filenameColumn && seqTrackRows.size() > 1) {
            Collection<Cell> filenameCells = seqTrackRows*.row*.getCell(filenameColumn)
            Map<String, Character> extractedMateNumbers = extractDistinguishingCharacter(filenameCells*.text)
            if (extractedMateNumbers == null) {
                context.addProblem(seqTrackCells(seqTrackRows) + filenameCells,
                        Level.ERROR, "The filenames '${filenameCells*.text.sort().join("', '")}' for ${seqTrackRows.first().seqTrackString} do not differ in exactly one character. They must differ in exactly one character which is the mate number.")
            } else {
                seqTrackRows.each {
                    Cell filenameCell = it.row.getCell(filenameColumn)
                    String filename = filenameCell.text
                    String extractedMateNumber = extractedMateNumbers.get(filename)
                    if (it.mateNumber && extractedMateNumber != it.mateNumber.value) {
                        context.addProblem(seqTrackCells(seqTrackRows) + seqTrackRows*.row*.getCell(filenameColumn).toSet() + mateCells([it]),
                                Level.ERROR, "The filenames '${filenameCells*.text.sort().join("', '")}' for ${seqTrackRows.first().seqTrackString} differ in exactly one character as expected, but the distinguishing character '${extractedMateNumber}' in filename '${filename}' is not the mate number '${it.mateNumber.value}'.")
                    }
                }
            }
        }
    }

    static Set<Cell> seqTrackCells(Collection<RowWithExtractedValues> rows) {
        return rows*.runName*.cells.sum() + rows*.laneNumber*.cells.sum() + rows*.barcode*.cells.sum()
    }

    static Set<Cell> mateCells(Collection<RowWithExtractedValues> rows) {
        return seqTrackCells(rows) + (Set<Cell>)rows*.mateNumber*.cells.sum()
    }
}

@TupleConstructor
class RowWithExtractedValues {
    final Row row
    final ExtractedValue runName
    final ExtractedValue laneNumber
    final ExtractedValue barcode
    final ExtractedValue mateNumber

    String getLaneString() {
        return "run '${runName.value}', lane '${laneNumber.value}'"
    }

    String getSeqTrackString() {
        return "${laneString}, ${barcode.value != null ? "barcode '${barcode.value}'" : "no barcode"}"
    }

    String getMateString() {
        return "${seqTrackString}, mate '${mateNumber.value}'"
    }
}

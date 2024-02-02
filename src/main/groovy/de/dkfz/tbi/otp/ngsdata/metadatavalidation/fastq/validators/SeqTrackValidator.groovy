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

import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.ngsdata.MultiplexingService.combineLaneNumberAndBarcode
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement
import static de.dkfz.tbi.otp.utils.StringUtils.extractDistinguishingCharacter

@Component
class SeqTrackValidator extends AbstractColumnSetValidator<MetadataValidationContext> implements MetadataValidator {

    static final Collection<MetaDataColumn> EQUAL_ATTRIBUTES = [
            ANTIBODY,
            ANTIBODY_TARGET,
            BASE_MATERIAL,
            FASTQ_GENERATOR,
            FRAGMENT_SIZE,
            ILSE_NO,
            LIB_PREP_KIT,
            PROJECT,
            SAMPLE_NAME,
            SEQUENCING_TYPE,
            SEQUENCING_READ_TYPE,
            SINGLE_CELL_WELL_LABEL,
            TAGMENTATION_LIBRARY,
    ].asImmutable()

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return [
                "For the same combination of run and lane, either all or none of the rows should have a barcode.",
                "For the same combination of run, lane and barcode, there must be the same value in each of the columns '${EQUAL_ATTRIBUTES.join("', '")}'.",
                "For the same combination of run, lane and barcode data is already registered in OTP.",
                "For each combination of run, lane and barcode, there must be exactly one row for each mate (Ignoring lines containing indexes).",
                "For the same combination of run, lane and barcode, the filenames must differ in exactly one character which is the mate number (Ignoring lines containing indexes).",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [
                RUN_ID,
                LANE_NO,
                PROJECT,
        ]*.name()
    }

    @Override
    void validate(MetadataValidationContext context) {
        getRowsWithExtractedValues(context).groupBy {
            it.runName.value
        }.each { String runName, List<RowWithExtractedValues> runRows ->
            Set<String> laneIds = findAllLanesForRun(runName)
            runRows.groupBy { it.laneNumber.value }.each { String laneNumber, List<RowWithExtractedValues> laneRows ->
                Map<String, List<RowWithExtractedValues>> laneRowsByBarcode = laneRows.findAll { it.barcode }.groupBy { it.barcode.value }
                validateMultiplexing(context, laneRowsByBarcode, laneIds)
                laneRowsByBarcode.values().each { List<RowWithExtractedValues> seqTrackRows ->
                    validateSeqTrack(context, seqTrackRows, laneIds)
                }
            }
        }
    }

    @CompileDynamic
    private List<RowWithExtractedValues> getRowsWithExtractedValues(MetadataValidationContext context) {
        Column runColumn
        Column laneNumberColumn
        Column projectColumn
        try {
            (runColumn, laneNumberColumn, projectColumn) = findColumns(context)
        } catch (ColumnsMissingException ignored) {
            return []
        }
        return context.spreadsheet.dataRows.collect {
            Cell runCell = it.getCell(runColumn)
            Cell laneNumberCell = it.getCell(laneNumberColumn)
            Cell projectCell = it.getCell(projectColumn)
            new RowWithExtractedValues(
                    it,
                    new ExtractedValue(projectCell.text, [projectCell] as Set),
                    new ExtractedValue(runCell.text, [runCell] as Set),
                    new ExtractedValue(laneNumberCell.text, [laneNumberCell] as Set),
                    MetadataImportService.extractBarcode(it),
                    MetadataImportService.extractMateNumber(it),
            )
        }
    }

    @CompileDynamic
    private Set<String> findAllLanesForRun(String runName) {
        return SeqTrack.withCriteria {
            projections {
                run {
                    eq 'name', runName
                }
                property('laneId')
            }
        } as Set<String>
    }

    @CompileDynamic
    private Set<String> findAllProjectsForRunAndLaneId(String runName, String laneId) {
        return SeqTrack.withCriteria {
            projections {
                run {
                    eq 'name', runName
                }
                eq 'laneId', laneId
                sample {
                    individual {
                        project {
                            property 'name'
                        }
                    }
                }
            }
        } as Set<String>
    }

    private void validateMultiplexing(MetadataValidationContext context, Map<String, List<RowWithExtractedValues>> laneRowsByBarcode, Set<String> laneIds) {
        if (laneRowsByBarcode.isEmpty()) {
            return
        }

        RowWithExtractedValues anyLaneRow = laneRowsByBarcode.values().first().first()

        boolean hasRowsWithBarcode = false
        if (laneRowsByBarcode.containsKey(null)) {
            if (laneRowsByBarcode.size() > 1) {
                hasRowsWithBarcode = true
                context.addProblem(seqTrackCells((Collection<RowWithExtractedValues>) laneRowsByBarcode.values().sum()),
                        LogLevel.WARNING,
                        "For ${anyLaneRow.laneString} there are rows with and without barcode.",
                        "There are rows with and without barcode.")
            }
            String search = "${anyLaneRow.laneNumber.value}${MultiplexingService.BARCODE_DELIMITER}"
            if (laneIds.any {
                it.startsWith(search)
            }) {
                List<RowWithExtractedValues> laneRowsWithoutBarcode = laneRowsByBarcode.get(null)
                context.addProblem(seqTrackCells(laneRowsWithoutBarcode),
                        LogLevel.WARNING,
                        "At least one row for ${anyLaneRow.laneString} has no barcode, but for that run and lane there already is data with " +
                                "a barcode registered in OTP.",
                        "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP.")
            }
        } else {
            hasRowsWithBarcode = true
        }

        if (hasRowsWithBarcode) {
            if (laneIds.contains(anyLaneRow.laneNumber.value)) {
                List<RowWithExtractedValues> laneRowsWithBarcode = (List<RowWithExtractedValues>) laneRowsByBarcode.findAll {
                    it.key != null
                }.values().sum()
                context.addProblem(seqTrackCells(laneRowsWithBarcode),
                        LogLevel.WARNING,
                        "At least one row for ${anyLaneRow.laneString} has a barcode, but for that run and lane there already is data without" +
                                " a barcode registered in OTP.",
                        "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP.")
            }
        }
    }

    private void validateSeqTrack(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows, Set<String> laneIds) {
        RowWithExtractedValues anySeqTrackRow = seqTrackRows.first()

        for (MetaDataColumn mdColumn : EQUAL_ATTRIBUTES) {
            Column column = context.spreadsheet.getColumn(mdColumn.name())
            if (column && seqTrackRows*.row*.getCell(column)*.text.unique().size() != 1) {
                context.addProblem(seqTrackCells(seqTrackRows) + seqTrackRows*.row*.getCell(column) as Set,
                        LogLevel.ERROR,
                        "All rows for ${anySeqTrackRow.seqTrackString} must have the same value in column '${mdColumn}'.",
                        "All rows of the same seqTrack must have the same value in column '${mdColumn}'.")
            }
        }

        if (anySeqTrackRow.laneNumber.value) {
            String laneId = combineLaneNumberAndBarcode(anySeqTrackRow.laneNumber.value, anySeqTrackRow.barcode.value)
            if (laneId in laneIds) {
                Project project = ProjectService.findByNameOrNameInMetadataFiles(anySeqTrackRow.projectName.value)
                Set<String> projectNames = findAllProjectsForRunAndLaneId(anySeqTrackRow.runName.value, laneId)
                boolean error = project && project.name in projectNames
                context.addProblem(seqTrackCells(seqTrackRows),
                        error ? LogLevel.ERROR : LogLevel.WARNING,
                        "For ${anySeqTrackRow.seqTrackString}${error ? " and project '${anySeqTrackRow.projectName.value}'" : ""}, " +
                                "data is already registered in OTP.",
                        "For at least one seqTrack, data is already registered in OTP.")
            }
        }

        validateMates(context, seqTrackRows)
        validateMateNumbersInFilenames(context, rowsWithoutIndex(context, seqTrackRows))
    }

    private void validateMates(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        Map<String, List<RowWithExtractedValues>> seqTrackRowsByMateNumber =
                seqTrackRows.findAll { it.mateNumber }.groupBy { it.mateNumber.value }

        seqTrackRowsByMateNumber.values().each { List<RowWithExtractedValues> mateRows ->
            if (mateRows.size() > 1) {
                context.addProblem(mateCells(mateRows),
                        LogLevel.ERROR, "There must be no more than one row for ${mateRows.first().mateString}.",
                        "There must be no more than one row for one mate."
                )
            }
        }

        Column libraryLayoutColumn = context.spreadsheet.getColumn(SEQUENCING_READ_TYPE.name())
        Collection<Cell> libraryLayoutCells = seqTrackRows*.row*.getCell(libraryLayoutColumn)
        Collection<String> libraryLayoutNames = libraryLayoutCells*.text.unique()
        String libraryLayoutName = libraryLayoutNames.size() == 1 ? libraryLayoutNames.first() : null
        SequencingReadType libraryLayout = SequencingReadType.values().find {
            it.name() == libraryLayoutName
        }
        if (seqTrackRows.every { it.mateNumber } && libraryLayout) {
            Collection<Integer> missingMateNumbers = (1..libraryLayout.mateCount).findAll {
                !seqTrackRowsByMateNumber.containsKey(Integer.toString(it))
            }
            if (missingMateNumbers.size() == 1) {
                context.addProblem(mateCells(seqTrackRows) + libraryLayoutCells,
                        LogLevel.ERROR, "Mate ${exactlyOneElement(missingMateNumbers)} is missing for ${seqTrackRows.first().seqTrackString} with " +
                        "sequencing read type '${libraryLayoutName}'.",
                        "A mate is missing for at least one seqTrack."
                )
            } else if (missingMateNumbers) {
                context.addProblem(mateCells(seqTrackRows) + libraryLayoutCells,
                        LogLevel.ERROR, "The following mates are missing for ${seqTrackRows.first().seqTrackString} with " +
                        "sequencing read type '${libraryLayoutName}': ${missingMateNumbers.join(', ')}",
                        "Mates are missing for at least one seqTrack."
                )
            }
        }
    }

    private List<RowWithExtractedValues> rowsWithoutIndex(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        Column mateColumn = context.spreadsheet.getColumn(READ.name())
        return seqTrackRows.findAll {
            !it.row.getCell(mateColumn)?.text?.toUpperCase(Locale.ENGLISH)?.startsWith('I')
        }
    }

    private void validateMateNumbersInFilenames(MetadataValidationContext context, List<RowWithExtractedValues> seqTrackRows) {
        Column filenameColumn = context.spreadsheet.getColumn(FASTQ_FILE.name())
        if (filenameColumn && seqTrackRows.size() > 1) {
            Collection<Cell> filenameCells = seqTrackRows*.row*.getCell(filenameColumn)
            Map<String, Character> extractedMateNumbers = extractDistinguishingCharacter(filenameCells*.text)
            if (extractedMateNumbers) {
                seqTrackRows.each {
                    Cell filenameCell = it.row.getCell(filenameColumn)
                    String filename = filenameCell.text
                    String extractedMateNumber = extractedMateNumbers.get(filename)
                    if (it.mateNumber && extractedMateNumber != it.mateNumber.value) {
                        context.addProblem(seqTrackCells(seqTrackRows) + seqTrackRows*.row*.getCell(filenameColumn).toSet() + mateCells([it]),
                                LogLevel.ERROR,
                                "The filenames '${filenameCells*.text.sort().join("', '")}' for ${seqTrackRows.first().seqTrackString} differ in exactly " +
                                        "one character as expected, but the distinguishing character '${extractedMateNumber}' in filename '${filename}' is " +
                                        "not the mate number '${it.mateNumber.value}'.",
                                "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not " +
                                        "the mate number.")
                    }
                }
            } else {
                context.addProblem(mateCells(seqTrackRows) + filenameCells,
                        LogLevel.ERROR,
                        "The filenames '${filenameCells*.text.sort().join("', '")}' for ${seqTrackRows.first().seqTrackString} do not differ in " +
                                "exactly one character. They must differ in exactly one character which is the mate number.",
                        "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is " +
                                "the mate number.")
            }
        }
    }

    @CompileDynamic
    private Set<Cell> seqTrackCells(Collection<RowWithExtractedValues> rows) {
        return rows*.runName*.cells.flatten() + rows*.laneNumber*.cells.flatten() + rows*.barcode*.cells.flatten()
    }

    private Set<Cell> mateCells(Collection<RowWithExtractedValues> rows) {
        return seqTrackCells(rows) + (Set<Cell>) rows*.mateNumber.findAll()*.cells.flatten()
    }
}

@TupleConstructor
class RowWithExtractedValues {
    final Row row
    final ExtractedValue projectName
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

/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

/**
 * A service for parsing inputs and returning values.
 */
class ScriptInputHelperService {

    private static final int INPUT_INDEX_SAMPLE_DEFINITION_PID = 0
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE = 1
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME = 2
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE = 3
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG = 4
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME = 5
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_5 = 5
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6 = 6
    private static final List<Integer> INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT = [
            INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_5,
            INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6,
    ].asImmutable()

    private static final int INPUT_INDEX_LANE_DEFINITION_PROJECT = 0
    private static final int INPUT_INDEX_LANE_DEFINITION_RUN = 1
    private static final int INPUT_INDEX_LANE_DEFINITION_LANE_ID = 2
    private static final int INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL = 3
    private static final int INPUT_INDEX_LANE_DEFINITION_COLUMN_3 = 3
    private static final int INPUT_INDEX_LANE_DEFINITION_COLUMN_4 = 4
    private static final List<Integer> INPUT_INDEX_LANE_DEFINITION_COLUMN_COUNT = [
            INPUT_INDEX_LANE_DEFINITION_COLUMN_3,
            INPUT_INDEX_LANE_DEFINITION_COLUMN_4,
    ].asImmutable()

    /**
     * The default separator for for columns.
     * The separators are:
     * - comma
     * - semicolon
     * - tab
     */
    static final String SPLIT_SEPARATOR = '[,;\t]'

    SeqTypeService seqTypeService

    /**
     * Helper to split an input area to input lines.
     * The following steps are done:
     * - trim all lines
     * - remove empty lines
     * - remove lines starting with '#' (comment)
     */
    List<String> parseHelper(String inputArea) {
        return inputArea.split('\n')*.trim().findAll {
            it && !it.startsWith('#')
        }
    }

    /**
     * Helper to split an input area to input fields.
     * Additional to {@link #parseHelper(String)} it do:
     * - split each line by the split expression, by default: {@link #SPLIT_SEPARATOR}
     * - trim all values
     */
    List<List<String>> parseAndSplitHelper(String input, String splitExpression = SPLIT_SEPARATOR) {
        return parseHelper(input).collect { String line ->
            return line.split(splitExpression)*.trim()
        }
    }

    private <T> List<T> singleValuePerLine(String input, String inputType, Closure<Collection<T>> selection) {
        return parseHelper(input).collect {
            CollectionUtils.exactlyOneElement(selection(it), "Could not find ${inputType} '${it}'")
        }
    }

    /**
     * Input parser for seqTracks ids.
     *
     * It uses {@link #parseHelper(String)} for parsing the input.
     */
    List<SeqTrack> seqTrackById(String input) {
        return singleValuePerLine(input, "seqTrack for id") {
            return SeqTrack.findAllById(it as long)
        }
    }

    /**
     * Input parser for seqTracks defined by sample seqType combination.
     * The input has the following columns:
     * - pid
     * - sample type
     * - seqType name or alias (for example WGS, WES, RNA, ...
     * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
     * - single cell flag: true = single cell, false = bulk
     * - sampleName: optional
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input.
     */
    List<SeqTrack> seqTracksBySampleDefinition(String input) {
        return parseAndSplitHelper(input).collectMany { List<String> values ->
            int valueSize = values.size()
            assert valueSize in INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT: "A multi input for sample seqType is defined by 5 or 6 columns"
            Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(
                    values[INPUT_INDEX_SAMPLE_DEFINITION_PID],
                    values[INPUT_INDEX_SAMPLE_DEFINITION_PID],
                    values[INPUT_INDEX_SAMPLE_DEFINITION_PID]),
                    "Could not find any individual with name ${values[INPUT_INDEX_SAMPLE_DEFINITION_PID]}")
            SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType.findAllByNameIlike(values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]),
                    "Could not find any sampleType with name ${values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")
            Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType),
                    "Could not find any sample for ${values[INPUT_INDEX_SAMPLE_DEFINITION_PID]} ${values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")

            SequencingReadType sequencingReadType = SequencingReadType.findByName(values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE])
            assert sequencingReadType: "${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]} is no valid sequencingReadType"

            assert values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG].toLowerCase() in ["true", "false"]: "singleCell have to be either 'true' or 'false'"
            boolean singleCell = Boolean.parseBoolean(values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG])

            SeqType seqType = seqTypeService.findByNameOrImportAlias(values[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME], [
                    libraryLayout: sequencingReadType,
                    singleCell   : singleCell,
            ])
            assert seqType: "Could not find seqType with : ${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME]} " +
                    "${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]} ${values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG]}"

            List<SeqTrack> seqTracks = SeqTrack.withCriteria {
                eq('sample', sample)
                eq('seqType', seqType)
                if (values.size() == INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6) {
                    eq('sampleIdentifier', values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME])
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${values.join(' ')}"
            return seqTracks
        }
    }

    /**
     * Input parser for seqTracks defined by project run laneId well combination.
     * The input has the following columns:
     * - project
     * - run
     * - lane (inclusive barcode)
     * - singleCellWellLabel: the label in case the data are single cell and given per well
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input.
     */
    List<SeqTrack> seqTracksByLaneDefinition(String input) {
        return parseAndSplitHelper(input).collectMany { List<String> values ->
            int valueSize = values.size()
            assert valueSize in INPUT_INDEX_LANE_DEFINITION_COLUMN_COUNT: "A multi input for lane is defined by 3 or 4 columns"
            Project project = CollectionUtils.exactlyOneElement(Project.findAllByNameOrNameInMetadataFiles(values[INPUT_INDEX_LANE_DEFINITION_PROJECT],
                    values[INPUT_INDEX_LANE_DEFINITION_PROJECT]),
                    "Could not find any project with name ${values[INPUT_INDEX_LANE_DEFINITION_PROJECT]}")
            Run run = CollectionUtils.exactlyOneElement(Run.findAllByName(values[INPUT_INDEX_LANE_DEFINITION_RUN]),
                    "Could not find any run with name ${values[INPUT_INDEX_LANE_DEFINITION_RUN]}")

            List<SeqTrack> seqTracks = SeqTrack.withCriteria {
                sample {
                    individual {
                        eq('project', project)
                    }
                }
                eq('run', run)
                eq('laneId', values[INPUT_INDEX_LANE_DEFINITION_LANE_ID])
                if (values.size() == INPUT_INDEX_LANE_DEFINITION_COLUMN_4 && values[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL]) {
                    eq('singleCellWellLabel', values[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL])
                } else {
                    isNull('singleCellWellLabel')
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${values.join(' ')}"
            return seqTracks
        }
    }
}

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
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6 = 6
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_7 = 7
    private static final List<Integer> INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT = [
            INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6,
            INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_7,
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
        return inputArea.split('\n(?=(?:[^\']*\'[^\']*\')*[^\']*$)')*.trim().findAll {
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

    /**
     * Input parser for seqTracks ids.
     *
     * It uses {@link #parseHelper(String)} for parsing the input.
     */
    List<SeqTrack> seqTrackById(String input) {
        return parseAndSplitHelper(input).collectMany { List<String> values ->
            int valuesSize = values.size()
            assert valuesSize == 2: "A multi input for seqType by ID is defined by 2 columns"
            List<String> valuesWithoutComments = removeCommentsFromStringList(values)
            return valuesWithoutComments
        }.collectMany {
            List<SeqTrack> seqTracks = SeqTrack.findAllById(it as long)
            assert seqTracks.size() >= 1 : "Could not find seqTrack for id '${it}'"
            return seqTracks
        }
    }

    /**
     * Checks whether a multiLineString contains content with lines that are not commented out.
     */
    boolean checkIfExactlyOneMultiLineStringContainsContent(List<String> multiLineStringList) {
        Integer sumOfEmptyEntries = 0
        for (multiLineString in multiLineStringList) {
            sumOfEmptyEntries += ((parseHelper(multiLineString).empty) ? 1 : 0)
        }
        return (sumOfEmptyEntries == multiLineStringList.size() - 1)
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
            assert valueSize in INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT: "A multi input for sample seqType is defined by 6 or 7 columns"
            List<String> valuesWithoutComments = removeCommentsFromStringList(values)
            Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPidOrMockPidOrMockFullName(
                    valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_PID],
                    valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_PID],
                    valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_PID]),
                    "Could not find any individual with name ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_PID]}")
            SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType
                    .findAllByNameIlike(valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]),
                    "Could not find any sampleType with name ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")
            Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType),
                    "Could not find any sample for ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_PID]}" +
                            " ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")

            SequencingReadType sequencingReadType = SequencingReadType.findByName(valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE])
            assert sequencingReadType: "${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]} is no valid sequencingReadType"

            assert valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG]
                    .toLowerCase() in ["true", "false"]: "singleCell have to be either 'true' or 'false'"
            boolean singleCell = Boolean.parseBoolean(valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG])

            SeqType seqType = seqTypeService.findByNameOrImportAlias(valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME], [
                    libraryLayout: sequencingReadType,
                    singleCell   : singleCell,
            ])
            assert seqType: "Could not find seqType with : ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME]} " +
                    "${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]}" +
                    " ${valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG]}"

            List<SeqTrack> seqTracks = SeqTrack.withCriteria {
                eq('sample', sample)
                eq('seqType', seqType)
                if (valuesWithoutComments.size() == INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_6) {
                    eq('sampleIdentifier', valuesWithoutComments[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME])
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${valuesWithoutComments.join(' ')}"
            return seqTracks
        }
    }

    /**
     * Input parser for withdrawn Comments.
     * The input has the following columns:
     * - pid
     * - sample type
     * - seqType name or alias (for example WGS, WES, RNA, ...
     * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
     * - single cell flag: true = single cell, false = bulk
     * - sampleName: optional
     * - withdrawn comment: optional. Put it in quote marks 'withdrawn Comment'
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input and then searches for the withdrawn
     * comments encapsulated by quote marks. If no such withdrawn comments exists it gets replaced by a defaultComment.
     */
    List<String> getCommentsFromMultiLineDefinition(String input) {
        return parseAndSplitHelper(input.trim()).collect {
            String comment = it.find {
                (it.startsWith("\'") && it.endsWith("\'"))
            }
            return comment ? comment.substring(1, comment.length() - 1).trim().stripIndent() : ""
        }
    }

    /**
     * Removes all comments that are encapsulated by quote marks and SPLIT_SEPARATOR and replaces it with one commata
     * @param input : a multiLineDefinition, that could contain comments
     * @return the multiLineDefinition without comments
     */
    List<String> removeCommentsFromStringList(List<String> input) {
        input.removeAll {
            it.startsWith("\'") && it.endsWith("\'")
        }
        return input
    }

    /**
     * Checks if all lines contain withdrawn comments.
     * The input has the following columns:
     * - pid
     * - sample type
     * - seqType name or alias (for example WGS, WES, RNA, ...
     * - sequencingReadType (LibraryLayout): PAIRED, SINGLE, MATE_PAIRED
     * - single cell flag: true = single cell, false = bulk
     * - sampleName: optional
     * - withdrawn comment: optional. Put it in quote marks 'withdrawn Comment'
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input and checking if all lines
     * contain a withdrawn comment encapsulated by quote marks.
     */
    boolean isCommentInMultiLineDefinition(String input) {
        return !parseAndSplitHelper(input).collect {
            return it.find {
                (it.startsWith("\'") && it.endsWith("\'"))
            }
        }.contains(null)
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
            List<String> valuesWithoutComments = removeCommentsFromStringList(values)
            int valueSize = valuesWithoutComments.size()
            assert valueSize in INPUT_INDEX_LANE_DEFINITION_COLUMN_COUNT: "A multi input for lane is defined by 3 or 4 columns"
            Project project = CollectionUtils.exactlyOneElement(Project.findAllByNameOrNameInMetadataFiles(
                    valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_PROJECT],
                    valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_PROJECT]),
                    "Could not find any project with name ${valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_PROJECT]}")
            Run run = CollectionUtils.exactlyOneElement(Run.findAllByName(valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_RUN]),
                    "Could not find any run with name ${valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_RUN]}")

            List<SeqTrack> seqTracks = SeqTrack.withCriteria {
                sample {
                    individual {
                        eq('project', project)
                    }
                }
                eq('run', run)
                eq('laneId', valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_LANE_ID])
                if (valuesWithoutComments.size() == INPUT_INDEX_LANE_DEFINITION_COLUMN_4 &&
                        valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL]) {
                    eq('singleCellWellLabel', valuesWithoutComments[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL])
                } else {
                    isNull('singleCellWellLabel')
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${valuesWithoutComments.join(' ')}"
            return seqTracks
        }
    }
}

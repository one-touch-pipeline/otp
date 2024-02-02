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
package de.dkfz.tbi.otp.utils

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project

/**
 * A service for parsing inputs and returning values.
 */
@CompileDynamic
class ScriptInputHelperService {

    private static final int INPUT_INDEX_SAMPLE_DEFINITION_PID = 0
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE = 1
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME = 2
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE = 3
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG = 4
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME = 5
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COMMENT = 6
    private static final int INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT = 7

    private static final int INPUT_INDEX_LANE_DEFINITION_PROJECT = 0
    private static final int INPUT_INDEX_LANE_DEFINITION_RUN = 1
    private static final int INPUT_INDEX_LANE_DEFINITION_LANE_ID = 2
    private static final int INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL = 3
    private static final int INPUT_INDEX_LANE_DEFINITION_COMMENT = 4
    private static final int INPUT_INDEX_LANE_DEFINITION_COLUMN_COUNT = 5

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
     * A helper to remove single quotes of an string.
     */
    String removeSurroundingSingleQuote(String s) {
        if (s == "''") {
            // for "''" returning s[1..-2] return again "''"
            return ""
        } else if (s && s.size() >= 2 && s[0] == "'" && s[-1] == "'") {
            return s[1..-2]
        }
        return s
    }

    /**
     * Input parser for seqTracks ids.
     *
     * It uses {@link #parseHelper(String)} for parsing the input.
     */
    List<SeqTrackWithComment> seqTrackById(String input) {
        return parseAndSplitHelper(input).collect { List<String> values ->
            assert values.size() == 2: "A multi input for seqType by ID is defined by 2 columns"
            SeqTrack seqTrack = SeqTrack.get(values[0] as long)
            assert seqTrack: "Could not find seqTrack for id '${values[0]}'"

            String comment = removeSurroundingSingleQuote(values[1])
            assert comment: "Comment may not be empty"

            return new SeqTrackWithComment(seqTrack, comment)
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
     * - sampleName: name of a sample, may be empty
     *
     * Additional, a comment have to be given, which may contain multiple lines and is enclosed by '
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input.
     */
    List<SeqTrackWithComment> seqTracksBySampleDefinition(String input) {
        return parseAndSplitHelper(input).collectMany { List<String> values ->
            assert values.size() == INPUT_INDEX_SAMPLE_DEFINITION_COLUMN_COUNT: "A multi input for sample seqType is defined by 7 columns"
            Individual individual = CollectionUtils.exactlyOneElement(Individual.findAllByPid(values[INPUT_INDEX_SAMPLE_DEFINITION_PID]),
                    "Could not find any individual with name ${values[INPUT_INDEX_SAMPLE_DEFINITION_PID]}")
            SampleType sampleType = CollectionUtils.exactlyOneElement(SampleType
                    .findAllByNameIlike(values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]),
                    "Could not find any sampleType with name ${values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")
            Sample sample = CollectionUtils.exactlyOneElement(Sample.findAllByIndividualAndSampleType(individual, sampleType),
                    "Could not find any sample for ${values[INPUT_INDEX_SAMPLE_DEFINITION_PID]}" +
                            " ${values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_TYPE]}")

            SequencingReadType sequencingReadType = SequencingReadType.getByName(values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE])
            assert sequencingReadType: "${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]} is no valid sequencingReadType"

            assert values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG]
                    .toLowerCase() in ["true", "false"]: "singleCell have to be either 'true' or 'false'"
            boolean singleCell = Boolean.parseBoolean(values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG])

            SeqType seqType = seqTypeService.findByNameOrImportAlias(values[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME], [
                    libraryLayout: sequencingReadType,
                    singleCell   : singleCell,
            ])
            assert seqType: "Could not find seqType with : ${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQ_TYPE_NAME]} " +
                    "${values[INPUT_INDEX_SAMPLE_DEFINITION_SEQUENCING_READ_TYPE]}" +
                    " ${values[INPUT_INDEX_SAMPLE_DEFINITION_SINGLE_CELL_FLAG]}"

            List<SeqTrack> seqTracks = SeqTrack.withCriteria {
                eq('sample', sample)
                eq('seqType', seqType)
                if (values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME]) {
                    eq('sampleIdentifier', values[INPUT_INDEX_SAMPLE_DEFINITION_SAMPLE_NAME])
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${values.join(' ')}"

            String comment = removeSurroundingSingleQuote(values[INPUT_INDEX_SAMPLE_DEFINITION_COMMENT])
            assert comment: "Comment may not be empty"

            return seqTracks.collect {
                new SeqTrackWithComment(it, comment)
            }
        }
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
     * Additional, a comment have to be given, which may contain multiple lines and is enclosed by '
     *
     * It uses {@link #parseAndSplitHelper(String)} with the separator {@link #SPLIT_SEPARATOR} for parsing the input.
     */
    List<SeqTrackWithComment> seqTracksByLaneDefinition(String input) {
        return parseAndSplitHelper(input).collectMany { List<String> values ->
            assert values.size() == INPUT_INDEX_LANE_DEFINITION_COLUMN_COUNT: "A multi input for lane is defined by 5 columns"
            Project project = CollectionUtils.exactlyOneElement(Project.findAllByNameOrNameInMetadataFiles(
                    values[INPUT_INDEX_LANE_DEFINITION_PROJECT],
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
                if (values[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL]) {
                    eq('singleCellWellLabel', values[INPUT_INDEX_LANE_DEFINITION_SINGLE_CELL_WELL_LABEL])
                } else {
                    isNull('singleCellWellLabel')
                }
            }
            assert seqTracks: "Could not find any seqTracks for ${values.join(' ')}"

            String comment = removeSurroundingSingleQuote(values[INPUT_INDEX_LANE_DEFINITION_COMMENT])
            assert comment: "Comment may not be empty"

            return seqTracks.collect {
                new SeqTrackWithComment(it, comment)
            }
        }
    }
}

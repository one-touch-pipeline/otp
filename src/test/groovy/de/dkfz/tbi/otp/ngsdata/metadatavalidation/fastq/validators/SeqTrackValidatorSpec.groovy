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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Column
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.LibraryLayout.PAIRED
import static de.dkfz.tbi.otp.ngsdata.LibraryLayout.SINGLE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.ngsdata.MultiplexingService.combineLaneNumberAndBarcode
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.createContext

class SeqTrackValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                Project,
                Realm,
                Run,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    SeqTrackValidator validator = new SeqTrackValidator()

    private static final Collection<MetaDataColumn> SEQ_TRACK_COLUMNS = [RUN_ID, LANE_NO, INDEX].asImmutable()

    private static final Collection<MetaDataColumn> MATE_COLUMNS = (SEQ_TRACK_COLUMNS + READ).asImmutable()

    private
    static Set<Cell> cells(MetadataValidationContext context, Collection<MetaDataColumn> columns, int ... rowIndices) {
        Set<Cell> cells = [] as Set
        columns.each {
            Column column = context.spreadsheet.getColumn(it.name())
            if (column) {
                rowIndices.each {
                    cells.add(context.spreadsheet.dataRows[it].getCell(column))
                }
            }
        }
        return cells
    }

    private static MetadataValidationContext createContextWithoutBarcode() {
        return createContext((
                "${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "runA L1\n" +
                        "runA L1\n" +
                        "runA L2\n" +  // unrelated lane
                        "runB L1"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithBarcode() {
        return createContext((
                "${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "runA L1 ABC\n" +
                        "runA L1 ABC\n" +
                        "runA L2 ABC\n" +  // unrelated lane
                        "runB L1 ABC"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithAndWithoutBarcode() {
        return createContext((
                "${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "runA L1\n" +
                        "runA L1 ABC"
        ).replace(' ', '\t'))
    }

    private static void createSeqTrack(String runName, String laneNumber, String barcode = null) {
        DomainFactory.createSeqTrack(
                run: Run.findByName(runName) ?: DomainFactory.createRun(name: runName),
                laneId: combineLaneNumberAndBarcode(laneNumber, barcode),
        )
    }

    void 'validate, when lane number column is missing, does not crash'() {
        given:
        MetadataValidationContext context = createContext("${RUN_ID}\nrunA")
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR, "Required column 'LANE_NO' is missing."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when barcode extraction fails, does not complain'() {
        given:
        MetadataValidationContext context = createContext((
                "${RUN_ID} ${LANE_NO}\n" +
                        "runA L1\n" +
                        "runA L1"
        ).replace(' ', '\t'))
        createSeqTrack('runA', 'L1')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    // validate, given data without barcode ...

    void 'validate, given data without barcode and no data for same lane in database, succeeds'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSeqTrack('runA', 'L8')
        createSeqTrack('runZ', 'L1')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data without barcode and data without barcode for same lane in database, adds error'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data without barcode and data with barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data without barcode and data with and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSeqTrack('runA', 'L1')
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    // validate, given data with barcode ...

    void 'validate, given data with barcode and no data for same lane in database, succeeds'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L8', 'ABC')
        createSeqTrack('runZ', 'L1', 'ABC')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data with barcode and data without barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with same barcode for same lane in database, adds error'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with other barcode for same lane in database, succeeds'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L1', 'DEF')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data with barcode and data with same and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L1')
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with other and without barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSeqTrack('runA', 'L1')
        createSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    // validate, given data with and without barcode ...

    void 'validate, given data with and without barcode and no data for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L8')
        createSeqTrack('runZ', 'L1')
        createSeqTrack('runA', 'L8', 'ABC')
        createSeqTrack('runZ', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data without barcode for same lane in database, adds error and warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with same barcode for same lane in database, adds error and warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with other barcode for same lane in database, adds warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with same and without barcode for same lane in database, adds errors and warning'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L1')
        createSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with other and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSeqTrack('runA', 'L1')
        createSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    // equal values in columns

    void 'validate, when columns which must have equal values for a SeqTrack have different values, adds errors'() {
        given:
        MetadataValidationContext context = createContext((
                "${SAMPLE_NAME} ${SEQUENCING_TYPE} ${FASTQ_GENERATOR} ${RUN_ID} ${LANE_NO} ${TAGMENTATION_LIBRARY} ${INDEX}\n" +
                        "A F K runA L1 1\n" +
                        "A F K runA L1 2\n" +
                        "B F L runA L1 1\n" +
                        "A F K runA L3 1 ABC\n" +
                        "A G K runA L3 1 ABC\n" +
                        "C H M runA L3 1 DEF\n" +  // unrelated barcode
                        "D I N runA L2 1\n" +  // unrelated lane
                        "E J O runB L1 1\n"      // unrelated run
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + SAMPLE_NAME, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${SAMPLE_NAME}'.", "All rows of the same seqTrack must have the same value in column 'SAMPLE_NAME'."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_GENERATOR, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${FASTQ_GENERATOR}'.", "All rows of the same seqTrack must have the same value in column 'FASTQ_GENERATOR'."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + TAGMENTATION_LIBRARY, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${TAGMENTATION_LIBRARY}'.", "All rows of the same seqTrack must have the same value in column 'TAGMENTATION_LIBRARY'."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + SEQUENCING_TYPE, 3, 4),
                        Level.ERROR, "All rows for run 'runA', lane 'L3', barcode 'ABC' must have the same value in column '${SEQUENCING_TYPE}'.", "All rows of the same seqTrack must have the same value in column 'SEQUENCING_TYPE'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    // mates

    void 'validate, when mate extraction fails, does not complain'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${RUN_ID} ${LANE_NO} ${SEQUENCING_READ_TYPE}\n" +
                        "a.fastq.gz runA L1 ${PAIRED}\n" +
                        "b.fastq.gz runA L2 ${SINGLE}\n" +
                        "c.fastq.gz runA L3 ${SINGLE}\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when no mate is missing, succeeds'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${SEQUENCING_READ_TYPE}\n" +
                        "s_101202_7_r1.fastq.gz 1 runA L1 ${PAIRED}\n" +
                        "s_101202_7_r2.fastq.gz 2 runA L1 ${PAIRED}\n" +
                        "s_101202_7_i1.fastq.gz I1 runA L1 ${PAIRED}\n" +
                        "s_101202_7_i2.fastq.gz I2 runA L1 ${PAIRED}\n" +
                        "s_101202_7_1.fastq.gz 1 runA L2 ${SINGLE}\n" +
                        "do_not_care_.fastq.gz 1 runA L3 ${SINGLE}\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given duplicate mates for a SeqTrack, adds errors'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runB L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runB L2 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS, 0, 1),
                        Level.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 0, 1),
                        Level.ERROR, "The filenames 's_101202_7_1.fastq.gz', 's_101202_7_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when one mate is missing, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "${PAIRED} s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "${PAIRED} s_101202_5_1.fastq.gz 1 runB L1 ABC\n" +
                        "${PAIRED} s_101202_5_2.fastq.gz 2 runB L1 ABC\n" +
                        "${PAIRED} s_101202_6_1.fastq.gz 1 runB L2 ABC\n" +
                        "${PAIRED} s_101202_6_2.fastq.gz 2 runB L2 ABC\n" +
                        "${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 DEF\n" +
                        "${PAIRED} s_101202_7_2.fastq.gz 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS + SEQUENCING_READ_TYPE, 0),
                        Level.ERROR, "Mate 2 is missing for run 'runA', lane 'L1', barcode 'ABC' with sequencing read type '${LibraryLayout.PAIRED}'.", "A mate is missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when two mates are missing, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${SEQUENCING_READ_TYPE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "${PAIRED} 3 runA L1 ABC\n" +
                        "${PAIRED} 1 runB L1 ABC\n" +
                        "${PAIRED} 2 runB L1 ABC\n" +
                        "${PAIRED} 1 runB L2 ABC\n" +
                        "${PAIRED} 2 runB L2 ABC\n" +
                        "${PAIRED} 1 runA L1 DEF\n" +
                        "${PAIRED} 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS + SEQUENCING_READ_TYPE, 0),
                        Level.ERROR, "The following mates are missing for run 'runA', lane 'L1', barcode 'ABC' with sequencing read type '${LibraryLayout.PAIRED}': 1, 2", "Mates are missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing read type column is missing, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "s_101202_1_1.fastq.gz 1 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when sequencing read type is not unique for a SeqTrack, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "FOOBARFOO s_101202_7_2.fastq.gz 2 runA L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + SEQUENCING_READ_TYPE, 0, 1),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', barcode 'ABC' must have the same value in column 'SEQUENCING_READ_TYPE'.", "All rows of the same seqTrack must have the same value in column 'SEQUENCING_READ_TYPE'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing read type is not known to OTP, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "FOOBARFOO s_101202_7_1.fastq.gz 1 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    // filenames and mates

    void 'validate, when mate column exists, but filename column does not, does not crash'() {
        given:
        MetadataValidationContext context = createContext((
                "${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "1 runA L1\n" +
                        "2 runA L1\n" +
                        "i1 runA L1\n" +
                        "i2 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given only one row for a SeqTrack, does not complain about inconsistencies between mate number and filename'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "s_101202_4_1.fastq.gz 2 runA L1\n" +
                        "not_parseable_4.gz 2 runB L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when filenames of a SeqTrack do not differ in exactly one character, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_6_1.fastq.gz 1 runA L1 DEF\n" +
                        "s_101202_5_2.fastq.gz 2 runA L1 DEF\n" +
                        "not_parseable_4_1.fastq.gz 1 runB L1 ABC\n" +
                        "not_parseable_4_1.fastq.gz 1 runB L1 ABC\n" +
                        "not_parseable_6_1.fastq.gz 1 runB L1 DEF\n" +
                        "not_parseable_5_2.fastq.gz 2 runB L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS, 0, 1),
                        Level.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 0, 1),
                        Level.ERROR, "The filenames 's_101202_4_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 2, 3),
                        Level.ERROR, "The filenames 's_101202_5_2.fastq.gz', 's_101202_6_1.fastq.gz' for run 'runA', lane 'L1', barcode 'DEF' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS, 4, 5),
                        Level.ERROR, "There must be no more than one row for run 'runB', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 4, 5),
                        Level.ERROR, "The filenames 'not_parseable_4_1.fastq.gz', 'not_parseable_4_1.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 6, 7),
                        Level.ERROR, "The filenames 'not_parseable_5_2.fastq.gz', 'not_parseable_6_1.fastq.gz' for run 'runB', lane 'L1', barcode 'DEF' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when distinguishing character in filenames of a SeqTrack is not the mate number, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "s_101202_3_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_4_1.fastq.gz 2 runA L1 ABC\n" +
                        "s_101202_1_1.fastq.gz 1 runA L2 ABC\n" +
                        "s_101202_3_1.fastq.gz 2 runA L2 ABC\n" +
                        "not_parseable_2.fastq.gz 1 runB L1 ABC\n" +
                        "not_parseable_1.fastq.gz 2 runB L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 0, 1) + cells(context, MATE_COLUMNS, 0),
                        Level.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 0, 1) + cells(context, MATE_COLUMNS, 1),
                        Level.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '4' in filename 's_101202_4_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 2, 3) + cells(context, MATE_COLUMNS, 3),
                        Level.ERROR, "The filenames 's_101202_1_1.fastq.gz', 's_101202_3_1.fastq.gz' for run 'runA', lane 'L2', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 4, 5) + cells(context, MATE_COLUMNS, 4),
                        Level.ERROR, "The filenames 'not_parseable_1.fastq.gz', 'not_parseable_2.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '2' in filename 'not_parseable_2.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 4, 5) + cells(context, MATE_COLUMNS, 5),
                        Level.ERROR, "The filenames 'not_parseable_1.fastq.gz', 'not_parseable_2.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '1' in filename 'not_parseable_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when filenames of a SeqTrack differ in exactly one character which is the mate number, succeeds'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "s_101202_7_r1.fastq.gz 1 runA L1\n" +
                        "s_101202_7_r2.fastq.gz 2 runA L1\n" +
                        "s_101202_7_r1.fastq.gz 1 runA L2\n" +
                        "s_101202_7_r2.fastq.gz 2 runA L2\n" +
                        "s_101202_7_i1.fastq.gz i1 runA L1\n" +
                        "s_101202_7_i2.fastq.gz i2 runA L1\n" +
                        "s_101202_7_I1.fastq.gz I1 runA L2\n" +
                        "s_101202_7_I2.fastq.gz I2 runA L2\n" +
                        "not_parseable_1.fastq.gz 1 runC L1\n" +
                        "not_parseable_2.fastq.gz 2 runC L1\n" +
                        "not_parseable_4.fastq.gz 4 runC L2\n" +
                        "not_parseable_5.fastq.gz 5 runC L2\n" +
                        "not_parseable_6.fastq.gz 6 runC L2\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void "rowsWithoutIndex, when some rows contains index columns, then the returned list do not contain them."() {
        given:
        MetadataValidationContext context = createContext("""\
${READ}
1
2
I1
I2
i1
i2
abc

""")

        List<RowWithExtractedValues> rows = context.spreadsheet.dataRows.collect {
            new RowWithExtractedValues(it, null, null, null, new ExtractedValue(it.cells[0].text, it.cells as Set))
        }
        List<RowWithExtractedValues> expectedRows = rows[0, 1, 6, 7]

        when:
        List<RowWithExtractedValues> filteredRows = SeqTrackValidator.rowsWithoutIndex(context, rows)

        then:
        assertContainSame(
                expectedRows,
                filteredRows,
        )
    }
}

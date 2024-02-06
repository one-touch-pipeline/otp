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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Column
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.ngsdata.MultiplexingService.combineLaneNumberAndBarcode
import static de.dkfz.tbi.otp.ngsdata.SequencingReadType.PAIRED
import static de.dkfz.tbi.otp.ngsdata.SequencingReadType.SINGLE
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.createContext

@Rollback
@Integration
class SeqTrackValidatorIntegrationSpec extends Specification implements DomainFactoryCore {
    SeqTrackValidator validator = new SeqTrackValidator()

    private static final Collection<MetaDataColumn> SEQ_TRACK_COLUMNS = [RUN_ID, LANE_NO, INDEX].asImmutable()

    private static final Collection<MetaDataColumn> MATE_COLUMNS = (SEQ_TRACK_COLUMNS + READ).asImmutable()

    private static Set<Cell> cells(MetadataValidationContext context, Collection<MetaDataColumn> columns, int ... rowIndices) {
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
                "${PROJECT} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "projectA runA L1\n" +
                        "projectA runA L1\n" +
                        "projectA runA L2\n" +  // unrelated lane
                        "projectA runB L1"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithBarcode() {
        return createContext((
                "${PROJECT} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "projectA runA L1 ABC\n" +
                        "projectA runA L1 ABC\n" +
                        "projectA runA L2 ABC\n" +  // unrelated lane
                        "projectA runB L1 ABC"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithAndWithoutBarcode() {
        return createContext((
                "${PROJECT} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "projectA runA L1\n" +
                        "projectA runA L1 ABC"
        ).replace(' ', '\t'))
    }

    private void createSpecificSeqTrack(String runName, String laneNumber, String barcode = null, Sample sample = null) {
        createSeqTrack(
                run: CollectionUtils.atMostOneElement(Run.findAllByName(runName)) ?: createRun(name: runName),
                laneId: combineLaneNumberAndBarcode(laneNumber, barcode),
                sample: sample ?: createSample(),
        )
    }

    void 'validate, when lane number column is missing, does not crash'() {
        given:
        MetadataValidationContext context = createContext("${RUN_ID}\t${PROJECT}\nrunA\tproject")
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.ERROR, "Required column 'LANE_NO' is missing."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when barcode extraction fails, does not complain'() {
        given:
        MetadataValidationContext context = createContext((
                "${RUN_ID} ${LANE_NO} ${PROJECT}\n" +
                        "runA L1 project\n" +
                        "runA L1 project"
        ).replace(' ', '\t'))
        createSpecificSeqTrack('runA', 'L1')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    // validate, given data without barcode ...

    void 'validate, given data without barcode and no data for same lane in database, succeeds'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSpecificSeqTrack('runA', 'L8')
        createSpecificSeqTrack('runZ', 'L1')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data without barcode and data without barcode for same lane in database, adds error'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data without barcode and data with barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data without barcode and data with and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1')
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
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
        createSpecificSeqTrack('runA', 'L8', 'ABC')
        createSpecificSeqTrack('runZ', 'L1', 'ABC')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data with barcode and data without barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSpecificSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with same barcode for same lane in database, adds error'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with other barcode for same lane in database, succeeds'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSpecificSeqTrack('runA', 'L1', 'DEF')

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given data with barcode and data with same and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSpecificSeqTrack('runA', 'L1')
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with barcode and data with other and without barcode for same lane in database, adds warning'() {
        given:
        MetadataValidationContext context = createContextWithBarcode()
        createSpecificSeqTrack('runA', 'L1')
        createSpecificSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
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
        createSpecificSeqTrack('runA', 'L8')
        createSpecificSeqTrack('runZ', 'L1')
        createSpecificSeqTrack('runA', 'L8', 'ABC')
        createSpecificSeqTrack('runZ', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data without barcode for same lane in database, adds error and warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with same barcode for same lane in database, adds error and warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with other barcode for same lane in database, adds warnings'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with same and without barcode for same lane in database, adds errors and warning'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1')
        createSpecificSeqTrack('runA', 'L1', 'ABC')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, given data with and without barcode and data with other and without barcode for same lane in database, adds error and warning'() {
        given:
        MetadataValidationContext context = createContextWithAndWithoutBarcode()
        createSpecificSeqTrack('runA', 'L1')
        createSpecificSeqTrack('runA', 'L1', 'DEF')
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0, 1),
                        LogLevel.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1),
                        LogLevel.WARNING, "At least one row for run 'runA', lane 'L1' has a barcode, but for that run and lane there already is data without a barcode registered in OTP.", "At least one row has a barcode, but for that run and lane there already is data without a barcode registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    @Unroll
    void 'validate, when column #column which must have equal values for a SeqTrack has different values, adds errors'() {
        given:
        MetadataValidationContext context = createContext((
                "${column} ${PROJECT} ${RUN_ID} ${LANE_NO} ${INDEX}\n" +
                        "A project runA L1\n" +
                        "B project runA L1\n" +
                        "C project runB L2 ABC\n" +
                        "D project runB L2 ABC\n" +
                        "E project runB L2 DEF\n" +  // unrelated barcode
                        "F project runA L3\n" +  // unrelated lane
                        "G project runC L1\n"      // unrelated run
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + column, 0, 1),
                        LogLevel.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${column}'.", "All rows of the same seqTrack must have the same value in column '${column}'."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + column, 2, 3),
                        LogLevel.ERROR, "All rows for run 'runB', lane 'L2', barcode 'ABC' must have the same value in column '${column}'.", "All rows of the same seqTrack must have the same value in column '${column}'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        column << [SAMPLE_NAME,
                   ANTIBODY_TARGET,
                   ANTIBODY,
                   SEQUENCING_TYPE,
                   SEQUENCING_READ_TYPE,
                   LIB_PREP_KIT,
                   FRAGMENT_SIZE,
                   FASTQ_GENERATOR,
                   BASE_MATERIAL,
                   SINGLE_CELL_WELL_LABEL,
                   ILSE_NO,
                   TAGMENTATION_LIBRARY]
    }

    void 'validate, when mate extraction fails, does not complain'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${RUN_ID} ${LANE_NO} ${SEQUENCING_READ_TYPE}\n" +
                        "project a.fastq.gz runA L1 ${PAIRED}\n" +
                        "project b.fastq.gz runA L2 ${SINGLE}\n" +
                        "project c.fastq.gz runA L3 ${SINGLE}\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when no mate is missing, succeeds'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${SEQUENCING_READ_TYPE}\n" +
                        "project s_101202_7_r1.fastq.gz 1 runA L1 ${PAIRED}\n" +
                        "project s_101202_7_r2.fastq.gz 2 runA L1 ${PAIRED}\n" +
                        "project s_101202_7_i1.fastq.gz I1 runA L1 ${PAIRED}\n" +
                        "project s_101202_7_i2.fastq.gz I2 runA L1 ${PAIRED}\n" +
                        "project s_101202_7_1.fastq.gz 1 runA L2 ${SINGLE}\n" +
                        "project do_not_care_.fastq.gz 1 runA L3 ${SINGLE}\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given duplicate mates for a SeqTrack, adds errors'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "project s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "project s_101202_7_1.fastq.gz 1 runB L1 ABC\n" +
                        "project s_101202_7_1.fastq.gz 1 runB L2 ABC\n" +
                        "project s_101202_7_1.fastq.gz 1 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS, 0, 1),
                        LogLevel.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 0, 1),
                        LogLevel.ERROR, "The filenames 's_101202_7_1.fastq.gz', 's_101202_7_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when one mate is missing, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project ${PAIRED} s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "project ${PAIRED} s_101202_5_1.fastq.gz 1 runB L1 ABC\n" +
                        "project ${PAIRED} s_101202_5_2.fastq.gz 2 runB L1 ABC\n" +
                        "project ${PAIRED} s_101202_6_1.fastq.gz 1 runB L2 ABC\n" +
                        "project ${PAIRED} s_101202_6_2.fastq.gz 2 runB L2 ABC\n" +
                        "project ${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 DEF\n" +
                        "project ${PAIRED} s_101202_7_2.fastq.gz 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS + SEQUENCING_READ_TYPE, 0),
                        LogLevel.ERROR, "Mate 2 is missing for run 'runA', lane 'L1', barcode 'ABC' with sequencing read type '${SequencingReadType.PAIRED}'.", "A mate is missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when two mates are missing, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${SEQUENCING_READ_TYPE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project ${PAIRED} 3 runA L1 ABC\n" +
                        "project ${PAIRED} 1 runB L1 ABC\n" +
                        "project ${PAIRED} 2 runB L1 ABC\n" +
                        "project ${PAIRED} 1 runB L2 ABC\n" +
                        "project ${PAIRED} 2 runB L2 ABC\n" +
                        "project ${PAIRED} 1 runA L1 DEF\n" +
                        "project ${PAIRED} 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS + SEQUENCING_READ_TYPE, 0),
                        LogLevel.ERROR, "The following mates are missing for run 'runA', lane 'L1', barcode 'ABC' with sequencing read type '${SequencingReadType.PAIRED}': 1, 2", "Mates are missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing read type column is missing, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "project s_101202_1_1.fastq.gz 1 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when sequencing read type is not unique for a SeqTrack, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project ${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "project FOOBARFOO s_101202_7_2.fastq.gz 2 runA L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + SEQUENCING_READ_TYPE, 0, 1),
                        LogLevel.ERROR, "All rows for run 'runA', lane 'L1', barcode 'ABC' must have the same value in column 'SEQUENCING_READ_TYPE'.", "All rows of the same seqTrack must have the same value in column 'SEQUENCING_READ_TYPE'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing read type is not known to OTP, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${SEQUENCING_READ_TYPE} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "project FOOBARFOO s_101202_7_1.fastq.gz 1 runA L1\n"
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
                "${PROJECT} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "project 1 runA L1\n" +
                        "project 2 runA L1\n" +
                        "project i1 runA L1\n" +
                        "project i2 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given only one row for a SeqTrack, does not complain about inconsistencies between mate number and filename'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "project s_101202_4_1.fastq.gz 2 runA L1\n" +
                        "project not_parseable_4.gz 2 runB L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when filenames of a SeqTrack do not differ in exactly one character, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "project s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "project s_101202_6_1.fastq.gz 1 runA L1 DEF\n" +
                        "project s_101202_5_2.fastq.gz 2 runA L1 DEF\n" +
                        "project not_parseable_4_1.fastq.gz 1 runB L1 ABC\n" +
                        "project not_parseable_4_1.fastq.gz 1 runB L1 ABC\n" +
                        "project not_parseable_6_1.fastq.gz 1 runB L1 DEF\n" +
                        "project not_parseable_5_2.fastq.gz 2 runB L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, MATE_COLUMNS, 0, 1),
                        LogLevel.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 0, 1),
                        LogLevel.ERROR, "The filenames 's_101202_4_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 2, 3),
                        LogLevel.ERROR, "The filenames 's_101202_5_2.fastq.gz', 's_101202_6_1.fastq.gz' for run 'runA', lane 'L1', barcode 'DEF' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS, 4, 5),
                        LogLevel.ERROR, "There must be no more than one row for run 'runB', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 4, 5),
                        LogLevel.ERROR, "The filenames 'not_parseable_4_1.fastq.gz', 'not_parseable_4_1.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, MATE_COLUMNS + FASTQ_FILE, 6, 7),
                        LogLevel.ERROR, "The filenames 'not_parseable_5_2.fastq.gz', 'not_parseable_6_1.fastq.gz' for run 'runB', lane 'L1', barcode 'DEF' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when distinguishing character in filenames of a SeqTrack is not the mate number, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO} ${INDEX} UNRELATED_COLUMN\n" +
                        "project s_101202_3_1.fastq.gz 1 runA L1 ABC\n" +
                        "project s_101202_4_1.fastq.gz 2 runA L1 ABC\n" +
                        "project s_101202_1_1.fastq.gz 1 runA L2 ABC\n" +
                        "project s_101202_3_1.fastq.gz 2 runA L2 ABC\n" +
                        "project not_parseable_2.fastq.gz 1 runB L1 ABC\n" +
                        "project not_parseable_1.fastq.gz 2 runB L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 0, 1) + cells(context, MATE_COLUMNS, 0),
                        LogLevel.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 0, 1) + cells(context, MATE_COLUMNS, 1),
                        LogLevel.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '4' in filename 's_101202_4_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 2, 3) + cells(context, MATE_COLUMNS, 3),
                        LogLevel.ERROR, "The filenames 's_101202_1_1.fastq.gz', 's_101202_3_1.fastq.gz' for run 'runA', lane 'L2', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 4, 5) + cells(context, MATE_COLUMNS, 4),
                        LogLevel.ERROR, "The filenames 'not_parseable_1.fastq.gz', 'not_parseable_2.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '2' in filename 'not_parseable_2.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS + FASTQ_FILE, 4, 5) + cells(context, MATE_COLUMNS, 5),
                        LogLevel.ERROR, "The filenames 'not_parseable_1.fastq.gz', 'not_parseable_2.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '1' in filename 'not_parseable_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when filenames of a SeqTrack differ in exactly one character which is the mate number, succeeds'() {
        given:
        MetadataValidationContext context = createContext((
                "${PROJECT} ${FASTQ_FILE} ${READ} ${RUN_ID} ${LANE_NO}\n" +
                        "project s_101202_7_r1.fastq.gz 1 runA L1\n" +
                        "project s_101202_7_r2.fastq.gz 2 runA L1\n" +
                        "project s_101202_7_r1.fastq.gz 1 runA L2\n" +
                        "project s_101202_7_r2.fastq.gz 2 runA L2\n" +
                        "project s_101202_7_i1.fastq.gz i1 runA L1\n" +
                        "project s_101202_7_i2.fastq.gz i2 runA L1\n" +
                        "project s_101202_7_I1.fastq.gz I1 runA L2\n" +
                        "project s_101202_7_I2.fastq.gz I2 runA L2\n" +
                        "project not_parseable_1.fastq.gz 1 runC L1\n" +
                        "project not_parseable_2.fastq.gz 2 runC L1\n" +
                        "project not_parseable_4.fastq.gz 4 runC L2\n" +
                        "project not_parseable_5.fastq.gz 5 runC L2\n" +
                        "project not_parseable_6.fastq.gz 6 runC L2\n"
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
            new RowWithExtractedValues(it, null, null, null, null, new ExtractedValue(it.cells[0].text, it.cells as Set))
        }
        List<RowWithExtractedValues> expectedRows = rows[0, 1, 6, 7]

        when:
        List<RowWithExtractedValues> filteredRows = new SeqTrackValidator().rowsWithoutIndex(context, rows)

        then:
        assertContainSame(
                expectedRows,
                filteredRows,
        )
    }

    @Unroll
    void "validate, when data already exists adds #description"() {
        given:
        Project projectA = createProject(name: "projectA")
        Sample sample1 = createSample(individual: createIndividual(project: projectA))
        Sample sample2 = createSample(individual: createIndividual(project: projectA))
        Sample sampleOtherProject = createSample(individual: createIndividual(project: createProject(name: "projectB")))
        createRun(name: "runA")
        createRun(name: "runB")
        createSpecificSeqTrack("runA", "1", null, sample1)
        createSpecificSeqTrack("runA", "2", null, sample1)
        createSpecificSeqTrack("runB", "1", null, sample2)
        createSpecificSeqTrack("runA", "1", null, sampleOtherProject)
        MetadataValidationContext context = createContext((
                "${PROJECT} ${RUN_ID} ${LANE_NO} ${INDEX}\n" +
                        "project${error ? 'A' : 'C'} runA 1\n" +
                        "project${error ? 'A' : 'C'} runA 2\n" +
                        "project${error ? 'A' : 'C'} runB 1\n" +
                        "projectA runC 1\n" +
                        "projectA runA 4\n"
        ).replace(' ', '\t'))

        Collection<Problem> expectedProblems = [
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 0), error ? LogLevel.ERROR : LogLevel.WARNING,
                        "For run 'runA', lane '1', no barcode${error ? ' and project \'projectA\'' : ''}, data is already registered in OTP.",
                        "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 1), error ? LogLevel.ERROR : LogLevel.WARNING,
                        "For run 'runA', lane '2', no barcode${error ? ' and project \'projectA\'' : ''}, data is already registered in OTP.",
                        "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, SEQ_TRACK_COLUMNS, 2), error ? LogLevel.ERROR : LogLevel.WARNING,
                        "For run 'runB', lane '1', no barcode${error ? ' and project \'projectA\'' : ''}, data is already registered in OTP.",
                        "For at least one seqTrack, data is already registered in OTP."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        error | description
        false | "warnings"
        true  | "errors"
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.LibraryLayout.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.ngsdata.MultiplexingService.*
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.*

@Mock([
        Individual,
        Project,
        ProjectCategory,
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
])
class SeqTrackValidatorSpec extends Specification {

    SeqTrackValidator validator = new SeqTrackValidator()

    private static final Collection<MetaDataColumn> seqTrackColumns = [RUN_ID, LANE_NO, BARCODE].asImmutable()

    private static final Collection<MetaDataColumn> mateColumns = (seqTrackColumns + MATE).asImmutable()

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
                "${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "runA L1\n" +
                        "runA L1\n" +
                        "runA L2\n" +  // unrelated lane
                        "runB L1"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithBarcode() {
        return createContext((
                "${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "runA L1 ABC\n" +
                        "runA L1 ABC\n" +
                        "runA L2 ABC\n" +  // unrelated lane
                        "runB L1 ABC"      // unrelated run
        ).replace(' ', '\t'))
    }

    private static MetadataValidationContext createContextWithAndWithoutBarcode() {
        return createContext((
                "${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
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
                new Problem(Collections.emptySet(), Level.ERROR, "Mandatory column 'LANE_NO' is missing."),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
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
                new Problem(cells(context, seqTrackColumns, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, seqTrackColumns, 1),
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
                new Problem(cells(context, seqTrackColumns, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, seqTrackColumns, 0),
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
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, seqTrackColumns, 0),
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
                new Problem(cells(context, seqTrackColumns, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 1),
                        Level.WARNING, "For run 'runA', lane 'L1', barcode 'ABC', data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, seqTrackColumns, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 1),
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
                new Problem(cells(context, seqTrackColumns, 0),
                        Level.WARNING, "For run 'runA', lane 'L1', no barcode, data is already registered in OTP.", "For at least one seqTrack, data is already registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 0, 1),
                        Level.WARNING, "For run 'runA', lane 'L1' there are rows with and without barcode.", "There are rows with and without barcode."),
                new Problem(cells(context, seqTrackColumns, 0),
                        Level.WARNING, "At least one row for run 'runA', lane 'L1' has no barcode, but for that run and lane there already is data with a barcode registered in OTP.", "At least one row has no barcode, but for that run and lane there already is data with a barcode registered in OTP."),
                new Problem(cells(context, seqTrackColumns, 1),
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
                "${SAMPLE_ID} ${SEQUENCING_TYPE} ${PIPELINE_VERSION} ${RUN_ID} ${LANE_NO} ${CUSTOMER_LIBRARY} ${BARCODE}\n" +
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
                new Problem(cells(context, seqTrackColumns + SAMPLE_ID, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${SAMPLE_ID}'.", "All rows of the same seqTrack must have the same value in column 'SAMPLE_ID'."),
                new Problem(cells(context, seqTrackColumns + PIPELINE_VERSION, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${PIPELINE_VERSION}'.", "All rows of the same seqTrack must have the same value in column 'PIPELINE_VERSION'."),
                new Problem(cells(context, seqTrackColumns + CUSTOMER_LIBRARY, 0, 1, 2),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', no barcode must have the same value in column '${CUSTOMER_LIBRARY}'.", "All rows of the same seqTrack must have the same value in column 'CUSTOMER_LIBRARY'."),
                new Problem(cells(context, seqTrackColumns + SEQUENCING_TYPE, 3, 4),
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
                "${FASTQ_FILE} ${RUN_ID} ${LANE_NO} ${LIBRARY_LAYOUT}\n" +
                        "a.fastq.gz runA L1 ${PAIRED}\n" +
                        "b.fastq.gz runA L2 ${SINGLE}\n" +
                        "c.fastq.gz runA L3 ${SINGLE}\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when mate extraction fails, does not crash'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE}\t${RUN_ID}\t${LANE_NO}\t${LIBRARY_LAYOUT}\n" +
                        "a.fa stq.gz\trunA\tL1\t${SINGLE}\n" +
                        "c.fastq.gz\trunA\tL1\t${SINGLE}\n"
        ))
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells) as Set, Level.ERROR,
                        "The filenames 'a.fa stq.gz', 'c.fastq.gz' for run 'runA', lane 'L1', no barcode do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem((context.spreadsheet.dataRows[0].cells + context.spreadsheet.dataRows[1].cells) as Set, Level.ERROR,
                        "There must be no more than one row for run 'runA', lane 'L1', no barcode, mate '1'.", "There must be no more than one row for one mate.")]
        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when no mate is missing, succeeds'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${LIBRARY_LAYOUT}\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 ${PAIRED}\n" +
                        "s_101202_7_2.fastq.gz 2 runA L1 ${PAIRED}\n" +
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
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runB L1 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runB L2 ABC\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, mateColumns, 0, 1),
                        Level.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, mateColumns + FASTQ_FILE, 0, 1),
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
                "${LIBRARY_LAYOUT} ${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "${PAIRED} s_101202_4_1.fastq.gz 1 runA L1 ABC\n" +
                        "${PAIRED} s_101202_5_1.fastq.gz 1 runB L1 ABC\n" +
                        "${PAIRED} s_101202_5_2.fastq.gz 2 runB L1 ABC\n" +
                        "${PAIRED} s_101202_6_1.fastq.gz 1 runB L2 ABC\n" +
                        "${PAIRED} s_101202_6_2.fastq.gz 2 runB L2 ABC\n" +
                        "${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 DEF\n" +
                        "${PAIRED} s_101202_7_2.fastq.gz 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, mateColumns + LIBRARY_LAYOUT, 0),
                        Level.ERROR, "Mate 2 is missing for run 'runA', lane 'L1', barcode 'ABC' with library layout 'PAIRED'.", "A mate is missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when two mates are missing, adds error'() {
        given:
        MetadataValidationContext context = createContext((
                "${LIBRARY_LAYOUT} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "${PAIRED} 3 runA L1 ABC\n" +
                        "${PAIRED} 1 runB L1 ABC\n" +
                        "${PAIRED} 2 runB L1 ABC\n" +
                        "${PAIRED} 1 runB L2 ABC\n" +
                        "${PAIRED} 2 runB L2 ABC\n" +
                        "${PAIRED} 1 runA L1 DEF\n" +
                        "${PAIRED} 2 runA L1 DEF\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, mateColumns + LIBRARY_LAYOUT, 0),
                        Level.ERROR, "The following mates are missing for run 'runA', lane 'L1', barcode 'ABC' with library layout 'PAIRED': 1, 2", "Mates are missing for at least one seqTrack."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when library layout column is missing, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO}\n" +
                        "s_101202_1_1.fastq.gz 1 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, when library layout is not unique for a SeqTrack, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${LIBRARY_LAYOUT} ${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "${PAIRED} s_101202_7_1.fastq.gz 1 runA L1 ABC\n" +
                        "FOOBARFOO s_101202_7_2.fastq.gz 2 runA L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, seqTrackColumns + LIBRARY_LAYOUT, 0, 1),
                        Level.ERROR, "All rows for run 'runA', lane 'L1', barcode 'ABC' must have the same value in column 'LIBRARY_LAYOUT'.", "All rows of the same seqTrack must have the same value in column 'LIBRARY_LAYOUT'."),
        ]

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when library layout is not known to OTP, does not complain about missing mates'() {
        given:
        MetadataValidationContext context = createContext((
                "${LIBRARY_LAYOUT} ${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO}\n" +
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
                "${MATE} ${RUN_ID} ${LANE_NO}\n" +
                        "1 runA L1\n" +
                        "2 runA L1\n"
        ).replace(' ', '\t'))

        when:
        validator.validate(context)

        then:
        context.problems.isEmpty()
    }

    void 'validate, given only one row for a SeqTrack, does not complain about inconsistencies between mate number and filename'() {
        given:
        MetadataValidationContext context = createContext((
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO}\n" +
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
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
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
                new Problem(cells(context, mateColumns, 0, 1),
                        Level.ERROR, "There must be no more than one row for run 'runA', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, mateColumns + FASTQ_FILE, 0, 1),
                        Level.ERROR, "The filenames 's_101202_4_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, mateColumns + FASTQ_FILE, 2, 3),
                        Level.ERROR, "The filenames 's_101202_5_2.fastq.gz', 's_101202_6_1.fastq.gz' for run 'runA', lane 'L1', barcode 'DEF' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, mateColumns, 4, 5),
                        Level.ERROR, "There must be no more than one row for run 'runB', lane 'L1', barcode 'ABC', mate '1'.", "There must be no more than one row for one mate."),
                new Problem(cells(context, mateColumns + FASTQ_FILE, 4, 5),
                        Level.ERROR, "The filenames 'not_parseable_4_1.fastq.gz', 'not_parseable_4_1.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' do not differ in exactly one character. They must differ in exactly one character which is the mate number.", "The filenames of one seqTrack do not differ in exactly one character. They must differ in exactly one character which is the mate number."),
                new Problem(cells(context, mateColumns + FASTQ_FILE, 6, 7),
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
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO} ${BARCODE} UNRELATED_COLUMN\n" +
                        "s_101202_3_1.fastq.gz 1 runA L1 ABC\n" +
                        "s_101202_4_1.fastq.gz 2 runA L1 ABC\n" +
                        "s_101202_1_1.fastq.gz 1 runA L2 ABC\n" +
                        "s_101202_3_1.fastq.gz 2 runA L2 ABC\n" +
                        "not_parseable_2.fastq.gz 1 runB L1 ABC\n" +
                        "not_parseable_1.fastq.gz 2 runB L1 ABC\n"
        ).replace(' ', '\t'))
        Collection<Problem> expectedProblems = [
                new Problem(cells(context, seqTrackColumns + FASTQ_FILE, 0, 1) + cells(context, mateColumns, 0),
                        Level.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, seqTrackColumns + FASTQ_FILE, 0, 1) + cells(context, mateColumns, 1),
                        Level.ERROR, "The filenames 's_101202_3_1.fastq.gz', 's_101202_4_1.fastq.gz' for run 'runA', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '4' in filename 's_101202_4_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, seqTrackColumns + FASTQ_FILE, 2, 3) + cells(context, mateColumns, 3),
                        Level.ERROR, "The filenames 's_101202_1_1.fastq.gz', 's_101202_3_1.fastq.gz' for run 'runA', lane 'L2', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '3' in filename 's_101202_3_1.fastq.gz' is not the mate number '2'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, seqTrackColumns + FASTQ_FILE, 4, 5) + cells(context, mateColumns, 4),
                        Level.ERROR, "The filenames 'not_parseable_1.fastq.gz', 'not_parseable_2.fastq.gz' for run 'runB', lane 'L1', barcode 'ABC' differ in exactly one character as expected, but the distinguishing character '2' in filename 'not_parseable_2.fastq.gz' is not the mate number '1'.", "The filenames of one seqTrack differ in exactly one character as expected, but the distinguishing character is not the mate number."),
                new Problem(cells(context, seqTrackColumns + FASTQ_FILE, 4, 5) + cells(context, mateColumns, 5),
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
                "${FASTQ_FILE} ${MATE} ${RUN_ID} ${LANE_NO}\n" +
                        "s_101202_7_1.fastq.gz 1 runA L1\n" +
                        "s_101202_7_2.fastq.gz 2 runA L1\n" +
                        "s_101202_7_1.fastq.gz 1 runA L2\n" +
                        "s_101202_7_2.fastq.gz 2 runA L2\n" +
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
}

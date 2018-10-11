package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class MateNumberFilenameValidatorSpec extends Specification {

    void 'validate, when column FASTQ_FILE missing, then add expected problem'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("${MATE}\n")
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column 'FASTQ_FILE' is missing.")
        ]
        when:
        new MateNumberFilenameValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when column MATE missing, then add expected problem'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("${FASTQ_FILE}\n")
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column 'MATE' is missing. OTP will try to guess the mate numbers from the filenames.")
        ]
        when:
        new MateNumberFilenameValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when columns FASTQ_FILE and LIBRARY_LAYOUT, adds expected problems'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${LIBRARY_LAYOUT}\n" +
                        "NB_E_789_R.1.fastq.gz\t${LibraryLayout.SINGLE}\n" +
                        "NB_E_789_R.2.fastq.gz\t${LibraryLayout.SINGLE}\n" +
                        "testFileName.fastq.gz\t${LibraryLayout.SINGLE}\n" +
                        "NB_E_789_R.1.fastq.gz\t${LibraryLayout.PAIRED}\n" +
                        "testFileName.fastq.gz\t${LibraryLayout.PAIRED}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.WARNING,
                        "Optional column 'MATE' is missing. OTP will try to guess the mate numbers from the filenames.", "Optional column 'MATE' is missing. OTP will try to guess the mate numbers from the filenames."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING,
                        "The mate number '2' parsed from filename 'NB_E_789_R.2.fastq.gz' is not viable with library layout '${LibraryLayout.SINGLE}'." +
                                " If you ignore this warning, OTP will ignore the mate number parsed from the filename.", "At least one mate number parsed from filename is not viable with the library layout."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Cannot extract mate number, because neither a ${MATE} column exists, nor can a mate number be parsed from filename 'testFileName.fastq.gz' using any pattern known to OTP, nor can one be implied from the library layout '${LibraryLayout.PAIRED}'.", "Cannot extract mate number"),
        ]
        when:
        new MateNumberFilenameValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when columns FASTQ_FILE and MATE, adds expected problems'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${MATE}\n" +
                        "NB_E_789_R.1.fastq.gz\t1\n" +
                        "NB_E_789_R.1.fastq.gz\t2\n" +
                        "NB_E_789_R.1.fastq.gz\tabc\n" +
                        "testFileName.fastq.gz\t1\n" +
                        "testFileName.fastq.gz\t2\n" +
                        "testFileName.fastq.gz\tabc\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING,
                        "The value '2' in the MATE column is different from the mate number '1' parsed from the filename 'NB_E_789_R.1.fastq.gz'. " +
                                "If you ignore this warning, OTP will use the mate number from the MATE column and ignore the value parsed from the filename.", "At least one value in the MATE column is different from the mate number parsed from the filename."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.WARNING,
                        "The value 'abc' in the MATE column is different from the mate number '1' parsed from the filename 'NB_E_789_R.1.fastq.gz'. " +
                                "If you ignore this warning, OTP will use the mate number from the MATE column and ignore the value parsed from the filename.", "At least one value in the MATE column is different from the mate number parsed from the filename."),
        ]
        when:
        new MateNumberFilenameValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when columns FASTQ_FILE, LIBRARY_LAYOUT and MATE, adds expected problems'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${FASTQ_FILE}\t${LIBRARY_LAYOUT}\t${MATE}\n" +
                        "NB_E_789_R.1.fastq.gz\t${LibraryLayout.SINGLE}\t1\n" +
                        "NB_E_789_R.1.fastq.gz\t${LibraryLayout.SINGLE}\t2\n" +
                        "NB_E_789_R.1.fastq.gz\t${LibraryLayout.SINGLE}\tabc\n" +
                        "NB_E_789_R.2.fastq.gz\t${LibraryLayout.SINGLE}\t1\n" +
                        "NB_E_789_R.2.fastq.gz\t${LibraryLayout.SINGLE}\t2\n" +
                        "NB_E_789_R.2.fastq.gz\t${LibraryLayout.SINGLE}\tabc\n" +
                        "testFileName.fastq.gz\t${LibraryLayout.SINGLE}\t1\n" +
                        "testFileName.fastq.gz\t${LibraryLayout.SINGLE}\t2\n" +
                        "testFileName.fastq.gz\t${LibraryLayout.SINGLE}\tabc\n" +
                        "NB_E_789_R.1.fastq.gz\ttestLibraryLayout\t1\n" +
                        "NB_E_789_R.1.fastq.gz\ttestLibraryLayout\t2\n" +
                        "NB_E_789_R.1.fastq.gz\ttestLibraryLayout\tabc\n" +
                        "testFileName.fastq.gz\ttestLibraryLayout\t1\n" +
                        "testFileName.fastq.gz\ttestLibraryLayout\t2\n" +
                        "testFileName.fastq.gz\ttestLibraryLayout\tabc\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[3].cells.findAll({it.columnAddress=="A" || it.columnAddress=="B"}) as Set, Level.WARNING,
                        "The mate number '2' parsed from filename 'NB_E_789_R.2.fastq.gz' is not viable with library layout '${LibraryLayout.SINGLE}'." +
                                " If you ignore this warning, OTP will ignore the mate number parsed from the filename.", "At least one mate number parsed from filename is not viable with the library layout."),
                new Problem(context.spreadsheet.dataRows[4].cells.findAll({it.columnAddress=="A" || it.columnAddress=="B"}) as Set, Level.WARNING,
                        "The mate number '2' parsed from filename 'NB_E_789_R.2.fastq.gz' is not viable with library layout '${LibraryLayout.SINGLE}'." +
                                " If you ignore this warning, OTP will ignore the mate number parsed from the filename.", "At least one mate number parsed from filename is not viable with the library layout."),
                new Problem(context.spreadsheet.dataRows[5].cells.findAll({it.columnAddress=="A" || it.columnAddress=="B"}) as Set, Level.WARNING,
                        "The mate number '2' parsed from filename 'NB_E_789_R.2.fastq.gz' is not viable with library layout '${LibraryLayout.SINGLE}'." +
                                " If you ignore this warning, OTP will ignore the mate number parsed from the filename.", "At least one mate number parsed from filename is not viable with the library layout."),
                new Problem(context.spreadsheet.dataRows[10].cells.findAll({it.columnAddress=="A" || it.columnAddress=="C"}) as Set, Level.WARNING,
                        "The value '2' in the MATE column is different from the mate number '1' parsed from the filename 'NB_E_789_R.1.fastq.gz'. " +
                                "If you ignore this warning, OTP will use the mate number from the MATE column and ignore the value parsed from the filename.", "At least one value in the MATE column is different from the mate number parsed from the filename."),
                new Problem(context.spreadsheet.dataRows[11].cells.findAll({it.columnAddress=="A" || it.columnAddress=="C"}) as Set, Level.WARNING,
                        "The value 'abc' in the MATE column is different from the mate number '1' parsed from the filename 'NB_E_789_R.1.fastq.gz'. " +
                                "If you ignore this warning, OTP will use the mate number from the MATE column and ignore the value parsed from the filename.", "At least one value in the MATE column is different from the mate number parsed from the filename."),
        ]
        when:
        new MateNumberFilenameValidator().validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}

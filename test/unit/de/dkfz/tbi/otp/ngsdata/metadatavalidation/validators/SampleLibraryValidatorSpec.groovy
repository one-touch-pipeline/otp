package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class SampleLibraryValidatorSpec extends Specification {

    void 'validate, when SAMPLE_ID and CUSTOMER_LIBRARY are missing, add error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column 'SAMPLE_ID' is missing.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when CUSTOMER_LIBRARY is missing, add warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\n" +
                "testSample\n" +
                "testSampleLib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING, "For sample 'testSampleLib' which contains 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the CUSTOMER_LIBRARY column.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when SAMPLE_ID is missing, add error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        "lib"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Mandatory column 'SAMPLE_ID' is missing.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, valid SAMPLE_ID and CUSTOMER_LIBRARY combinations, succeeds'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${CUSTOMER_LIBRARY}\n" +
                        "testSampleLib\tlib\n" +
                        "testSample\tlib\n" +
                        "testSample\n" +
                        "testLIbSample\tlib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when SAMPLE_ID does contain "lib" and CUSTOMER_LIBRARY is empty, adds warning'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SAMPLE_ID}\t${CUSTOMER_LIBRARY}\n" +
                        "testSampleLib\n"
        )

        when:
        new SampleLibraryValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "For sample 'testSampleLib' which contains 'lib', there should be a value in the ${CUSTOMER_LIBRARY} column.", "For samples which contain 'lib', there should be a value in the CUSTOMER_LIBRARY column.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }

}

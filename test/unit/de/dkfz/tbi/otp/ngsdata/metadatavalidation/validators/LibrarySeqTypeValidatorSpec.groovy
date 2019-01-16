package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class LibrarySeqTypeValidatorSpec extends Specification {

    void 'validate, when CUSTOMER_LIBRARY and SEQUENCING_TYPE are missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        context.problems.empty
    }

    void 'validate, when SEQUENCING_TYPE is missing, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\n" +
                        ""
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        context.problems.empty
    }

    @Unroll
    void 'validate, valid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #tagmentation, #library), succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "${library}\t${seqTypeName}\t${tagmentation}\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)

        then:
        context.problems.empty

        where:
        seqTypeName                             | tagmentation | library
        'seqtype'                               | ''           | ''
        'seqtype'                               | 'true'       | '1'
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | ''           | '4'
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | 'true'       | '4'

    }

    @SuppressWarnings('LineLength')
    @Unroll
    void 'validate, invalid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations (#seqTypeName, #tagmentation, #library), show warning'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "${library}\t${seqTypeName}\t${tagmentation}\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, message1, message2),
        ]
        assertContainSame(context.problems, expectedProblems)

        where:
        seqTypeName                             | tagmentation | library || message1                                                                                                                                                                || message2
        'seqtype'                               | ''           | '5'     || "The library '5' in column ${CUSTOMER_LIBRARY} indicates tagmentation, but the seqtype 'seqtype' is without tagmentation"                                               || LibrarySeqTypeValidator.LIBRARY_WITHOUT_TAGMENTATION
        'seqtype'                               | 'true'       | ''      || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column."                               || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | ''           | ''      || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column."                               || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY
        "seqtype${SeqType.TAGMENTATION_SUFFIX}" | 'true'       | ''      || "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}${SeqType.TAGMENTATION_SUFFIX}' there should be a value in the ${CUSTOMER_LIBRARY} column." || LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY

    }


    void 'validate, when CUSTOMER_LIBRARY is missing and SEQUENCING_TYPE has TAGMENTATION, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "seqtype\ttrue\n" +
                        "seqtype${SeqType.TAGMENTATION_SUFFIX}\t\n"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there " +
                                "should be a value in the ${CUSTOMER_LIBRARY} column.",
                        LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.WARNING,
                        "For the tagmentation sequencing type 'seqtype${SeqType.TAGMENTATION_SUFFIX}' there " +
                                "should be a value in the ${CUSTOMER_LIBRARY} column.",
                        LibrarySeqTypeValidator.TAGMENTATION_WITHOUT_LIBRARY),
        ]
        assertContainSame(context.problems, expectedProblems)
    }
}

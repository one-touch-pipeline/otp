package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.TestCase.*
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

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
                        "lib1"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        context.problems.empty
    }

    void 'validate, valid CUSTOMER_LIBRARY SEQUENCING_TYPE combinations, succeeds'() {
        given:

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "lib1\${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\ttrue\n" +
                        "\tseqType"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }


    void 'validate, when CUSTOMER_LIBRARY is empty and SEQUENCING_TYPE is WHOLE_GENOME_BISULFITE_TAGMENTATION, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${CUSTOMER_LIBRARY}\t${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "\t${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\ttrue"
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "For sequencing type '${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}' there should be a value in the ${CUSTOMER_LIBRARY} column.", "For specific sequencing types there should be a value in the CUSTOMER_LIBRARY column.")
        ]
        assertContainSame(context.problems, expectedProblems)
    }


    void 'validate, when CUSTOMER_LIBRARY is missing and SEQUENCING_TYPE is WHOLE_GENOME_BISULFITE_TAGMENTATION, adds warning'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${TAGMENTATION_BASED_LIBRARY}\n" +
                        "${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\ttrue\n" +
                        ""
        )

        when:
        new LibrarySeqTypeValidator().validate(context)


        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING, "For sequencing type '${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}' there should be a value in the ${CUSTOMER_LIBRARY} column.", "For specific sequencing types there should be a value in the CUSTOMER_LIBRARY column.")
        ]
        containSame(context.problems, expectedProblems)
    }
}

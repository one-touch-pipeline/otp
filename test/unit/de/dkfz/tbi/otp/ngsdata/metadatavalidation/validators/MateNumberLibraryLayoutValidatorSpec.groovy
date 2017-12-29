package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class MateNumberLibraryLayoutValidatorSpec extends Specification {


    void 'validate, when column(s) is/are missing, adds error(s)'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${header}
value1\tvalue2
""")

        when:
        new MateNumberLibraryLayoutValidator().validate(context)

        then:
        containSame(context.problems*.message, messages)

        where:
        header || messages
        "${MetaDataColumn.MATE.name()}\tlayout" || ["Mandatory column 'LIBRARY_LAYOUT' is missing."]
        "nomate\t${MetaDataColumn.LIBRARY_LAYOUT.name()}" || []
        "nomate\tlayout" || ["Mandatory column 'LIBRARY_LAYOUT' is missing."]
    }

    void 'validate, all are fine'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}\t${MetaDataColumn.LIBRARY_LAYOUT}
1\t${LibraryLayout.PAIRED}
2\t${LibraryLayout.PAIRED}
1\t${LibraryLayout.MATE_PAIR}
2\t${LibraryLayout.MATE_PAIR}
1\t${LibraryLayout.SINGLE}
""")

        when:
        new MateNumberLibraryLayoutValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.MATE}\t${MetaDataColumn.LIBRARY_LAYOUT}
1\tUnknownLibrary
3\t${LibraryLayout.PAIRED}
3\t${LibraryLayout.MATE_PAIR}
2\t${LibraryLayout.SINGLE}
-1\t${LibraryLayout.PAIRED}
abc\t${LibraryLayout.PAIRED}
""")

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "OTP does not know the library layout 'UnknownLibrary' and can therefore not validate the mate number.", "OTP does not know at least one library layout and can therefore not validate the mate number."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The mate number '3' is bigger then the allowed value for the library layout '${LibraryLayout.PAIRED}' of '2'.", "At least one mate number is bigger then the allowed value for the library layout."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The mate number '3' is bigger then the allowed value for the library layout '${LibraryLayout.MATE_PAIR}' of '2'.", "At least one mate number is bigger then the allowed value for the library layout."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The mate number '2' is bigger then the allowed value for the library layout '${LibraryLayout.SINGLE}' of '1'.", "At least one mate number is bigger then the allowed value for the library layout."),
        ] as Set

        when:
        new MateNumberLibraryLayoutValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}

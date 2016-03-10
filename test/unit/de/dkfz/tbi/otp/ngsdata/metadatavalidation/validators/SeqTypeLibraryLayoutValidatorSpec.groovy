package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem
import grails.test.mixin.Mock
import spock.lang.Specification

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

@Mock(SeqType)
class SeqTypeLibraryLayoutValidatorSpec extends Specification {

    void 'validate, when column(s) is/are missing, adds error(s)'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${header}
value1\tvalue2
""")

        when:
        new SeqTypeLibraryLayoutValidator().validate(context)

        then:
        containSame(context.problems*.message, messages)

        where:
        header || messages
        "${MetaDataColumn.SEQUENCING_TYPE.name()}\tlayout" || ["Mandatory column 'LIBRARY_LAYOUT' is missing."]
        "seqtype\t${MetaDataColumn.LIBRARY_LAYOUT.name()}" || ["Mandatory column 'SEQUENCING_TYPE' is missing."]
        "seqtype\tlayout" || ["Mandatory column 'SEQUENCING_TYPE' is missing.", "Mandatory column 'LIBRARY_LAYOUT' is missing."]
    }


    void 'validate, when combinations are in database, adds no problem'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}
SeqType1\tLibraryLayout1
SeqType1\tLibraryLayout2
SeqType2\tLibraryLayout1
SeqType2\tLibraryLayout2
""")
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout2')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout2')

        when:
        new SeqTypeLibraryLayoutValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when combinations are not in database, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}
SeqType1\tLibraryLayout1
SeqType1\tLibraryLayout2
SeqType1\tLibraryLayout3
SeqType2\tLibraryLayout1
SeqType2\tLibraryLayout2
SeqType2\tLibraryLayout3
SeqType3\tLibraryLayout1
SeqType3\tLibraryLayout2
SeqType3\tLibraryLayout3
""")
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout2')

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout2' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout3' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout3' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout1' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout2' is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout3' is not registered in the OTP database."),
        ]

        when:
        new SeqTypeLibraryLayoutValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import grails.test.mixin.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

@Mock(SeqType)
class SeqTypeLibraryLayoutValidatorSpec extends Specification {

    void 'validate, when context is BamMetadataValidationContext, gets expected columns'() {

        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext()

        when:
        List<String> columns = new SeqTypeLibraryLayoutValidator().getColumnTitles(context)

        then:
        columns == [BamMetadataColumn.SEQUENCING_TYPE.name(), BamMetadataColumn.LIBRARY_LAYOUT.name()]
    }

    void 'validate, when context is MetadataValidationContext, gets expected columns'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        List<String> columns = new SeqTypeLibraryLayoutValidator().getColumnTitles(context)

        then:
        columns == [MetaDataColumn.SEQUENCING_TYPE.name(), MetaDataColumn.LIBRARY_LAYOUT.name(), MetaDataColumn.TAGMENTATION_BASED_LIBRARY.name()]
    }

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
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}
SeqType1\tLibraryLayout1\t
SeqType1\tLibraryLayout2\t
SeqType2\tLibraryLayout1\t
SeqType2\tLibraryLayout2\t
SeqType2\tLibraryLayout2\ttrue
""")
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout2')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout2')
        DomainFactory.createSeqType(name: 'SeqType2_TAGMENTATION', libraryLayout: 'LibraryLayout2')

        when:
        new SeqTypeLibraryLayoutValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when combinations are not in database, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}
SeqType1\tLibraryLayout1\t
SeqType1\tLibraryLayout2\t
SeqType1\tLibraryLayout3\t
SeqType2\tLibraryLayout1\t
SeqType2\tLibraryLayout2\t
SeqType2\tLibraryLayout3\t
SeqType3\tLibraryLayout1\t
SeqType3\tLibraryLayout2\t
SeqType3\tLibraryLayout3\t
SeqType3\tLibraryLayout3\ttrue
""")
        DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')
        DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout2')

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout2' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout3' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout1' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout3' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout1' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout2' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout3' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[9].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3_TAGMENTATION' and library layout 'LibraryLayout3' is not registered in the OTP database.", "At least one combination of sequencing type and library layout is not registered in the OTP database."),
        ]

        when:
        new SeqTypeLibraryLayoutValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}

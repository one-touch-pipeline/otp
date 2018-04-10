package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import de.dkfz.tbi.*
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
        columns == [MetaDataColumn.SEQUENCING_TYPE.name(), MetaDataColumn.LIBRARY_LAYOUT.name(), MetaDataColumn.BASE_MATERIAL.name(), MetaDataColumn.TAGMENTATION_BASED_LIBRARY.name()]
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
        "seqtype\t${MetaDataColumn.BASE_MATERIAL.name()}" || ["Mandatory column 'SEQUENCING_TYPE' is missing.", "Mandatory column 'LIBRARY_LAYOUT' is missing."]
        "seqtype\tlayout" || ["Mandatory column 'SEQUENCING_TYPE' is missing.", "Mandatory column 'LIBRARY_LAYOUT' is missing."]
    }

    void 'validate, when combinations are in database, adds no problem'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
SeqType1\tLibraryLayout1\t
SeqType1\tLibraryLayout2\t
SeqType2\tLibraryLayout1\t
SeqType2\tLibraryLayout2\t
SeqType2\tLibraryLayout2\ttrue
SeqType3\tLibraryLayout3\t\t${SeqType.SINGLE_CELL_RNA}
SeqType3\tLibraryLayout3\t\t${SeqType.SINGLE_CELL_DNA}
""")
        SeqType seqType1ll1 = DomainFactory.createSeqType(name: 'SeqType1', dirName: 'NameSeqType1', libraryLayout: 'LibraryLayout1', singleCell: false)
        SeqType seqType1ll2 = DomainFactory.createSeqType(name: 'SeqType1', dirName: 'NameSeqType1', libraryLayout: 'LibraryLayout2', singleCell: false)
        SeqType seqType2ll1 = DomainFactory.createSeqType(name: 'SeqType2', dirName: 'NameSeqType2', libraryLayout: 'LibraryLayout1', singleCell: false)
        SeqType seqType2ll2 = DomainFactory.createSeqType(name: 'SeqType2', dirName: 'NameSeqType2', libraryLayout: 'LibraryLayout2', singleCell: false)
        SeqType seqType3ll3True = DomainFactory.createSeqType(name: 'SeqType3', dirName: 'NameSeqType3', libraryLayout: 'LibraryLayout3', singleCell: true)
        SeqType seqType2Tagll2 = DomainFactory.createSeqType(name: 'SeqType2_TAGMENTATION', dirName: 'SeqType2_TAGMENTATION', libraryLayout: 'LibraryLayout2', singleCell: false)

        when:
        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: 'LibraryLayout1', singleCell: false]) >> seqType1ll1
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> seqType1ll2
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout1', singleCell: false]) >> seqType2ll1
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> seqType2ll2
            2 * findByNameOrImportAlias('SeqType3', [libraryLayout: 'LibraryLayout3', singleCell: true]) >> seqType3ll3True
            1 * findByNameOrImportAlias('SeqType2_TAGMENTATION', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> seqType2Tagll2
            2 * findByNameOrImportAlias('SeqType1') >> seqType1ll1
            2 * findByNameOrImportAlias('SeqType2') >> seqType2ll1
            1 * findByNameOrImportAlias('SeqType2_TAGMENTATION') >> seqType2Tagll2
            2 * findByNameOrImportAlias('SeqType3') >> seqType3ll3True
            0 * _
        }
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when precondition are not valid, add no problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
SeqTypeUnknown\tLibraryLayout1\t\twer
SeqType1\tLibraryLayoutUnknown\t\twer
\tLibraryLayout1\t\twer
SeqType1\t\t\twer
""")
        SeqType seqType = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')

        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqTypeUnknown') >> null
            1 * findByNameOrImportAlias('SeqType1') >> seqType
        }

        when:
        validator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when combinations are not in database, adds expected errors'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
SeqType1\tLibraryLayout1\t\twer
SeqType1\tLibraryLayout2\t\twer
SeqType1\tLibraryLayout3\t\twer
SeqType2\tLibraryLayout1\t\twer
SeqType2\tLibraryLayout2\t\twer
SeqType2\tLibraryLayout3\t\trt
SeqType3\tLibraryLayout1\t\tert
SeqType3\tLibraryLayout2\t\tewer
SeqType3\tLibraryLayout3\t\twer
SeqType3\tLibraryLayout3\ttrue\terwr
SeqType3\tLibraryLayout2\ttrue\t${SeqType.SINGLE_CELL_DNA}
SeqType2\tLibraryLayout2\t\t${SeqType.SINGLE_CELL_RNA}
""")

        SeqType seqType1ll1 = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: 'LibraryLayout1')
        SeqType seqType2ll2 = DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: 'LibraryLayout2')
        SeqType seqType3 = DomainFactory.createSeqType(name: 'SeqType3')
        SeqType seqType3Tag =  DomainFactory.createSeqType(name: 'SeqType3_TAGMENTATION')
        DomainFactory.createSeqType(libraryLayout: 'LibraryLayout3')


        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: 'LibraryLayout1', singleCell: false]) >> seqType1ll1
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: 'LibraryLayout3', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout1', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> seqType2ll2
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout3', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: 'LibraryLayout1', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: 'LibraryLayout2', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: 'LibraryLayout3', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3_TAGMENTATION', [libraryLayout: 'LibraryLayout3', singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3_TAGMENTATION', [libraryLayout: 'LibraryLayout2', singleCell: true]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: 'LibraryLayout2', singleCell: true]) >> null
            3 * findByNameOrImportAlias('SeqType1') >> seqType1ll1
            4 * findByNameOrImportAlias('SeqType2') >> seqType2ll2
            3 * findByNameOrImportAlias('SeqType3') >> seqType3
            2 * findByNameOrImportAlias('SeqType3_TAGMENTATION') >> seqType3Tag
            0 * _
        }
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout2' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout 'LibraryLayout3' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout1' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout3' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout1' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout2' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout 'LibraryLayout3' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[9].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3_TAGMENTATION' and library layout 'LibraryLayout3' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[10].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3_TAGMENTATION' and library layout 'LibraryLayout2' and Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[11].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout 'LibraryLayout2' and Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and Single Cell is not registered in the OTP database."),
        ]

        when:
        validator.validate(context)


        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}

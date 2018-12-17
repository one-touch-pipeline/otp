package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

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
SeqType1\t${LibraryLayout.SINGLE}\t
SeqType1\t${LibraryLayout.PAIRED}\t
SeqType2\t${LibraryLayout.SINGLE}\t
SeqType2\t${LibraryLayout.PAIRED}\t
SeqType2\t${LibraryLayout.PAIRED}\ttrue
SeqType3\t${LibraryLayout.MATE_PAIR}\t\t${SeqType.SINGLE_CELL_RNA}
SeqType3\t${LibraryLayout.MATE_PAIR}\t\t${SeqType.SINGLE_CELL_DNA}
""")
        SeqType seqType1ll1 = DomainFactory.createSeqType(name: 'SeqType1', dirName: 'NameSeqType1', libraryLayout: LibraryLayout.SINGLE, singleCell: false)
        SeqType seqType1ll2 = DomainFactory.createSeqType(name: 'SeqType1', dirName: 'NameSeqType1', libraryLayout: LibraryLayout.PAIRED, singleCell: false)
        SeqType seqType2ll1 = DomainFactory.createSeqType(name: 'SeqType2', dirName: 'NameSeqType2', libraryLayout: LibraryLayout.SINGLE, singleCell: false)
        SeqType seqType2ll2 = DomainFactory.createSeqType(name: 'SeqType2', dirName: 'NameSeqType2', libraryLayout: LibraryLayout.PAIRED, singleCell: false)
        SeqType seqType3ll3True = DomainFactory.createSeqType(name: 'SeqType3', dirName: 'NameSeqType3', libraryLayout: LibraryLayout.MATE_PAIR, singleCell: true)
        SeqType seqType2Tagll2 = DomainFactory.createSeqType(name: 'SeqType2_TAGMENTATION', dirName: 'SeqType2_TAGMENTATION', libraryLayout: LibraryLayout.PAIRED, singleCell: false)

        when:
        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> seqType1ll1
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> seqType1ll2
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> seqType2ll1
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> seqType2ll2
            2 * findByNameOrImportAlias('SeqType3', [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: true]) >> seqType3ll3True
            1 * findByNameOrImportAlias('SeqType2_TAGMENTATION', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> seqType2Tagll2
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
SeqTypeUnknown\t${LibraryLayout.SINGLE}\t\twer
SeqType1\tLibraryLayoutUnknown\t\twer
\t${LibraryLayout.SINGLE}\t\twer
SeqType1\t\t\twer
SeqType1\t${LibraryLayout.SINGLE}\t\twer
""")
        SeqType seqType = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: LibraryLayout.SINGLE)

        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqTypeUnknown') >> null
            1 * findByNameOrImportAlias('SeqType1') >> seqType
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> seqType
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
SeqType1\t${LibraryLayout.SINGLE}\t\twer
SeqType1\t${LibraryLayout.PAIRED}\t\twer
SeqType1\t${LibraryLayout.MATE_PAIR}\t\twer
SeqType2\t${LibraryLayout.SINGLE}\t\twer
SeqType2\t${LibraryLayout.PAIRED}\t\twer
SeqType2\t${LibraryLayout.MATE_PAIR}\t\trt
SeqType3\t${LibraryLayout.SINGLE}\t\tert
SeqType3\t${LibraryLayout.PAIRED}\t\tewer
SeqType3\t${LibraryLayout.MATE_PAIR}\t\twer
SeqType3\t${LibraryLayout.MATE_PAIR}\ttrue\terwr
SeqType3\t${LibraryLayout.PAIRED}\ttrue\t${SeqType.SINGLE_CELL_DNA}
SeqType2\t${LibraryLayout.PAIRED}\t\t${SeqType.SINGLE_CELL_RNA}
""")

        SeqType seqType1ll1 = DomainFactory.createSeqType(name: 'SeqType1', libraryLayout: LibraryLayout.SINGLE)
        SeqType seqType2ll2 = DomainFactory.createSeqType(name: 'SeqType2', libraryLayout: LibraryLayout.PAIRED)
        SeqType seqType3 = DomainFactory.createSeqType(name: 'SeqType3')
        SeqType seqType3Tag =  DomainFactory.createSeqType(name: 'SeqType3_TAGMENTATION')
        DomainFactory.createSeqType(libraryLayout: LibraryLayout.MATE_PAIR)


        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()
        validator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> seqType1ll1
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType1', [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> seqType2ll2
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3', [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3_TAGMENTATION', [libraryLayout: LibraryLayout.MATE_PAIR, singleCell: false]) >> null
            1 * findByNameOrImportAlias('SeqType3_TAGMENTATION', [libraryLayout: LibraryLayout.PAIRED, singleCell: true]) >> null
            1 * findByNameOrImportAlias('SeqType2', [libraryLayout: LibraryLayout.PAIRED, singleCell: true]) >> null
            3 * findByNameOrImportAlias('SeqType1') >> seqType1ll1
            4 * findByNameOrImportAlias('SeqType2') >> seqType2ll2
            3 * findByNameOrImportAlias('SeqType3') >> seqType3
            2 * findByNameOrImportAlias('SeqType3_TAGMENTATION') >> seqType3Tag
            0 * _
        }
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout '${LibraryLayout.PAIRED}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType1' and library layout '${LibraryLayout.MATE_PAIR}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout '${LibraryLayout.SINGLE}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout '${LibraryLayout.MATE_PAIR}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout '${LibraryLayout.SINGLE}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout '${LibraryLayout.PAIRED}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3' and library layout '${LibraryLayout.MATE_PAIR}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[9].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3_TAGMENTATION' and library layout '${LibraryLayout.MATE_PAIR}' and without Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and without Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[10].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType3_TAGMENTATION' and library layout '${LibraryLayout.PAIRED}' and Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and Single Cell is not registered in the OTP database."),
                new Problem(context.spreadsheet.dataRows[11].cells as Set, Level.ERROR,
                        "The combination of sequencing type 'SeqType2' and library layout '${LibraryLayout.PAIRED}' and Single Cell is not registered in the OTP database.", "At least one combination of sequencing type and library layout and Single Cell is not registered in the OTP database."),
        ]

        when:
        validator.validate(context)


        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }
}

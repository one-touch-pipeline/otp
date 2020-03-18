/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()

        when:
        List<String> columns = validator.getRequiredColumnTitles(context) + validator.getOptionalColumnTitles(context)

        then:
        columns == [BamMetadataColumn.SEQUENCING_TYPE.name(), BamMetadataColumn.SEQUENCING_READ_TYPE.name()]
    }

    void 'validate, when context is MetadataValidationContext, gets expected columns'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()
        SeqTypeLibraryLayoutValidator validator = new SeqTypeLibraryLayoutValidator()

        when:
        List<String> columns = validator.getRequiredColumnTitles(context) + validator.getOptionalColumnTitles(context)

        then:
        columns == [MetaDataColumn.SEQUENCING_TYPE.name(), MetaDataColumn.SEQUENCING_READ_TYPE.name(), MetaDataColumn.BASE_MATERIAL.name(), MetaDataColumn.TAGMENTATION_BASED_LIBRARY.name()]
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
        "${MetaDataColumn.SEQUENCING_TYPE.name()}\tlayout" || ["Required column 'SEQUENCING_READ_TYPE' is missing."]
        "seqtype\t${MetaDataColumn.SEQUENCING_READ_TYPE.name()}" || ["Required column 'SEQUENCING_TYPE' is missing."]
        "seqtype\t${MetaDataColumn.BASE_MATERIAL.name()}" || ["Required column 'SEQUENCING_TYPE' is missing.", "Required column 'SEQUENCING_READ_TYPE' is missing."]
        "seqtype\tlayout" || ["Required column 'SEQUENCING_TYPE' is missing.", "Required column 'SEQUENCING_READ_TYPE' is missing."]
    }

    void 'validate, when combinations are in database, adds no problem'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.SEQUENCING_READ_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
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

        Collection<Problem> expectedProblems = [
                createInfoForSeqType([
                        seqType1ll1,
                        seqType1ll2,
                        seqType2ll1,
                        seqType2ll2,
                        seqType3ll3True,
                        seqType2Tagll2,
                ]),
        ]

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
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when precondition are not valid, add no problems'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.SEQUENCING_READ_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
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

        Collection<Problem> expectedProblems = [
                createInfoForSeqType([seqType,]),
        ]

        when:
        validator.validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    void 'validate, when combinations are not in database, adds expected errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.SEQUENCING_READ_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\t${MetaDataColumn.BASE_MATERIAL}
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
                createInfoForSeqType([seqType1ll1, seqType2ll2]),
        ]

        when:
        validator.validate(context)

        then:
        TestCase.assertContainSame(context.problems, expectedProblems)
    }

    private Problem createInfoForSeqType(List<SeqType> seqTypes) {
        String infoSeqType = "The submission contains following seqTypes:\n- ${seqTypes*.toString().sort().join('\n- ')}"
        return new Problem([] as Set, Level.INFO, infoSeqType, infoSeqType)
    }
}

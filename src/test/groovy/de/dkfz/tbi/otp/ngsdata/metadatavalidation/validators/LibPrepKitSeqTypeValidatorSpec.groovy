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


import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class LibPrepKitSeqTypeValidatorSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            LibraryPreparationKit,
            SeqType,
    ]}

    static final String VALID_METADATA =
            "${MetaDataColumn.LIB_PREP_KIT}\t${MetaDataColumn.LIBRARY_LAYOUT}\t${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.TAGMENTATION_BASED_LIBRARY}\n" +
                    "lib_prep_kit\t${LibraryLayout.PAIRED}\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${LibraryLayout.PAIRED}\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                    "lib_prep_kit\t${LibraryLayout.SINGLE}\t${SeqTypeNames.RNA.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${LibraryLayout.PAIRED}\t${SeqTypeNames.RNA.seqTypeName}\t\n" +
                    "lib_prep_kit\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}\t\n" +
                    "lib_prep_kit\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\n"+
                    "lib_prep_kit\t${LibraryLayout.PAIRED}\t${SeqTypeNames.CHIP_SEQ.seqTypeName}\t\n" +
                    "${InformationReliability.UNKNOWN_VERIFIED.rawValue}\t${LibraryLayout.PAIRED}\t${SeqTypeNames.CHIP_SEQ.seqTypeName}\t\n"

    void setup() {
        DomainFactory.createLibraryPreparationKit(name: 'lib_prep_kit')
        DomainFactory.createAllAlignableSeqTypes()
    }


    void 'validate, when sequencing type is exome, RNA, WGBS, WGBSTag or ChipSeq and LibPrepKit is valid'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA
        )

        when:
        LibPrepKitSeqTypeValidator libPrepKitSeqTypeValidator = new LibPrepKitSeqTypeValidator()
        libPrepKitSeqTypeValidator.seqTypeService = Mock(SeqTypeService) {
            2 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.exomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> SeqTypeService.rnaSingleSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.rnaPairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.chipSeqPairedSeqType
        }
        libPrepKitSeqTypeValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is exome, RNA, WGBS, WGBSTag or ChipSeq and LibPrepKit is empty, adds error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.EXOME.seqTypeName}\t\n" +
                        "\t${LibraryLayout.SINGLE}\t${SeqTypeNames.RNA.seqTypeName}\t\n" +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t\n" +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}\t\n" +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.CHIP_SEQ.seqTypeName}\t\n"
        )

        when:
        LibPrepKitSeqTypeValidator libPrepKitSeqTypeValidator = new LibPrepKitSeqTypeValidator()
        libPrepKitSeqTypeValidator.seqTypeService = Mock(SeqTypeService) {
            3 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.exomePairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> SeqTypeService.rnaSingleSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.rnaPairedSeqType
            3 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            3 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
            3 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.chipSeqPairedSeqType
        }
        libPrepKitSeqTypeValidator.validate(context)

        then:
        context.problems.size() == 5
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[10].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(10).cells.get(2).text} ${context.spreadsheet.dataRows.get(10).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[11].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(11).cells.get(2).text} ${context.spreadsheet.dataRows.get(11).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[12].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(12).cells.get(2).text} ${context.spreadsheet.dataRows.get(12).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[13].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(13).cells.get(2).text} ${context.spreadsheet.dataRows.get(13).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[14].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(14).cells.get(2).text} ${context.spreadsheet.dataRows.get(14).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing type is not exome'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                VALID_METADATA +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t\n" +
                        "\t${LibraryLayout.PAIRED}\t${SeqTypeNames.EXOME.seqTypeName}\ttrue\n"
        )

        when:
        LibPrepKitSeqTypeValidator libPrepKitSeqTypeValidator = new LibPrepKitSeqTypeValidator()
        libPrepKitSeqTypeValidator.seqTypeService = Mock(SeqTypeService) {
            2 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.exomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.SINGLE, singleCell: false]) >> SeqTypeService.rnaSingleSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.rnaPairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfiteTagmentationPairedSeqType
            2 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.chipSeqPairedSeqType
        }
        libPrepKitSeqTypeValidator.validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is not exome and no libPrepKit column exist, succeeds without problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}
WGS
WGBS
CHIPSEQ
""")

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, when sequencing type is exome, RNA, WGS, WGBS or ChipSeq and no libPrepKit column exist, adds one error'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.SEQUENCING_TYPE}\t${MetaDataColumn.LIBRARY_LAYOUT}
${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.EXOME.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.WHOLE_GENOME.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.EXOME.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.RNA.seqTypeName}\t${LibraryLayout.PAIRED}
${SeqTypeNames.CHIP_SEQ.seqTypeName}\t${LibraryLayout.PAIRED}
""")

        when:
        LibPrepKitSeqTypeValidator libPrepKitSeqTypeValidator = new LibPrepKitSeqTypeValidator()
        libPrepKitSeqTypeValidator.seqTypeService = Mock(SeqTypeService) {
            1 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.wholeGenomeBisulfitePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.EXOME.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.exomePairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.RNA.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.rnaPairedSeqType
            1 * findByNameOrImportAlias(SeqTypeNames.CHIP_SEQ.seqTypeName, [libraryLayout: LibraryLayout.PAIRED, singleCell: false]) >> SeqTypeService.chipSeqPairedSeqType
        }
        libPrepKitSeqTypeValidator.validate(context)

        then:
        context.problems.size() == 4
        Collection<Problem> expectedProblems = [
                new Problem((context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[4].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(1).cells.get(0).text} ${context.spreadsheet.dataRows.get(1).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[2].cells + context.spreadsheet.dataRows[5].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(2).cells.get(0).text} ${context.spreadsheet.dataRows.get(2).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[6].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(6).cells.get(0).text} ${context.spreadsheet.dataRows.get(6).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
                new Problem((context.spreadsheet.dataRows[7].cells) as Set, Level.ERROR,
                        "If the sequencing type is '${context.spreadsheet.dataRows.get(7).cells.get(0).text} ${context.spreadsheet.dataRows.get(7).cells.get(1).text}'" +
                                ", the library preparation kit must be given."),
        ]
        containSame(context.problems, expectedProblems)
    }

    void 'validate, when sequencing type column missing, succeeds without problems'() {

        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext()

        when:
        new LibPrepKitSeqTypeValidator().validate(context)

        then:
        context.problems.empty
    }
}

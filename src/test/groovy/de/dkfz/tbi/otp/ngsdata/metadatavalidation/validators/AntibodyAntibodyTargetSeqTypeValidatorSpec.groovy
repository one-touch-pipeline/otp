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


import spock.lang.*

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class AntibodyAntibodyTargetSeqTypeValidatorSpec extends Specification implements DomainFactoryCore {


    @Unroll
    void 'validate, when  #name, then validation do not return problems'() {
        given:
        SeqType seqType = new SeqType([
                name             : 'seqType',
                displayName      : 'seqType',
                libraryLayout    : LibraryLayout.PAIRED,
                hasAntibodyTarget: hasAntibodyTarget,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${LIBRARY_LAYOUT},${ANTIBODY_TARGET},${ANTIBODY}
${seqType.seqTypeName},${seqType.libraryLayout},${antibodyTarget},${antibody}
""".replaceAll(',', '\t')
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator([
                seqTypeService: Mock(SeqTypeService) {
                    1 * findByNameOrImportAlias(_, _) >> seqType
                }
        ]).validate(context)

        then:
        context.problems.empty

        where:
        name                                                                         | hasAntibodyTarget | antibodyTarget    | antibody
        'seq type do not support antibody target and no antibody is given'           | false             | ''                | ''
        'seq type require antibody target and only antibody target is given'         | true              | 'some text'       | ''
        'seq type require antibody target and antibody target and antibody is given' | true              | 'antibody target' | 'antibody'
    }


    @Unroll
    void 'validate, when  #name, then validation return extpected problems'() {
        given:
        SeqType seqType = new SeqType([
                name             : 'seqType',
                displayName      : 'seqType',
                libraryLayout    : LibraryLayout.PAIRED,
                hasAntibodyTarget: hasAntibodyTarget,
        ])

        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${SEQUENCING_TYPE},${LIBRARY_LAYOUT},${ANTIBODY_TARGET},${ANTIBODY}
${seqType.seqTypeName},${seqType.libraryLayout},${antibodyTarget},${antibody}
""".replaceAll(',', '\t')
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, level, message, message2),
        ]

        when:
        new AntibodyAntibodyTargetSeqTypeValidator([
                seqTypeService: Mock(SeqTypeService) {
                    1 * findByNameOrImportAlias(_, _) >> seqType
                }
        ]).validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)

        where:
        name                                                                       | hasAntibodyTarget | antibodyTarget    | antibody   || level         | message                                                                                                                                                                     | message2
        'seq type require antibody target and no antibody is given'                | true              | ''                | ''         || Level.ERROR   | "Antibody target is not provided although the SeqType 'seqType PAIRED bulk' require it."                                                                                    | "Antibody target is not provided for SeqType require AntibodyTarget"
        'seq type do not support target and only antibody target is given'         | false             | 'antibody target' | ''         || Level.WARNING | "Antibody target ('antibody target') and/or antibody ('') are/is provided although the SeqType 'seqType PAIRED bulk do not support it. OTP will ignore the values."         | "Antibody target and/or antibody are/is provided for an SeqType not supporting it. OTP will ignore the values."
        'seq type do not support target and antibody target and antibody is given' | false             | 'antibody target' | 'antibody' || Level.WARNING | "Antibody target ('antibody target') and/or antibody ('antibody') are/is provided although the SeqType 'seqType PAIRED bulk do not support it. OTP will ignore the values." | "Antibody target and/or antibody are/is provided for an SeqType not supporting it. OTP will ignore the values."
        'seq type do not support target and only antibody is given'                | false             | ''                | 'antibody' || Level.WARNING | "Antibody target ('') and/or antibody ('antibody') are/is provided although the SeqType 'seqType PAIRED bulk do not support it. OTP will ignore the values."                | "Antibody target and/or antibody are/is provided for an SeqType not supporting it. OTP will ignore the values."
    }


    void 'validate, when no ANTIBODY_TARGET and ANTIBODY column exist and seqType do not require antibody Target, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${LIBRARY_LAYOUT}\n" +
                        "some seqType\tPAIRED"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator([
                seqTypeService: Mock(SeqTypeService) {
                    1 * findByNameOrImportAlias(_, _) >> new SeqType()
                }
        ]).validate(context)


        then:
        context.problems.empty
    }


    void 'validate, when no ANTIBODY column exists and seqType require AntibodyTarget and AntibodyTarget is given, succeeds'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${LIBRARY_LAYOUT}\t${ANTIBODY_TARGET}\n" +
                        "ChIP Seq\tPAIRED\tsome_antibody_target"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator([
                seqTypeService: Mock(SeqTypeService) {
                    1 * findByNameOrImportAlias(_, _) >> new SeqType(hasAntibodyTarget: true)
                }
        ]).validate(context)


        then:
        assertContainSame(context.problems, [])
    }

    void 'validate, when no ANTIBODY_TARGET column exists and seqType require AntibodyTarget, adds error'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${SEQUENCING_TYPE}\t${LIBRARY_LAYOUT}\t${ANTIBODY}\n" +
                        "ChIP Seq\tPAIRED\tsome_antibody"
        )

        when:
        new AntibodyAntibodyTargetSeqTypeValidator([
                seqTypeService: Mock(SeqTypeService) {
                    1 * findByNameOrImportAlias(_, _) >> new SeqType(hasAntibodyTarget: true, displayName: 'seqType', libraryLayout: LibraryLayout.PAIRED)
                }
        ]).validate(context)

        then:
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "Antibody target is not provided although the SeqType 'seqType PAIRED bulk' require it.",
                "Antibody target is not provided for SeqType require AntibodyTarget"),
        ]
        assertContainSame(context.problems, expectedProblems)
    }

}

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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class MateNumberLibraryLayoutValidatorSpec extends Specification {


    @Unroll
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
        "${MetaDataColumn.READ.name()}\tlayout" || ["Required column 'SEQUENCING_READ_TYPE' is missing."]
        "nomate\t${MetaDataColumn.SEQUENCING_READ_TYPE.name()}" || []
        "nomate\tlayout" || ["Required column 'SEQUENCING_READ_TYPE' is missing."]
    }

    void 'validate, all are fine'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.READ}\t${MetaDataColumn.SEQUENCING_READ_TYPE}
1\t${SequencingReadType.PAIRED}
2\t${SequencingReadType.PAIRED}
1\t${SequencingReadType.MATE_PAIR}
2\t${SequencingReadType.MATE_PAIR}
1\t${SequencingReadType.SINGLE}
i1\t${SequencingReadType.PAIRED}
i2\t${SequencingReadType.PAIRED}
i3\\t${SequencingReadType.PAIRED}
i1\t${SequencingReadType.MATE_PAIR}
i2\t${SequencingReadType.MATE_PAIR}
i3\\t${SequencingReadType.MATE_PAIR}
i1\t${SequencingReadType.SINGLE}
i2\\t${SequencingReadType.SINGLE}
i3\\t${SequencingReadType.SINGLE}
""")

        when:
        new MateNumberLibraryLayoutValidator().validate(context)

        then:
        context.problems.empty
    }

    void 'validate, adds expected errors'() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext("""\
${MetaDataColumn.READ}\t${MetaDataColumn.SEQUENCING_READ_TYPE}
1\tUnknownLibrary
3\t${SequencingReadType.PAIRED}
3\t${SequencingReadType.MATE_PAIR}
2\t${SequencingReadType.SINGLE}
-1\t${SequencingReadType.PAIRED}
abc\t${SequencingReadType.PAIRED}
ijk\t${SequencingReadType.PAIRED}
i\t${SequencingReadType.PAIRED}
""")

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.WARNING,
                        "OTP does not know the sequencing read type 'UnknownLibrary' and can therefore not validate the mate number.", "OTP does not know at least one sequencing read type and can therefore not validate the mate number."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The mate number '3' is bigger then the allowed value for the sequencing read type '${SequencingReadType.PAIRED}' of '2'.", "At least one mate number is bigger then the allowed value for the sequencing read type."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The mate number '3' is bigger then the allowed value for the sequencing read type '${SequencingReadType.MATE_PAIR}' of '2'.", "At least one mate number is bigger then the allowed value for the sequencing read type."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The mate number '2' is bigger then the allowed value for the sequencing read type '${SequencingReadType.SINGLE}' of '1'.", "At least one mate number is bigger then the allowed value for the sequencing read type."),
        ] as Set

        when:
        new MateNumberLibraryLayoutValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }
}

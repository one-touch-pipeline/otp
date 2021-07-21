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

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInGpcfSpecificStructure
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators.OnlyOneRunValidatorSpec.Ilse.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class OnlyOneRunValidatorSpec extends Specification {

    @Unroll
    void "test validate"(boolean containsMultipleRuns, boolean isDataFilesOnGpcfMidTerm, Ilse ilse, boolean error) {
        given:
        String header = "${MetaDataColumn.RUN_ID}\n"
        String firstData = ""
        String secondData = ""
        if (ilse != NONE) {
            header = "${MetaDataColumn.ILSE_NO}\t" + header
            switch (ilse) {
                case EMPTY:
                    firstData = "\t"
                    secondData = "\t"
                    break
                case DIFFERENT:
                    firstData = "ilse1\t"
                    secondData = "ilse2\t"
                    break
                case SAME:
                    firstData = "ilse1\t"
                    secondData = "ilse1\t"
                    break
            }
        }
        firstData += "run1\n"
        secondData += containsMultipleRuns ? "run2\n" : "run1\n"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                header + firstData + secondData,
                [directoryStructure: isDataFilesOnGpcfMidTerm ? new DataFilesInGpcfSpecificStructure() : [:]],
        )

        when:
        new OnlyOneRunValidator().validate(context)

        then:
        if (error) {
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == LogLevel.WARNING
            List<String> affectedCells = ilse == NONE ? ['A2', 'A3'] : ['B2', 'B3']
            assert containSame(problem.affectedCells*.cellAddress, affectedCells)
            assert problem.message.contains("Metadata file contains data from more than one run.")
        } else {
            assert context.problems.isEmpty()
        }

        where:
        containsMultipleRuns | isDataFilesOnGpcfMidTerm | ilse        || error
        false                | false                    | NONE        || false
        false                | false                    | EMPTY       || false
        false                | false                    | DIFFERENT   || false
        false                | false                    | SAME        || false
        false                | true                     | NONE        || false
        false                | true                     | EMPTY       || false
        false                | true                     | DIFFERENT   || false
        false                | true                     | SAME        || false
        true                 | false                    | NONE        || true
        true                 | false                    | EMPTY       || true
        true                 | false                    | DIFFERENT   || true
        true                 | false                    | SAME        || true
        true                 | true                     | NONE        || true
        true                 | true                     | EMPTY       || true
        true                 | true                     | DIFFERENT   || true
        true                 | true                     | SAME        || false
    }

    enum Ilse {    // Ilse column
        NONE,      // does not exist
        EMPTY,     // is empty
        DIFFERENT, // contains different values
        SAME,      // contains only one values
    }
}

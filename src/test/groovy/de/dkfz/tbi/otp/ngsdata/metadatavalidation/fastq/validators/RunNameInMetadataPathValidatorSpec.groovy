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
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesWithAbsolutePath
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Paths

import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class RunNameInMetadataPathValidatorSpec extends Specification {

    @Unroll
    void "test validate"(boolean containsMultipleRuns, boolean isDataFilesOnGpcfMidTerm, boolean mdFilenameContainsRunName, boolean error) {
        given:
        String data = containsMultipleRuns ? "run1\nrun2\n" : "run1\nrun1\n"

        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n" + data,
                [
                        metadataFile: mdFilenameContainsRunName ? Paths.get("run1") : Paths.get("whatever"),
                        directoryStructure: isDataFilesOnGpcfMidTerm ? new DataFilesInGpcfSpecificStructure() : [:],
                ],
        )

        when:
        new RunNameInMetadataPathValidator().validate(context)

        then:
        if (error) {
            Problem problem = exactlyOneElement(context.problems)
            assert problem.level == LogLevel.WARNING
            assert containSame(problem.affectedCells*.cellAddress, ['A2', 'A3'])
            assert problem.message.contains("The path of the metadata file should contain the run name.")
        } else {
            assert context.problems.isEmpty()
        }

        where:
        containsMultipleRuns | isDataFilesOnGpcfMidTerm | mdFilenameContainsRunName || error
        false                | false                    | false                     || true
        false                | false                    | true                      || false
        false                | true                     | false                     || false
        false                | true                     | true                      || false
        true                 | false                    | false                     || false
        true                 | false                    | true                      || false
        true                 | true                     | false                     || false
        true                 | true                     | true                      || false
    }


    @Unroll
    void "validate, when directory structure is DataFilesWithAbsolutePath, then run name does not need to be in path"() {
        given:
        MetadataValidationContext context = MetadataValidationContextFactory.createContext(
                "${MetaDataColumn.RUN_ID}\n${runEntry1}\n${runEntry2}",
                [
                        metadataFile: Paths.get("whatever"),
                        directoryStructure: new DataFilesWithAbsolutePath(),
                ],
        )

        when:
        new RunNameInMetadataPathValidator().validate(context)

        then:
        context.problems.isEmpty()

        where:
        runEntry1 | runEntry2
        'run1' | 'run2'
        'run1' | 'run1'
    }
}

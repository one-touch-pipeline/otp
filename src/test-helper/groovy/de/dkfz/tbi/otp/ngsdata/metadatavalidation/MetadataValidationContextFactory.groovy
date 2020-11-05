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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Paths

class MetadataValidationContextFactory {

    static MetadataValidationContext createContext(String document, Map properties = [:]) {
        assert !properties.containsKey('document')
        return createContext(properties + [document: document])
    }

    @SuppressWarnings('UnsafeImplementationAsMap')
    static MetadataValidationContext createContext(Map properties = [:]) {
        return new MetadataValidationContext(
                properties.metadataFile ?: Paths.get(properties.testDirectory ?: TestCase.uniqueNonExistentPath.path, "run${HelperUtils.uniqueString}"
                        as String, 'metadata_fastq.tsv'),
                properties.metadataFileMd5sum ?: HelperUtils.randomMd5sum,
                properties.spreadsheet ?: new Spreadsheet(properties.document ?: 'I am header!\nI am data!'),
                properties.problems ?: new Problems(),
                properties.directoryStructure ?: [:] as DirectoryStructure,
                properties.directoryStructureDescription ?: "",
                properties.content ?: ''.bytes
        )
    }
}

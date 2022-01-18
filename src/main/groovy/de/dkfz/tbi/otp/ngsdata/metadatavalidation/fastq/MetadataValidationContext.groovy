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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.MD5

class MetadataValidationContext extends AbstractMetadataValidationContext {

    final DirectoryStructure directoryStructure
    final String directoryStructureDescription

    /**
     * Keep track of which SampleIdentifiers were actually used in the processing of this MetaDataFile, for cleanup later.
     */
    final Set<SampleIdentifier> usedSampleIdentifiers

    private MetadataValidationContext(Path metadataFile,
                                      String metadataFileMd5sum,
                                      Spreadsheet spreadsheet,
                                      Problems problems,
                                      DirectoryStructure directoryStructure,
                                      String directoryStructureDescription,
                                      byte[] content) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
        this.directoryStructure = directoryStructure
        this.directoryStructureDescription = directoryStructureDescription
        this.usedSampleIdentifiers = [] as Set
    }

    static MetadataValidationContext createFromFile(Path metadataFile, DirectoryStructure directoryStructure, String directoryStructureDescription,
        boolean ignoreAlreadyKnownMd5sum = false) {
        Map parametersForFile = readAndCheckFile(metadataFile, { String s ->
            MetaDataColumn.getColumnForName(s)?.name() ?: s
        }, { Row row ->
            !row.getCellByColumnTitle(FASTQ_FILE.name())?.text?.startsWith('Undetermined') &&
                    // Add additional filter to skip rows containing known md5sum in database
                    (!ignoreAlreadyKnownMd5sum || DataFile.findAllByMd5sum(row.getCellByColumnTitle(MD5.name())?.text).empty)
        })

        return new MetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, parametersForFile.problems, directoryStructure, directoryStructureDescription, parametersForFile.bytes)
    }

    List<String> getSummary() {
        return problems*.type.flatten().unique()
    }
}

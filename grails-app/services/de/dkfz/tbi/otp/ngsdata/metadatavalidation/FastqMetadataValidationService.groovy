/*
 * Copyright 2011-2024 The OTP authors
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

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Row
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.MD5

class FastqMetadataValidationService extends MetadataValidationService {

    @CompileDynamic
    MetadataValidationContext createFromFile(Path metadataFile, DirectoryStructure directoryStructure, String directoryStructureDescription,
                                             boolean ignoreAlreadyKnownMd5sum = false) {
        Map parametersForFile = readAndCheckFile(metadataFile) { String s ->
            MetaDataColumn.getColumnForName(s)?.name() ?: s
        } { Row row ->
            !row.getCellByColumnTitle(FASTQ_FILE.name())?.text?.startsWith('Undetermined') &&
                    // Add additional filter to skip rows containing known md5sum in database
                    (!ignoreAlreadyKnownMd5sum || RawSequenceFile.findAllByFastqMd5sum(row.getCellByColumnTitle(MD5.name())?.text).empty)
        }

        return new MetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, parametersForFile.problems, directoryStructure,
                directoryStructureDescription, parametersForFile.bytes)
    }

    @CompileDynamic
    MetadataValidationContext createFromContent(ContentWithPathAndProblems contentWithPathAndProblems,
                                                DirectoryStructure directoryStructure,
                                                String directoryStructureDescription,
                                                boolean ignoreAlreadyKnownMd5sum = false) {
        Map parametersForFile = checkContent(contentWithPathAndProblems.content) { String s ->
            MetaDataColumn.getColumnForName(s)?.name() ?: s
        } { Row row ->
            !row.getCellByColumnTitle(FASTQ_FILE.name())?.text?.startsWith('Undetermined') &&
                    // Add additional filter to skip rows containing known md5sum in database
                    (!ignoreAlreadyKnownMd5sum || RawSequenceFile.findAllByFastqMd5sum(row.getCellByColumnTitle(MD5.name())?.text).empty)
        }

        return new MetadataValidationContext(contentWithPathAndProblems.path, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, parametersForFile.problems.addProblems(contentWithPathAndProblems.problems),
                directoryStructure, directoryStructureDescription, contentWithPathAndProblems.content)
    }
}

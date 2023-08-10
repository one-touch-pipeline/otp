/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.SampleIdentifier
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Path

class MetadataValidationContext extends AbstractMetadataValidationContext {

    final DirectoryStructure directoryStructure
    final String directoryStructureDescription

    /**
     * Keep track of which SampleIdentifiers were actually used in the processing of this MetaDataFile, for cleanup later.
     */
    final Set<SampleIdentifier> usedSampleIdentifiers

    MetadataValidationContext(Path metadataFile,
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

    List<String> getSummary() {
        return problems*.type.flatten().unique() as List<String>
    }
}

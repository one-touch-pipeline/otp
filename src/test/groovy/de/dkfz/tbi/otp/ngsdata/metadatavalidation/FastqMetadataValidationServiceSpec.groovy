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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import grails.testing.gorm.DataTest
import org.junit.ClassRule
import spock.lang.*

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

class FastqMetadataValidationServiceSpec extends Specification implements DomainFactoryCore, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingPriority,
                Project,
                Individual,
                SampleType,
                Sample,
                FastqImportInstance,
                FastqFile,
        ]
    }

    DirectoryStructure directoryStructure = [:] as DirectoryStructure

    @Shared
    @ClassRule
    @TempDir
    Path tempDir

    FastqMetadataValidationService metadataValidationFileService

    void setup() {
        metadataValidationFileService = new FastqMetadataValidationService()
    }

    @Unroll
    void 'createFromFile, when file contains undetermined entries, or with ignoreMd5sum flag, ignores them'() {
        given:
        final String md5sum = '123456789012345678901234567890ab'
        createFastqFile(
                fileName: 'fastq',
                fastqMd5sum: md5sum,
        )
        Path file = CreateFileHelper.createFile(tempDir.resolve("${HelperUtils.uniqueString}.tsv"))
        file.bytes = ("c ${FASTQ_FILE} ${SAMPLE_NAME} ${INDEX} ${MD5}\n" +
                "0 Undetermined_1.fastq.gz x x x\n" +
                "1 Undetermined_1.fastq.gz x Undetermined x\n" +
                "2 Undetermined_1.fastq.gz Undetermined_1 x x\n" +
                "3 Undetermined_1.fastq.gz Undetermined_1 Undetermined x\n" +
                "4 x x x x\n" +
                "5 x x Undetermined x\n" +
                "6 x Undetermined_1 x x\n" +
                "7 x Undetermined_1 Undetermined x\n" +
                "8 fastq x x ${md5sum}\n" +
                "").replaceAll(' ', '\t').getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = metadataValidationFileService.createFromFile(file, directoryStructure, "", ignoreMd5sum)

        then:
        metadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> true
        }

        and:
        context.spreadsheet.dataRows.size() == numRows
        context.spreadsheet.dataRows[0].cells[0].text == '4'
        context.spreadsheet.dataRows[1].cells[0].text == '5'
        context.spreadsheet.dataRows[2].cells[0].text == '6'
        context.spreadsheet.dataRows[3].cells[0].text == '7'
        if (!ignoreMd5sum) {
            assert (context.spreadsheet.dataRows[4].cells[0].text == '8')
        }

        where:
        ignoreMd5sum || numRows
        true         || 4
        false        || 5
    }

    void 'createFromFile, when file header contains alias, replace it'() {
        given:
        Path file = CreateFileHelper.createFile(tempDir.resolve("${HelperUtils.uniqueString}.tsv"))
        file.bytes = ("UNKNOWN ${ALIGN_TOOL} ${SEQUENCING_READ_TYPE.importAliases.first()}\n" +
                "1 2 3"
        ).replaceAll(' ', '\t').getBytes(MetadataValidationContext.CHARSET)

        when:
        MetadataValidationContext context = metadataValidationFileService.createFromFile(file, directoryStructure, "")

        then:
        metadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> true
        }

        and:
        context.spreadsheet.header.cells[0].text == "UNKNOWN"
        context.spreadsheet.header.cells[1].text == ALIGN_TOOL.name()
        context.spreadsheet.header.cells[2].text == SEQUENCING_READ_TYPE.name()
    }
}

/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.infrastructure.fastqc.FastqcWorkFileService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper

import java.nio.file.Path

class FastqcDataFilesServiceSpec extends Specification implements ServiceUnitTest<FastqcDataFilesService>, DataTest, FastqcDomainFactory,
        WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
                FastqImportInstance,
                SeqPlatform,
                SeqPlatformGroup,
                ProcessingOption,
        ]
    }

    TestConfigService configService

    @TempDir
    Path tempDir

    SeqTrack seqTrack
    RawSequenceFile rawSequenceFile
    FastqcProcessedFile fastqcProcessedFile

    void setup() {
        configService = new TestConfigService(tempDir)
        service.fastqcWorkFileService = Mock(FastqcWorkFileService) {
            _ * fastqcOutputPath(_) >> tempDir.resolve("fastqc")
        }

        seqTrack = createSeqTrack()

        rawSequenceFile = createFastqFile([seqTrack: seqTrack, project: seqTrack.project, run: seqTrack.run])
        fastqcProcessedFile = createFastqcProcessedFile([sequenceFile: rawSequenceFile])
    }

    void "updateFastqcProcessedFile, when file exist, then update fastqcProcessedFile"() {
        given:
        Path path = service.fastqcWorkFileService.fastqcOutputPath(fastqcProcessedFile)
        CreateFileHelper.createFile(path)
        service.fileService = Mock(FileService) {
            1 * fileIsReadable(path) >> true
        }

        when:
        service.updateFastqcProcessedFile(fastqcProcessedFile)

        then:
        fastqcProcessedFile.fileExists
        fastqcProcessedFile.fileSize > 0
        fastqcProcessedFile.dateFromFileSystem
    }

    void "updateFastqcProcessedFile, when file not exist, then do not update fastqcProcessedFile"() {
        given:
        service.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> false
        }

        when:
        service.updateFastqcProcessedFile(fastqcProcessedFile)

        then:
        !fastqcProcessedFile.fileExists
        fastqcProcessedFile.fileSize == -1
        !fastqcProcessedFile.dateFromFileSystem
    }
}

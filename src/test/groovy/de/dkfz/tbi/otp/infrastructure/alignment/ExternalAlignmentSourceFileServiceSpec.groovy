/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.FileSystemService

import java.nio.file.FileSystems
import java.nio.file.Paths

class ExternalAlignmentSourceFileServiceSpec extends Specification implements ServiceUnitTest<ExternalAlignmentSourceFileService>, DataTest, de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryBam {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    ExternallyProcessedBamFile bamFile
    String importDir

    void setup() {
        importDir = TestCase.uniqueNonExistentPath
        String bamName = "bamFile.bam"
        bamFile = createBamFile(fileName: bamName, importedFrom: Paths.get(importDir, bamName))
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
        service.fileSystemService = Mock(FileSystemService) {
            remoteFileSystem >> FileSystems.default
        }
    }

    void "test getBamFile"() {
        expect:
        service.getBamFile(bamFile) == Paths.get(importDir, bamFile.bamFileName)
    }

    void "test getBaiFile"() {
        expect:
        service.getBaiFile(bamFile) == Paths.get(importDir, bamFile.baiFileName)
    }

    void "test getDirectoryPath"() {
        expect:
        service.getDirectoryPath(bamFile) == Paths.get(importDir)
    }
}

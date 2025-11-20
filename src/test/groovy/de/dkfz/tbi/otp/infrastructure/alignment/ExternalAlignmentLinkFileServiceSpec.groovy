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
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactoryBam
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Path
import java.nio.file.Paths

class ExternalAlignmentLinkFileServiceSpec extends Specification implements ServiceUnitTest<ExternalAlignmentLinkFileService>, DataTest, ExternalBamFactoryBam {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    ExternallyProcessedBamFile bamFile

    void setup() {
        String bamName = "bamFile.bam"
        bamFile = createBamFile(fileName: bamName, importedFrom: Paths.get(TestCase.uniqueNonExistentPath.path, bamName))
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getBamFile"() {
        expect:
        service.getBamFile(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", bamFile.bamFileName)
    }

    void "test getBaiFile"() {
        expect:
        service.getBaiFile(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", bamFile.baiFileName)
    }

    void "test getFurtherFiles"() {
        given:
        List<String> files = [
                'file1',
                'file2',
        ]
        bamFile.furtherFiles = files as Set

        List<Path> expected = files.collect {
            Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", it)
        }

        expect:
        service.getFurtherFiles(bamFile) == expected
    }

    void "test getBamMaxReadLengthFile"() {
        expect:
        service.getBamMaxReadLengthFile(bamFile) ==
                Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", "${bamFile.bamFileName}.maxReadLength")
    }

    void "test getNonOtpFolder"() {
        expect:
        service.getNonOtpFolder(bamFile) == Paths.get("/base-dir", "nonOTP")
    }

    void "test getImportFolder"() {
        expect:
        service.getDirectoryPath(bamFile) == Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}")
    }

    void "test getPathForFurtherProcessing, should return final directory"() {
        given:
        bamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        bamFile.md5sum = HelperUtils.randomMd5sum
        bamFile.fileSize = 1
        bamFile.save(flush: true)
        bamFile.mergingWorkPackage.bamFileInProjectFolder = bamFile
        bamFile.mergingWorkPackage.save(flush: true)

        expect:
        service.getPathForFurtherProcessing(bamFile) ==
                Paths.get("/base-dir", "nonOTP", "analysisImport_${bamFile.referenceGenome.name}", bamFile.bamFileName)
    }

    void "test getPathForFurtherProcessing, when not set in mergingWorkPackage, should throw exception"() {
        when:
        service.getPathForFurtherProcessing(bamFile)

        then:
        thrown(IllegalStateException)
    }
}

/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.HelperUtils

import java.nio.file.Paths

class ExternallyProcessedBamFileServiceSpec extends Specification implements ServiceUnitTest<ExternallyProcessedBamFileService>, DataTest, ExternalBamFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    ExternallyProcessedBamFile bamFile
    String testDir

    void setup() {
        bamFile = createBamFile()
        testDir = "/base-dir"
        service.abstractBamFileService = Mock(AbstractBamFileService) {
            getBaseDirectory(_) >> Paths.get("/base-dir")
        }
    }

    void "test getBamFile"() {
        expect:
        service.getBamFile(bamFile).toString() == "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}/${bamFile.bamFileName}"
    }

    void "test getBaiFile"() {
        expect:
        service.getBaiFile(bamFile).toString() == "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}/${bamFile.baiFileName}"
    }

    void "test getBamMaxReadLengthFile"() {
        expect:
        service.getBamMaxReadLengthFile(bamFile).toString() ==
                "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}/${bamFile.bamFileName}.maxReadLength"
    }

    void "test getNonOtpFolder"() {
        expect:
        service.getNonOtpFolder(bamFile).toString() == "/base-dir/nonOTP"
    }

    void "test getImportFolder"() {
        expect:
        service.getImportFolder(bamFile).toString() == "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}"
    }

    void "test getFinalInsertSizeFile"() {
        given:
        bamFile.insertSizeFile = "insert-size-file"

        expect:
        service.getFinalInsertSizeFile(bamFile).toString() == "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}/insert-size-file"
    }

    void "test getPathForFurtherProcessing, returns null since qcTrafficLightStatus is #status"() {
        given:
        bamFile.qcTrafficLightStatus = status
        bamFile.comment = DomainFactory.createComment()

        expect:
        !service.getPathForFurtherProcessing(bamFile)

        where:
        status << [AbstractBamFile.QcTrafficLightStatus.BLOCKED, AbstractBamFile.QcTrafficLightStatus.REJECTED]
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
        service.getPathForFurtherProcessing(bamFile).toString() == "/base-dir/nonOTP/analysisImport_${bamFile.referenceGenome.name}/${bamFile.bamFileName}"
    }

    void "test getPathForFurtherProcessing, when not set in mergingWorkPackage, should throw exception"() {
        when:
        service.getPathForFurtherProcessing(bamFile)

        then:
        thrown(IllegalStateException)
    }
}

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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.RoddyConfigService
import de.dkfz.tbi.otp.workflow.alignment.AlignmentQualityOverviewService

import java.nio.file.Path

class AlignmentQualityOverviewServiceSpec extends Specification implements ServiceUnitTest<AlignmentQualityOverviewService>, DataTest, RoddyPanCancerFactory {

    @TempDir
    private Path workDir

    private RoddyBamFile roddyBamFile

    private byte[] expectedContent

    @Override
    Class[] getDomainClassesToMock() {
        return [
                MergingWorkPackage,
                RoddyBamFile,
        ]
    }

    void setupData(boolean fileExist) {
        Path configFile = workDir.resolve('config.xml')
        if (fileExist) {
            expectedContent = "some_text_$nextId".bytes
            configFile.bytes = expectedContent
        } else {
            expectedContent = new byte[0]
        }
        roddyBamFile = new RoddyBamFile()
        service.roddyResultServiceFactoryService = Mock(RoddyResultServiceFactoryService) {
            1 * getService(roddyBamFile) >> {
                return Mock(RoddyResultServiceTrait) {
                    1 * getDirectoryPath(roddyBamFile) >> workDir
                    0 * _
                }
            }
            0 * _
        }
        service.roddyConfigService = Mock(RoddyConfigService) {
            1 * getConfigPath(workDir) >> configFile
            0 * _
        }
        service.fileService = Mock(FileService) {
            1 * fileIsReadable(configFile) >> fileExist
            0 * _
        }
    }

    void "fetchConfigFileContent, if result has an config file, return the content as byte array"() {
        given:
        setupData(true)

        when:
        byte[] bytes = service.fetchConfigFileContent(roddyBamFile)

        then:
        bytes == expectedContent
    }

    void "fetchConfigFileContent, if result has no config file, return empty byte array"() {
        given:
        setupData(false)

        when:
        byte[] bytes = service.fetchConfigFileContent(roddyBamFile)

        then:
        bytes.size() == 0
    }
}

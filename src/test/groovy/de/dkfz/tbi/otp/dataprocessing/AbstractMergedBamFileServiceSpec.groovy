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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils

class AbstractMergedBamFileServiceSpec extends Specification implements DataTest {

    @Rule
    TemporaryFolder temporaryFolder

    void "getExistingBamFilePath, when all fine, return the file"() {
        given:
        File file = CreateFileHelper.createFile(temporaryFolder.newFile())
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            1 * getPathForFurtherProcessing() >> file
            1 * getMd5sum() >> HelperUtils.randomMd5sum
            2 * getFileSize() >> file.size()
            0 * _
        }

        when:
        File existingFile = service.getExistingBamFilePath(bamFile)

        then:
        file == existingFile
    }

    @Unroll
    void "getExistingBamFilePath, when fail for #failCase, throw an exception"() {
        given:
        File file = CreateFileHelper.createFile(temporaryFolder.newFile())
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            _ * getPathForFurtherProcessing() >> {
                if (failCase == 'exceptionInFurtherProcessingPath') {
                    throw new AssertionError('getPathForFurtherProcessing fail')
                } else if (failCase == 'fileNotExist') {
                    TestCase.uniqueNonExistentPath
                } else {
                    file
                }
            }
            _ * getMd5sum() >> {
                failCase == 'invalidMd5sum' ? 'invalid' : HelperUtils.randomMd5sum
            }
            _ * getFileSize() >> {
                failCase == 'fileSizeZero' ? 0 :
                        failCase == 'fileSizeWrong' ? 1234 : file.length()
            }
            0 * _
        }

        when:
        service.getExistingBamFilePath(bamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(exception)

        where:
        failCase                           || exception
        'exceptionInFurtherProcessingPath' || 'getPathForFurtherProcessing fail'
        'fileNotExist'                     || 'on local filesystem is not accessible or does not exist. Expression: de.dkfz.tbi.otp.utils.ThreadUtils.waitFor'
        'invalidMd5sum'                    || 'assert bamFile.getMd5sum()'
        'fileSizeZero'                     || 'assert bamFile.getFileSize()'
        'fileSizeWrong'                    || 'assert file.length() == bamFile.getFileSize()'
    }
}

/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.withdraw

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.DeletionService

import java.nio.file.Path

abstract class WithdrawBamFileServiceSpec<T extends WithdrawBamFileService> extends Specification implements ServiceUnitTest<T>, DataTest {

    @Rule
    TemporaryFolder temporaryFolder

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                ReferenceGenomeProjectSeqType,
        ]
    }

    void "collectPaths, when called for bamFiles, then return paths in workDir, except nonOTP paths"() {
        given:
        Path vbpPath = temporaryFolder.newFolder().toPath()

        service.abstractMergedBamFileService = new AbstractMergedBamFileService()
        service.abstractMergedBamFileService.individualService = Mock(IndividualService) {
            getViewByPidPath(_, _) >> vbpPath
        }

        List<AbstractMergedBamFile> bamFiles = (1..3).collect {
            createBamFile()
        }
        List<Path> files = bamFiles.collectMany {
            Path baseDirectory = service.abstractMergedBamFileService.getBaseDirectory(it)
            CreateFileHelper.createFile(baseDirectory.resolve("nonOTP").resolve("file"))

            return [
                    "a", "b", "c",
            ].collect {
                CreateFileHelper.createFile(baseDirectory.resolve(it))
            } + [
                    "dir1", "dir2", "dir3",
            ].collect {
                CreateFileHelper.createFile(baseDirectory.resolve(it).resolve("file")).parent
            }
        }

        when:
        List<String> result = service.collectPaths(bamFiles)

        then:
        TestCase.assertContainSame(result, files*.toString())
    }

    void "withdrawObjects, when called for bamFiles, then mark each as withdrawn "() {
        given:
        List<AbstractMergedBamFile> bamFiles = (1..3).collect {
            createBamFile()
        }
        bamFiles << createBamFile([
                status: AbstractBamFile.State.NEEDS_PROCESSING
        ])

        when:
        service.withdrawObjects(bamFiles)

        then:
        bamFiles.each {
            assert it.withdrawn
            assert it.status != AbstractBamFile.State.NEEDS_PROCESSING
        }
    }

    void "deleteObjects, when called for bamFiles, then delete each of them"() {
        given:
        service.deletionService = Mock(DeletionService) {
            3 * deleteAllProcessingInformationAndResultOfOneSeqTrack(_)
        }

        List<AbstractMergedBamFile> bamFiles = (1..3).collect {
            createBamFile()
        }

        when:
        service.deleteObjects(bamFiles)

        then:
        noExceptionThrown()
    }

    abstract AbstractBamFile createBamFile()

    abstract AbstractBamFile createBamFile(Map map)
}

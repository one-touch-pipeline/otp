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
package de.dkfz.tbi.otp.utils

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType

class DeletionServiceSpec extends Specification implements ServiceUnitTest<DeletionService>, DataTest, RoddyPancanFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataFile,
                MergingWorkPackage,
                RoddyBamFile,
                ReferenceGenomeProjectSeqType,
        ]
    }

    void 'addRoddyBamFilesRecursively, when bam file is not part of hierarchy, return same list'() {
        given:
        RoddyBamFile rowdyBamFileNoHierarchy1 = createBamFile()
        RoddyBamFile rowdyBamFileNoHierarchy2 = createBamFile()

        createBamFile()

        List<RoddyBamFile> inputList = [
                rowdyBamFileNoHierarchy1,
                rowdyBamFileNoHierarchy2,
        ]

        List<RoddyBamFile> expected = inputList

        when:
        List<RoddyBamFile> ret = service.addRoddyBamFilesRecursively(inputList)

        then:
        TestCase.assertContainSame(ret, expected)
    }

    void 'addRoddyBamFilesRecursively, when bam file has base but no depending bam files, return same list'() {
        given:
        RoddyBamFile rowdyBamFileWithBase1 = createBamFile([baseBamFile: createBamFile()])
        RoddyBamFile rowdyBamFileWithBase2 = createBamFile([baseBamFile: createBamFile()])

        List<RoddyBamFile> inputList = [
                rowdyBamFileWithBase1,
                rowdyBamFileWithBase2,
        ]

        List<RoddyBamFile> expected = inputList

        when:
        List<RoddyBamFile> ret = service.addRoddyBamFilesRecursively(inputList)

        then:
        TestCase.assertContainSame(ret, expected)
    }

    void 'addRoddyBamFilesRecursively, when bam file has depending bam files, return given and depending'() {
        given:
        RoddyBamFile rowdyBamFileWithDepending1 = createBamFile()
        RoddyBamFile rowdyBamFileDepending1 = createBamFile([baseBamFile: rowdyBamFileWithDepending1])
        RoddyBamFile rowdyBamFileWithDepending2 = createBamFile()
        RoddyBamFile rowdyBamFileDepending2 = createBamFile([baseBamFile: rowdyBamFileWithDepending2])

        List<RoddyBamFile> inputList = [
                rowdyBamFileWithDepending1,
                rowdyBamFileWithDepending2,
        ]

        List<RoddyBamFile> expected = inputList + [
                rowdyBamFileDepending1,
                rowdyBamFileDepending2,
        ]

        when:
        List<RoddyBamFile> ret = service.addRoddyBamFilesRecursively(inputList)

        then:
        TestCase.assertContainSame(ret, expected)
    }

    void 'addRoddyBamFilesRecursively, when bam file has depending bam files about multiple level, then return all of them'() {
        given:
        RoddyBamFile rowdyBamFileWithDepending = createBamFile()
        RoddyBamFile rowdyBamFileDepending = createBamFile([baseBamFile: rowdyBamFileWithDepending])
        RoddyBamFile rowdyBamFileDependingLevel2 = createBamFile([baseBamFile: rowdyBamFileDepending])
        RoddyBamFile rowdyBamFileDependingLevel3 = createBamFile([baseBamFile: rowdyBamFileDependingLevel2])

        List<RoddyBamFile> inputList = [
                rowdyBamFileWithDepending,
        ]

        List<RoddyBamFile> expected = inputList + [
                rowdyBamFileDepending,
                rowdyBamFileDependingLevel2,
                rowdyBamFileDependingLevel3,
        ]

        when:
        List<RoddyBamFile> ret = service.addRoddyBamFilesRecursively(inputList)

        then:
        TestCase.assertContainSame(ret, expected)
    }
}

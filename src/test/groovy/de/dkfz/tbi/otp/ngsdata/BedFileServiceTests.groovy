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

package de.dkfz.tbi.otp.ngsdata

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService

@TestFor(BedFileService)
@Build([
        BedFile,
        ProcessingOption,
])
class BedFileServiceTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    BedFile bedFile
    File referenceGenomesBaseDirectory

    @Before
    void setUp() {
        bedFile = BedFile.build([fileName: 'bedFileName'])

        referenceGenomesBaseDirectory = temporaryFolder.newFolder("reference_genomes", bedFile.referenceGenome.path, "targetRegions")
        referenceGenomesBaseDirectory.mkdirs()

        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomesBaseDirectory.parentFile.parent)

        service.referenceGenomeService = new ReferenceGenomeService()
        service.referenceGenomeService.processingOptionService = new ProcessingOptionService()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ReferenceGenomeService, service.referenceGenomeService)
        bedFile = null
    }

    @Test
    void test_filePath_WhenBedFileIsNull_ShouldFailWithException() {
        shouldFail(IllegalArgumentException) { service.filePath(null) }
    }

    @Test
    void test_filePath_WhenBedFileDoesNotExist_ShouldFailWithException() {
        assert shouldFail(RuntimeException) {
            service.filePath(bedFile)
        } =~ /the bedFile can not be read/
    }

    @Test
    void test_filePath_WhenBedFileExists_ShouldReturnPathToFile() {
        // setup:
        new File(referenceGenomesBaseDirectory, 'bedFileName').createNewFile()

        // expect:
        assert service.filePath(bedFile) == "${referenceGenomesBaseDirectory.parentFile.path}/targetRegions/bedFileName" as String
    }
}

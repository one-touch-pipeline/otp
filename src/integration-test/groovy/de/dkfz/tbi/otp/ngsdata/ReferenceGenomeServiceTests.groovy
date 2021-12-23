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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.CreateFileHelper

import static org.junit.Assert.assertEquals

@Rollback
@Integration
class ReferenceGenomeServiceTests {

    ReferenceGenomeService referenceGenomeService
    TestConfigService configService

    File directory
    File statFile
    File chromosomeLengthFile

    @SuppressWarnings("PublicInstanceField") // must be public in JUnit tests
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    private MergingWorkPackage createDataForChromosomeSizeInformationFiles()  {
        configService.addOtpProperties(temporaryFolder.newFolder().toPath())
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
                referenceGenome: DomainFactory.createReferenceGenome(chromosomeLengthFilePath: DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME),
                pipeline: DomainFactory.createPanCanPipeline(),
        ])

        File referenceGenomeDirectory = new File(configService.processingRootPath, 'reference_genomes')
        DomainFactory.createProcessingOptionBasePathReferenceGenome(referenceGenomeDirectory.path)
        directory = new File(
                new File(referenceGenomeDirectory,
                        mergingWorkPackage.referenceGenome.path),
                ReferenceGenomeService.CHROMOSOME_SIZE_FILES_PREFIX)
        statFile = new File(directory, DomainFactory.DEFAULT_TAB_FILE_NAME)
        chromosomeLengthFile = new File(directory, DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME)

        return mergingWorkPackage
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void testChromosomeStatSizeFile_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = statFile
        File pathAct = referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeStatSizeFile_WithFileCheck_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = statFile
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, true)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeStatSizeFile_MergingWorkPackageIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeStatSizeFile(null)
        }.contains('mergingWorkPackage')
    }

    @Test
    void testChromosomeStatSizeFile_NoStatSizeFileIsDefined_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        mergingWorkPackage.pipeline = DomainFactory.createDefaultOtpPipeline()
        mergingWorkPackage.statSizeFileName = null
        mergingWorkPackage.save(flush: true)
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, false)
        }.contains('No stat file size name is defined')
    }

    @Test
    void testChromosomeStatSizeFile_StateSizeFileDoesNotExistAndExistenceIsChecked_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        assert directory.mkdirs()
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.chromosomeStatSizeFile(mergingWorkPackage, true)
        }.contains(DomainFactory.DEFAULT_TAB_FILE_NAME)
    }

    @Test
    void testChromosomeLengthFile_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = chromosomeLengthFile
        File pathAct = referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, false)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeLengthFile_WithFileCheck_AllFine() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        File pathExp = chromosomeLengthFile
        CreateFileHelper.createFile(pathExp)
        File pathAct = referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, true)
        assertEquals(pathExp, pathAct)
    }

    @Test
    void testChromosomeLengthFile_MergingWorkPackageIsNull_ShouldFail() {
        assert TestCase.shouldFail(AssertionError) {
            referenceGenomeService.chromosomeLengthFile(null)
        }.contains('mergingWorkPackage')
    }

    @Test
    void testChromosomeLengthFile_ChromosomeLengthFileFileDoesNotExistAndExistenceIsChecked_ShouldFail() {
        MergingWorkPackage mergingWorkPackage = createDataForChromosomeSizeInformationFiles()
        assert directory.mkdirs()
        assert TestCase.shouldFail(RuntimeException) {
            referenceGenomeService.chromosomeLengthFile(mergingWorkPackage, true)
        }.contains(DomainFactory.DEFAULT_CHROMOSOME_LENGTH_FILE_NAME)
    }
}

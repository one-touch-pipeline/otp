package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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

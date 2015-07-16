package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@TestFor(BedFileService)
@Build([BedFile])
class BedFileServiceTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    Realm realm
    BedFile bedFile
    File referenceGenomesBaseDirectory

    @Before
    void setUp() {
        realm = DomainFactory.createRealmDataProcessingDKFZ([
                processingRootPath: temporaryFolder.root.path,
                rootPath          : temporaryFolder.root.path,
        ])
        bedFile = BedFile.build([fileName: 'bedFileName'])

        referenceGenomesBaseDirectory = temporaryFolder.newFolder("reference_genomes", bedFile.referenceGenome.path, "targetRegions")
        referenceGenomesBaseDirectory.mkdirs()

        service.referenceGenomeService = new ReferenceGenomeService()
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ReferenceGenomeService, service.referenceGenomeService)
        realm = null
        bedFile = null
    }

    void test_filePath_WhenProjectIsNull_ShouldFailWithException() {
        shouldFail(IllegalArgumentException) { service.filePath(null, bedFile) }
    }

    void test_filePath_WhenBedFileIsNull_ShouldFailWithException() {
        shouldFail(IllegalArgumentException) { service.filePath(realm, null) }
    }

    void test_filePath_WhenBedFileDoesNotExist_ShouldFailWithException() {
        assert shouldFail(RuntimeException) {
            service.filePath(realm, bedFile)
        } =~ /the bedFile can not be read/
    }

    void test_filePath_WhenBedFileExists_ShouldReturnPathToFile() {
        // setup:
        new File(referenceGenomesBaseDirectory, 'bedFileName').createNewFile()

        // expect:
        assert service.filePath(realm, bedFile) == "${referenceGenomesBaseDirectory.parentFile.path}//targetRegions/bedFileName" as String
    }
}

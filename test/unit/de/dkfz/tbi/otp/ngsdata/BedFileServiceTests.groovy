package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestFor
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@TestFor(BedFileService)
class BedFileServiceTests {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    def fakeRealm = [] as Realm
    def fakeBedFileDomain = [fileName: 'bedFileName'] as BedFile
    def fakeReferenceGenomeService = [
            filePathToDirectory: { realm, refGenome -> "${temporaryFolder.root}/referenceGenome" }
    ] as ReferenceGenomeService

    void test_filePath_WhenProjectIsNull_ShouldFailWithException() {
        shouldFail(IllegalArgumentException) { service.filePath(null, fakeBedFileDomain) }
    }

    void test_filePath_WhenBedFileIsNull_ShouldFailWithException() {
        shouldFail(IllegalArgumentException) { service.filePath(fakeRealm, null) }
    }

    void test_filePath_WhenBedFileDoesNotExist_ShouldFailWithException() {
        service.referenceGenomeService = fakeReferenceGenomeService

        assert shouldFail(RuntimeException) {
            service.filePath(fakeRealm, fakeBedFileDomain)
        } =~ /the bedFile can not be read/
    }

    void test_filePath_WhenBedFileExists_ShouldReturnPathToFile() {
        // setup:
        File referenceGenomesBaseDirectory = temporaryFolder.newFolder("referenceGenome/targetRegions")
        referenceGenomesBaseDirectory.mkdirs()

        new File(referenceGenomesBaseDirectory, 'bedFileName').createNewFile()

        service.referenceGenomeService = fakeReferenceGenomeService

        // expect:
        assert service.filePath(fakeRealm, fakeBedFileDomain) == "${temporaryFolder.root.path}/referenceGenome/targetRegions/bedFileName"
    }
}

package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*

import org.junit.*

class ChecksumFileServiceTests {

    private final static String filename = "/tmp/otp-test.file.md5sum"

    File file
    ChecksumFileService checksumFileService

    @Before
    void setUp() {
        file = new File(filename)
        file.createNewFile()
        assertTrue(file.exists())
    }

    @After
    void tearDown() {
        file.delete()
    }

    @Test
    void testFirstMD5ChecksumFromFile() {
        file << "68b329da9893e34099c7d8ad5cb9c940  opt-test.file\n999329da9893e34099c7d8ad5cb9c940  opt-test2.file\n"
        String md5sumExp = "68b329da9893e34099c7d8ad5cb9c940"
        String md5sumAct = checksumFileService.firstMD5ChecksumFromFile(file)
        assertEquals(md5sumExp, md5sumAct)
        md5sumAct = checksumFileService.firstMD5ChecksumFromFile(filename)
        assertEquals(md5sumExp, md5sumAct)
    }

    @Test(expected = RuntimeException)
    void testFirstMD5ChecksumFromFileNotReadable() {
        file.setReadable(false)
        checksumFileService.firstMD5ChecksumFromFile(file)
    }

    @Test(expected = RuntimeException)
    void testFirstMD5ChecksumFromFileEmpty() {
        checksumFileService.firstMD5ChecksumFromFile(file)
    }

    @Test(expected = RuntimeException)
    void testFirstMD5ChecksumFromFileFileNotExists() {
        file.delete()
        checksumFileService.firstMD5ChecksumFromFile(file)
    }
}

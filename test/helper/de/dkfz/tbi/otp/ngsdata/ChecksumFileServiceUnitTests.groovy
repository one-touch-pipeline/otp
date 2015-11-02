package de.dkfz.tbi.otp.ngsdata

import org.junit.Test

/**
 */
class ChecksumFileServiceUnitTests extends GroovyTestCase {

    ChecksumFileService checksumFileService

    final String BAM_FILE_NAME = "bamFileName.merged.mdup.bam"

    @Test
    void testMd5FileName() {
        assertEquals("${BAM_FILE_NAME}.md5sum", checksumFileService.md5FileName(BAM_FILE_NAME))
    }

    @Test
    void testPicardMd5FileName() {
        assertEquals("${BAM_FILE_NAME}.md5", checksumFileService.picardMd5FileName(BAM_FILE_NAME))
    }
}

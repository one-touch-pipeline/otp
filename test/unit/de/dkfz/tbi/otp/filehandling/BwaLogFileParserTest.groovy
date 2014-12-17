package de.dkfz.tbi.otp.filehandling

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.utils.CreateFileHelper

/**
 */
class BwaLogFileParserTest extends GroovyTestCase {

    void testParseReadNumberFromLog_WhenContainsCorrectLines_ShouldReturnReadNumber() {

        File testDirectory = TestCase.createEmptyTestDirectory()
        File testLogFile = new File(testDirectory, 'bwaAlnTestLog.log')

        final long expectedReadNumber = 44402390

        CreateFileHelper.createFile(testLogFile, """\
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
[cnybwa_aln_core]  41943495 sequences processed... (io rate: 124.19 mb/s)
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
[cnybwa_aln_core]  42992992 sequences processed... (io rate: 119.31 mb/s)
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
[cnyaln_bwa_read_seq] 0.5% bases are trimmed.
[cnyaln_bwa_read_seq] 0.5% bases are trimmed.
[cnyaln_bwa_read_seq] 0.7% bases are trimmed.
[cnybwa_aln_core]  44041113 sequences processed... (io rate: 117.61 mb/s)
[cnyaln_bwa_read_seq] 0.5% bases are trimmed.
[cnybwa_aln_core]  44402390 sequences processed... (io rate: 118.49 mb/s)
cnybwa aln core time 159.06 sec
cnybwa overall time  162.99 sec
""")
        try {
            assert expectedReadNumber == BwaLogFileParser.parseReadNumberFromLog(testLogFile)
        } finally {
            testLogFile.delete()
            testDirectory.delete()
        }
    }

    void testParseReadNumberFromLog_WhenContainsNoMatchingLines_ShouldThrowException() {

        File testDirectory = TestCase.createEmptyTestDirectory()
        File testLogFile = new File(testDirectory, 'bwaAlnTestLog.log')

        CreateFileHelper.createFile(testLogFile, """\
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
[cnyaln_bwa_read_seq] 0.4% bases are trimmed.
cnybwa aln core time 159.06 sec
cnybwa overall time  162.99 sec
""")
        try {
            shouldFail RuntimeException, { BwaLogFileParser.parseReadNumberFromLog(testLogFile) }
        } finally {
            testLogFile.delete()
            testDirectory.delete()
        }
    }
}

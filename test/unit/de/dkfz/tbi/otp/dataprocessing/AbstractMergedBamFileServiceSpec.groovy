package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.utils.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

class AbstractMergedBamFileServiceSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder


    void "getExistingBamFilePath, when all fine, return the file"() {
        given:
        File file = CreateFileHelper.createFile(temporaryFolder.newFile())
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            1 * getPathForFurtherProcessing() >> file
            1 * getMd5sum() >> HelperUtils.randomMd5sum
            2 * getFileSize() >> file.size()
            0 * _
        }

        when:
        File existingFile = service.getExistingBamFilePath(bamFile)

        then:
        file == existingFile
    }


    @Unroll
    void "getExistingBamFilePath, when fail for #failCase, throw an exception"() {
        given:
        String errorMessage = HelperUtils.uniqueString
        File file = CreateFileHelper.createFile(temporaryFolder.newFile())
        AbstractMergedBamFileService service = new AbstractMergedBamFileService()
        AbstractMergedBamFile bamFile = Mock(AbstractMergedBamFile) {
            _ * getPathForFurtherProcessing() >> {
                if (failCase == 'exceptionInFurtherProcessingPath') {
                    throw new AssertionError('getPathForFurtherProcessing fail')
                } else if (failCase == 'fileNotExist') {
                    TestCase.uniqueNonExistentPath
                } else {
                    file
                }

            }
            _ * getMd5sum() >> {
                failCase == 'invalidMd5sum' ? 'invalid' : HelperUtils.randomMd5sum
            }
            _ * getFileSize() >> {
                failCase == 'fileSizeZero' ? 0 :
                        failCase == 'fileSizeWrong' ? 1234 : file.length()
            }
            0 * _
        }

        when:
        service.getExistingBamFilePath(bamFile)

        then:
        AssertionError e = thrown()
        e.message.contains(exception)

        where:
        failCase                           || exception
        'exceptionInFurtherProcessingPath' || 'getPathForFurtherProcessing fail'
        'fileNotExist'                     || 'not found.. Expression: de.dkfz.tbi.otp.utils.ThreadUtils.waitFor'
        'invalidMd5sum'                    || 'assert bamFile.getMd5sum()'
        'fileSizeZero'                     || 'assert bamFile.getFileSize()'
        'fileSizeWrong'                    || 'assert file.length() == bamFile.getFileSize()'
    }

}

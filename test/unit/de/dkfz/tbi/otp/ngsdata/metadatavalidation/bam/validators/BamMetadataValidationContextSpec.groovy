package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class BamMetadataValidationContextSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    BamMetadataValidationContext context
    Problems problems

    def setup() {
        context = BamMetadataValidationContextFactory.createContext()
        problems = new Problems()
    }

    void "checkFilesInDirectory, when a folder is empty, add a warning"() {
        given:
        File emptyFolder = temporaryFolder.newFolder()

        when:
        context.checkFilesInDirectory(emptyFolder, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.WARNING
        problem.message.contains("is empty.")
    }

    void "checkFilesInDirectory, when find a subfolder, check the content"() {
        given:
        File folder = temporaryFolder.newFolder()
        File subfolder = new File(folder, "subfolder")
        assert subfolder.mkdirs()
        File file2 = new File(subfolder, "file2.txt")
        file2.write("something other")

        when:
        context.checkFilesInDirectory(folder, problems)

        then:
        problems.getProblems().empty
    }

    void "checkFile, when is not readable, add the corresponding problem"() {
        given:
        File notReadAble = temporaryFolder.newFile('notReadable.txt')
        assert ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        when:
        context.checkFile(notReadAble, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.ERROR
        problem.message.contains("is not readable.")
    }

    void "checkFile, when is empty, add the corresponding problem"() {
        given:
        File emptyFile = temporaryFolder.newFile('emptyFile.txt')

        when:
        context.checkFile(emptyFile, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.ERROR
        problem.message.contains("is empty.")
    }

}
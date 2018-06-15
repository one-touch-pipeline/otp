package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*

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
        Path emptyFolder = temporaryFolder.newFolder().toPath()

        when:
        context.checkFilesInDirectory(emptyFolder, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.WARNING
        problem.message.contains("is empty.")
    }

    void "checkFilesInDirectory, when find a subfolder, check the content"() {
        given:
        Path folder = temporaryFolder.newFolder().toPath()
        Path subfolder = folder.resolve("subfolder")
        assert Files.createDirectories(subfolder)
        Path file2 = subfolder.resolve("file2.txt")
        file2.write("something other")

        when:
        context.checkFilesInDirectory(folder, problems)

        then:
        problems.getProblems().empty
    }

    void "checkFile, when is not readable, add the corresponding problem"() {
        given:
        Path notReadAble = temporaryFolder.newFile('notReadable.txt').toPath()
        assert ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.toAbsolutePath()} && echo OK").trim() == 'OK'
        assert !Files.isReadable(notReadAble)

        when:
        context.checkFile(notReadAble, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.ERROR
        problem.message.contains("is not readable.")
    }

    void "checkFile, when is empty, add the corresponding problem"() {
        given:
        Path emptyFile = temporaryFolder.newFile('emptyFile.txt').toPath()

        when:
        context.checkFile(emptyFile, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.WARNING
        problem.message.contains("is empty.")
    }

    void "checkFile, when is to large, add the corresponding problem"() {
        given:
        Files.metaClass.static.size = { Path path ->
            return AbstractMetadataValidationContext.MAX_ADDITIONAL_FILE_SIZE_IN_GIB * 1024L * 1024L * 1024L + 1
        }
        Path bigFile = temporaryFolder.newFile('bigFile.txt').toPath()

        when:
        context.checkFile(bigFile, problems)

        then:
        Problem problem = exactlyOneElement(problems.getProblems())
        problem.level == Level.WARNING
        problem.message.contains("is larger than ${AbstractMetadataValidationContext.MAX_ADDITIONAL_FILE_SIZE_IN_GIB} GiB.")

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(Files)
    }

}

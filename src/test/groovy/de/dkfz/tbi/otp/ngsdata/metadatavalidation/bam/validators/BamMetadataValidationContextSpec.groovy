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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

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
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.toAbsolutePath()} && echo OK").trim() == 'OK'
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

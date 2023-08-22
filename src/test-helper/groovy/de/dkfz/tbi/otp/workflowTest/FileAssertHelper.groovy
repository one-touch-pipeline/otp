/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowTest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Files
import java.nio.file.Path

@Component
class FileAssertHelper {

    @Autowired
    FileService fileService

    @Autowired
    ConfigService configService

    void assertPathIsReadable(final Path file) {
        assert file.absolute
        assert fileService.fileIsReadable(file, configService.defaultRealm)
    }

    void assertFileIsReadable(final Path file) {
        assertPathIsReadable(file)
        assert Files.isRegularFile(file)
    }

    void assertFileIsReadableAndNotEmpty(final Path file) {
        assertFileIsReadable(file)
        assert Files.size(file) > 0L
    }

    void assertDirectoryIsReadable(final Path dir) {
        assertPathIsReadable(dir)
        assert Files.isDirectory(dir)
    }

    void assertDirectoryIsReadableAndExecutable(final Path dir) {
        assertDirectoryIsReadable(dir)
        assert Files.isExecutable(dir)
    }

    void assertDirectoryContent(Path baseDir, List<Path> expectedDirs, List<Path> expectedFiles = [], List<Path> expectedLinks = []) {
        expectedDirs.each {
            assertDirectoryIsReadableAndExecutable(it)
        }
        expectedFiles.each {
            assertFileIsReadableAndNotEmpty(it)
        }
        expectedLinks.each {
            assertPathIsReadable(it)
            assert Files.isSymbolicLink(it)
        }

        Set<Path> expectedEntries = (expectedDirs + expectedFiles + expectedLinks).findAll {
            it.parent == baseDir
        } as Set
        Set<Path> foundEntries = fileService.findAllFilesInPath(baseDir)
        assert expectedEntries == foundEntries
    }
}

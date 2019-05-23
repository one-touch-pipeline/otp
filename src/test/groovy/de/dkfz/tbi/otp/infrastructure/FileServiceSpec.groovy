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

package de.dkfz.tbi.otp.infrastructure

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermission

@SuppressWarnings('MethodCount')
class FileServiceSpec extends Specification implements DataTest {

    static final String SOME_CONTENT = 'SomeContent'

    static final byte[] SOME_BYTE_CONTENT = "SomeByteContent".bytes

    @Shared
    FileService fileService = new FileService()

    @Rule
    TemporaryFolder temporaryFolder


    void "setPermission, if directory does not exist, but the parent directory exists, then create directory"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()

        when:
        fileService.setPermission(basePath, FileService.OWNER_DIRECTORY_PERMISSION)

        then:
        Files.getPosixFilePermissions(basePath) == FileService.OWNER_DIRECTORY_PERMISSION

        when:
        fileService.setPermission(basePath, FileService.DEFAULT_DIRECTORY_PERMISSION)

        then:
        Files.getPosixFilePermissions(basePath) == FileService.DEFAULT_DIRECTORY_PERMISSION
    }

    //----------------------------------------------------------------------------------------------------
    // test for createDirectoryRecursively


    void "createDirectoryRecursively, if directory does not exist, but the parent directory exists, then create directory"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newPath = basePath.resolve('newFolder')

        when:
        fileService.createDirectoryRecursively(newPath)

        then:
        assertDirectory(newPath)
    }

    void "createDirectoryRecursively, if directory and parent directory do not exist, then create directory recursively"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newPath = basePath.resolve('newFolder/newSubFolder')

        when:
        fileService.createDirectoryRecursively(newPath)

        then:
        assertDirectory(newPath.parent)
        assertDirectory(newPath)
    }

    void "createDirectoryRecursively, if directory already exists, then do not fail"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        assert Files.exists(path)

        when:
        fileService.createDirectoryRecursively(path)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "createDirectoryRecursively, if parameter is #cases, throw assertion"() {
        when:
        fileService.createDirectoryRecursively(path)

        then:
        thrown(AssertionError)

        where:
        cases                | path
        'null'               | null
        'only one component' | new File('path').toPath()
        'relative path'      | new File('relative/path').toPath()
    }

    void "createDirectoryRecursively, if path is a file, then throw assertion"() {
        given:
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)

        when:
        fileService.createDirectoryRecursively(filePath)

        then:
        thrown(AssertionError)
    }

    void "createDirectoryRecursively, if a parent of path is a file, then throw assertion"() {
        given:
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newDirectory = filePath.resolve('newDirectory')

        when:
        fileService.createDirectoryRecursively(newDirectory)

        then:
        thrown(AssertionError)
    }

    private void assertDirectory(Path path) {
        assert Files.exists(path)
        assert Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path)

        assert permissions.contains(PosixFilePermission.OWNER_READ)
        assert permissions.contains(PosixFilePermission.OWNER_WRITE)
        assert permissions.contains(PosixFilePermission.OWNER_EXECUTE)

        assert permissions.contains(PosixFilePermission.GROUP_READ)
        assert !permissions.contains(PosixFilePermission.GROUP_WRITE)
        assert permissions.contains(PosixFilePermission.GROUP_EXECUTE)

        assert !permissions.contains(PosixFilePermission.OTHERS_READ)
        assert !permissions.contains(PosixFilePermission.OTHERS_WRITE)
        assert !permissions.contains(PosixFilePermission.OTHERS_EXECUTE)
    }

    //----------------------------------------------------------------------------------------------------
    // test for deleteDirectoryRecursively


    void "deleteDirectoryRecursively, if path does not exist, then do nothing"() {
        given:
        Path filePath = temporaryFolder.newFolder().toPath()
        Path file = filePath.resolve('file')

        when:
        fileService.deleteDirectoryRecursively(file)

        then:
        notThrown()
    }

    void "deleteDirectoryRecursively, if path is an empty directory, then delete it"() {
        given:
        Path filePath = temporaryFolder.newFolder().toPath()

        when:
        fileService.deleteDirectoryRecursively(filePath)

        then:
        !Files.exists(filePath)
    }

    void "deleteDirectoryRecursively, if path is an file, then delete it"() {
        given:
        Path filePath = temporaryFolder.newFile().toPath()

        when:
        fileService.deleteDirectoryRecursively(filePath)

        then:
        !Files.exists(filePath)
    }

    void "deleteDirectoryRecursively, if path is link, then delete it"() {
        given:
        Path filePath = temporaryFolder.newFolder().toPath()
        Path file = filePath.resolve('file')
        Path link = filePath.resolve('link')
        Files.createSymbolicLink(link, file)

        when:
        fileService.deleteDirectoryRecursively(link)

        then:
        !Files.exists(link)
    }

    void "deleteDirectoryRecursively, if path contains a directory structure, then delete it with all content recursively"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path linkedFolder = temporaryFolder.newFolder().toPath()
        Path linkedFile = temporaryFolder.newFile().toPath()

        Path dir1 = basePath.resolve('dir1')
        Path subDir = dir1.resolve('subDir')

        Path dir2 = basePath.resolve('dir2')
        Path file = dir2.resolve('file')

        Path linkToFolder = dir2.resolve('linkToDir')
        Path linkToFile = dir2.resolve('linkToFile')

        [
                dir1,
                dir2,
                subDir,
        ].each {
            Files.createDirectories(it)
        }
        file.text = 'text'
        Files.createSymbolicLink(linkToFolder, linkedFolder)
        Files.createSymbolicLink(linkToFile, linkedFile)

        when:
        fileService.deleteDirectoryRecursively(basePath)

        then:
        !Files.exists(basePath)
        Files.exists(linkedFile)
        Files.exists(linkedFolder)
    }

    void "deleteDirectoryRecursively, if path is not absolute, then throw an assertion"() {
        given:
        Path file = Paths.get('abc')

        when:
        fileService.deleteDirectoryRecursively(file)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createFileWithContent using characters


    void "createFileWithContent, if file does not exist, then create file with given context"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT)

        then:
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    void "createFileWithContent, if parent directory and file do not exist, then create directory and file"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT)

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    @Unroll
    void "createFileWithContent, if parameter is #cases, throw assertion"() {
        when:
        fileService.createFileWithContent(path, SOME_CONTENT)

        then:
        thrown(AssertionError)

        where:
        cases                | path
        'null'               | null
        'only one component' | new File('path').toPath()
        'relative path'      | new File('relative/path').toPath()
    }


    void "createFileWithContent, if file already exists, then throw assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_CONTENT)

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent, if a parent of path is a file, then throw assertion"() {
        given:
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT)

        then:
        thrown(AssertionError)
    }

    private void assertFile(Path path) {
        assert Files.exists(path)
        assert Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path)

        assert permissions.contains(PosixFilePermission.OWNER_READ)
        assert !permissions.contains(PosixFilePermission.OWNER_WRITE)
        assert !permissions.contains(PosixFilePermission.OWNER_EXECUTE)

        assert permissions.contains(PosixFilePermission.GROUP_READ)
        assert !permissions.contains(PosixFilePermission.GROUP_WRITE)
        assert !permissions.contains(PosixFilePermission.GROUP_EXECUTE)

        assert !permissions.contains(PosixFilePermission.OTHERS_READ)
        assert !permissions.contains(PosixFilePermission.OTHERS_WRITE)
        assert !permissions.contains(PosixFilePermission.OTHERS_EXECUTE)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createFileWithContent using bytes


    void "createFileWithContent (byte), if file does not exist, then create file with given context"() {
        given:

        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    void "createFileWithContent (byte), if parent directory and file do not exist, then create directory and file"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    @Unroll
    void "createFileWithContent (byte), if parameter is #cases, throw assertion"() {
        when:
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT)

        then:
        thrown(AssertionError)

        where:
        cases                | path
        'null'               | null
        'only one component' | new File('path').toPath()
        'relative path'      | new File('relative/path').toPath()
    }

    void "createFileWithContent (byte), if file already exists, then throw assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT)

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent (byte), if a parent of path is a file, then throw assertion"() {
        given:
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createLink


    void "createLink, if input is valid, then create link"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path file = basePath.resolve('file')
        Path link = basePath.resolve('link')

        file.text = 'text'

        when:
        fileService.createLink(link, file, null, CreateLinkOption.ABSOLUTE)

        then:
        Files.isSymbolicLink(link)
        Files.readSymbolicLink(link).isAbsolute()
        Files.readSymbolicLink(link) == file
    }

    @Unroll
    void "createLink, if input is #type, then throw an assertion"() {
        given:
        Path file = fileName ? Paths.get(fileName) : null
        Path link = linkName ? Paths.get(linkName) : null

        when:
        fileService.createLink(link, file, null, CreateLinkOption.ABSOLUTE)

        then:
        AssertionError e = thrown()
        e.getMessage().contains(message)

        where:
        type                   | fileName          | linkName    || message
        'file is null'         | null              | '/somthing' || 'existingPath'
        'link is null'         | '/tmp'            | null        || 'linkPath'
        'file is not absolute' | 'tmp'             | '/somthing' || 'existingPath.absolute'
        'link is not absolute' | '/tmp'            | 'somthing'  || 'linkPath.absolute'
        'file does not exist'  | '/somthingTarget' | '/somthing' || 'Files.exists(existingPath)'
        'link does exist'      | '/tmp'            | '/tmp'      || '!Files.exists(linkPath)'
    }

    void "createRelativeLink, if input is valid, then create link"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path file = basePath.resolve('file')
        Path link = basePath.resolve('link')

        file.text = 'text'

        when:
        fileService.createLink(link, file, null)

        then:
        Files.isSymbolicLink(link)
        !Files.readSymbolicLink(link).isAbsolute()
        link.parent.resolve(Files.readSymbolicLink(link)).normalize() == file
    }


    @Unroll
    void "createRelativeLink, if input is #type, then throw an assertion"() {
        given:
        Path file = fileName ? Paths.get(fileName) : null
        Path link = linkName ? Paths.get(linkName) : null

        when:
        fileService.createLink(link, file, null)

        then:
        AssertionError e = thrown()
        e.getMessage().contains(message)

        where:
        type                   | fileName          | linkName    || message
        'file is null'         | null              | '/somthing' || 'existingPath'
        'link is null'         | '/tmp'            | null        || 'linkPath'
        'file is not absolute' | 'tmp'             | '/somthing' || 'existingPath.absolute'
        'link is not absolute' | '/tmp'            | 'somthing'  || 'linkPath.absolute'
        'file does not exist'  | '/somthingTarget' | '/somthing' || 'Files.exists(existingPath)'
        'link does exist'      | '/tmp'            | '/tmp'      || '!Files.exists(linkPath)'
    }

    //----------------------------------------------------------------------------------------------------
    // test for moveFile


    void "moveFile, if input is valid, then move file"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path oldFile = basePath.resolve('oldFile')
        Path newFile = basePath.resolve('newFile')

        oldFile.text = 'text'

        when:
        fileService.moveFile(oldFile, newFile)

        then:
        Files.exists(newFile)
        !Files.exists(oldFile)
    }

    @Unroll
    void "moveFile, if input is #type, then throw an assertion"() {
        given:
        Path oldFile = oldFileName ? Paths.get(oldFileName) : null
        Path newFile = newFileName ? Paths.get(newFileName) : null

        when:
        fileService.moveFile(oldFile, newFile)

        then:
        AssertionError e = thrown()
        e.getMessage().contains(message)

        where:
        type                      | oldFileName       | newFileName || message
        'oldFile is null'         | null              | '/somthing' || 'source'
        'newFile is null'         | '/tmp'            | null        || 'destination'
        'oldFile is not absolute' | 'tmp'             | '/somthing' || 'source.absolute'
        'newFile is not absolute' | '/tmp'            | 'somthing'  || 'destination.absolute'
        'oldFile does not exist'  | '/somthingTarget' | '/somthing' || 'Files.exists(source)'
        'newFile does exist'      | '/tmp'            | '/tmp'      || '!Files.exists(destination)'
    }

    //----------------------------------------------------------------------------------------------------
    // test for correctPathPermissionRecursive


    void "correctPathPermissionRecursive, correct permission of output folder"() {
        given:
        Path filePath = temporaryFolder.newFolder().toPath()
        Path dir1 = filePath.resolve('dir1')
        Path subDir = dir1.resolve('subDir')

        Path dir2 = filePath.resolve('dir2')
        Path file = dir2.resolve('file')
        Path bamFile = dir2.resolve('file.bam')
        Path baiFile = dir2.resolve('file.bam.bai')

        Path link = dir2.resolve('link')

        [
                dir1,
                dir2,
                subDir,
        ].each {
            Files.createDirectory(it)
            Files.setPosixFilePermissions(it, FileService.OWNER_DIRECTORY_PERMISSION)
        }
        [
                file,
                bamFile,
                baiFile,
        ].each {
            it.text = 'text'
            Files.setPosixFilePermissions(it, [] as Set)
        }

        Files.createSymbolicLink(link, file)

        when:
        fileService.correctPathPermissionRecursive(filePath)

        then:
        Files.getPosixFilePermissions(filePath) == FileService.DEFAULT_DIRECTORY_PERMISSION
        Files.getPosixFilePermissions(dir1) == FileService.DEFAULT_DIRECTORY_PERMISSION
        Files.getPosixFilePermissions(dir2) == FileService.DEFAULT_DIRECTORY_PERMISSION

        Files.getPosixFilePermissions(file) == FileService.DEFAULT_FILE_PERMISSION
        Files.getPosixFilePermissions(baiFile) == FileService.DEFAULT_BAM_FILE_PERMISSION
        Files.getPosixFilePermissions(baiFile) == FileService.DEFAULT_BAM_FILE_PERMISSION
    }

    //----------------------------------------------------------------------------------------------------
    // test for isFileReadableAndNotEmpty

    void "isFileReadableAndNotEmpty, if file exists and has content, then return true"() {
        given:
        Path file = temporaryFolder.newFile().toPath()
        file.text = SOME_CONTENT

        expect:
        FileService.isFileReadableAndNotEmpty(file)
    }

    void "isFileReadableAndNotEmpty, if file exists but is empty, then return false"() {
        given:
        Path file = temporaryFolder.newFile().toPath()
        file.text = ''

        expect:
        !FileService.isFileReadableAndNotEmpty(file)
    }

    void "isFileReadableAndNotEmpty, if file exists and has content, but is not readable, then return false"() {
        given:
        Path file = temporaryFolder.newFile().toPath()
        file.text = SOME_CONTENT
        Files.setPosixFilePermissions(file, [] as Set)

        expect:
        !FileService.isFileReadableAndNotEmpty(file)
    }

    void "isFileReadableAndNotEmpty, if file does not exist, then return false"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path nonExistingFile = path.resolve('i-shouldnt-exist.tmp')

        expect:
        !FileService.isFileReadableAndNotEmpty(nonExistingFile)
    }

    void "isFileReadableAndNotEmpty, if path is a directory, then return false"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()

        expect:
        !FileService.isFileReadableAndNotEmpty(path)
    }

    void "isFileReadableAndNotEmpty, if path is a link, then return false"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('link')
        Files.createSymbolicLink(file, path)

        expect:
        !FileService.isFileReadableAndNotEmpty(file)
    }

    //----------------------------------------------------------------------------------------------------
    //test for isFileReadableAndNotEmpty


    void "ensureFileIsReadableAndNotEmpty, if file exists and has content, then return without error"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = SOME_CONTENT

        when:
        FileService.ensureFileIsReadableAndNotEmpty(path)

        then:
        noExceptionThrown()
    }

    void "ensureFileIsReadableAndNotEmpty, if file exists but is empty, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = ''

        when:
        FileService.ensureFileIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if file exists and has content, but is not readable, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = SOME_CONTENT
        Files.setPosixFilePermissions(path, [] as Set)

        when:
        FileService.ensureFileIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if file does not exist, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('file')

        when:
        FileService.ensureFileIsReadableAndNotEmpty(file)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if path is a directory, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()

        when:
        FileService.ensureFileIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)

    }

    void "ensureFileIsReadableAndNotEmpty, if path is a link to a folder, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('link')
        Files.createSymbolicLink(file, path)

        when:
        FileService.ensureFileIsReadableAndNotEmpty(file)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()

        when:
        FileService.ensureFileIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }


    void "ensureDirIsReadableAndNotEmpty, if directory exists and has content, then return without error"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Files.createDirectory(path.resolve('newFolder'))

        when:
        FileService.ensureDirIsReadableAndNotEmpty(path)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadableAndNotEmpty, if directory exists but is empty, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()

        when:
        FileService.ensureDirIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if directory exist and has content, but is not readable, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Files.createDirectory(path.resolve('newFolder'))
        Files.setPosixFilePermissions(path, [] as Set)

        when:
        FileService.ensureDirIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }


    void "ensureDirIsReadableAndNotEmpty, if directory does not exist, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path newFolder = path.resolve('newFolder')

        when:
        FileService.ensureDirIsReadableAndNotEmpty(newFolder)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if path is a file, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()

        when:
        FileService.ensureDirIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)

    }

    void "ensureDirIsReadableAndNotEmpty, if path is a link to a file, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('link')
        Files.createSymbolicLink(file, temporaryFolder.newFile().toPath())

        when:
        FileService.ensureDirIsReadableAndNotEmpty(file)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()

        when:
        FileService.ensureDirIsReadableAndNotEmpty(path)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    // test for ensureDirIsReadable


    void "ensureDirIsReadable, if directory exists and has content, then return without error"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Files.createDirectory(path.resolve('newFolder'))

        when:
        FileService.ensureDirIsReadable(path)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadable, if directory exists and is empty, then return without error"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()

        when:
        FileService.ensureDirIsReadable(path)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadable, if directory exists and has content, but is not readable, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Files.createDirectory(path.resolve('newFolder'))
        Files.setPosixFilePermissions(path, [] as Set)

        when:
        FileService.ensureDirIsReadable(path)

        then:
        thrown(AssertionError)
    }


    void "ensureDirIsReadable, if directory does not exist, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path newFolder = path.resolve('newFolder')

        when:
        FileService.ensureDirIsReadable(newFolder)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadable, if path is a file, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFile().toPath()

        when:
        FileService.ensureDirIsReadable(path)

        then:
        thrown(AssertionError)

    }

    void "ensureDirIsReadable, if path is a link to a file, then throw an assertion"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('link')
        Files.createSymbolicLink(file, temporaryFolder.newFile().toPath())

        when:
        FileService.ensureDirIsReadable(file)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadable, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()

        when:
        FileService.ensureDirIsReadable(path)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createOrOverwriteScriptOutputFile

    void "createOrOverwriteScriptOutputFile, creates file if not already there"() {
        given:
        String newName = "new-script-file"
        Path newFolder = temporaryFolder.newFolder().toPath()
        Path newFile = newFolder.resolve(newName)
        assert !Files.exists(newFile)

        when:
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName)

        then:
        Files.exists(newFile)
    }

    void "createOrOverwriteScriptOutputFile, replaces pre-existing files"() {
        given:
        String newName = "new-script-file"
        Path newFolder = temporaryFolder.newFolder().toPath()
        Path newFile = newFolder.resolve(newName)
        newFile << SOME_CONTENT
        assert newFile.text == SOME_CONTENT

        when:
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName)

        then:
        newFile.text.isEmpty()
    }

    void "createOrOverwriteScriptOutputFile, new script is editable+executable for both user and group"() {
        given:
        String newName = "new-script-file"
        Path newFolder = temporaryFolder.newFolder().toPath()
        Path newFile = newFolder.resolve(newName)

        when:
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName)

        then:
        Files.getPosixFilePermissions(newFile).containsAll([
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.GROUP_EXECUTE,
        ])
    }
}

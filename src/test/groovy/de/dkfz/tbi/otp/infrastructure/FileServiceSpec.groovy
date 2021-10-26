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
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.LocalShellHelper

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission

class FileServiceSpec extends Specification implements DataTest {

    static final String SOME_CONTENT = 'SomeContent'

    static final byte[] SOME_BYTE_CONTENT = "SomeByteContent".bytes

    static private final Set<PosixFilePermission> POSIX_DIRECTORY_PERMISSION_PART = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
    ].toSet().asImmutable()

    @Shared
    FileService fileService = new FileService()

    @Rule
    TemporaryFolder temporaryFolder

    private void mockRemoteShellHelper() {
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            _ * executeCommandReturnProcessOutput(_, _) >> { Realm realm2, String command ->
                return LocalShellHelper.executeAndWait(command)
            }
            0 * _
        }
    }

    void "test convertPermissionsToOctalString"() {
        expect:
        output == fileService.convertPermissionsToOctalString(permission)

        where:
        permission                                                || output
        FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION || "770"
        FileService.OWNER_DIRECTORY_PERMISSION                    || "700"
        FileService.DEFAULT_FILE_PERMISSION                       || "440"
        FileService.OWNER_READ_WRITE_GROUP_READ_FILE_PERMISSION   || "640"
        FileService.DEFAULT_BAM_FILE_PERMISSION                   || "444"
    }

    @Unroll
    void "setPermission, if permission is set to #permission, then path has expected permission"() {
        given:
        Path basePath = temporaryFolder.newFile().toPath()
        Set<PosixFilePermission> permissions = [permission] as Set

        when:
        fileService.setPermission(basePath, permissions)

        then:
        Files.getPosixFilePermissions(basePath) == permissions

        where:
        permission << PosixFilePermission.values()
    }

    @Unroll
    void "setPermissionViaBash, if permission set to #permissionString, then path has expected permissionPosix"() {
        given:
        mockRemoteShellHelper()
        Path basePath = temporaryFolder.newFolder().toPath()
        Realm realm = new Realm()

        when:
        fileService.setPermissionViaBash(basePath, realm, permissionString)

        then:
        //sticky bit can't be checked via PosixFilePermission, so only posix part is checked
        TestCase.assertContainSame(Files.getPosixFilePermissions(basePath), permissionPosix)

        where:
        permissionString || permissionPosix
        '500'            || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE]
        '550'            || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE]
        '700'            || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE]
        '2700'           || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE]
    }

    @Unroll
    void "setGroupViaBash, if group is set to #group, then path has expected group"() {
        given:
        mockRemoteShellHelper()
        Path basePath = temporaryFolder.newFolder().toPath()
        Realm realm = new Realm()

        when:
        fileService.setGroupViaBash(basePath, realm, group)

        then:
        Files.getFileAttributeView(basePath, PosixFileAttributeView, LinkOption.NOFOLLOW_LINKS).readAttributes().group().name == group

        where:
        group << [
                new TestConfigService().testingGroup,
                new TestConfigService().workflowProjectUnixGroup,
        ]
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
        noExceptionThrown()
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
        fileService.createFileWithContent(newFile, SOME_CONTENT, new Realm())

        then:
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    void "createFileWithContent, if parent directory and file do not exist, then create directory and file"() {
        given:
        mockRemoteShellHelper()
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT, new Realm())

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    @Unroll
    void "createFileWithContent, if parameter is #cases, throw assertion"() {
        when:
        mockRemoteShellHelper()
        fileService.createFileWithContent(path, SOME_CONTENT, new Realm())

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
        mockRemoteShellHelper()
        Path path = temporaryFolder.newFile().toPath()
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_CONTENT, new Realm())

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent, if a parent of path is a file, then throw assertion"() {
        given:
        mockRemoteShellHelper()
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT, new Realm())

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
        mockRemoteShellHelper()
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT, new Realm())

        then:
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    void "createFileWithContent (byte), if parent directory and file do not exist, then create directory and file"() {
        given:
        mockRemoteShellHelper()
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT, new Realm())

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    @Unroll
    void "createFileWithContent (byte), if parameter is #cases, throw assertion"() {
        when:
        mockRemoteShellHelper()
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT, new Realm())

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
        mockRemoteShellHelper()
        Path path = temporaryFolder.newFile().toPath()
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT, new Realm())

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent (byte), if a parent of path is a file, then throw assertion"() {
        given:
        mockRemoteShellHelper()
        Path filePath = temporaryFolder.newFile().toPath()
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT, new Realm())

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createLink

    void "createLink, if input is valid, then create link"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path target = basePath.resolve('target')
        Path link = basePath.resolve('link')

        target.text = 'text'

        when:
        fileService.createLink(link, target, null, CreateLinkOption.ABSOLUTE)

        then:
        Files.isSymbolicLink(link)
        Files.readSymbolicLink(link).absolute
        Files.readSymbolicLink(link) == target
    }

    @Unroll
    void "createLink, if input is #type, then throw an assertion"() {
        given:
        Path target = targetName ? Paths.get(targetName) : null
        Path link = linkName ? Paths.get(linkName) : null

        when:
        fileService.createLink(link, target, null, CreateLinkOption.ABSOLUTE)

        then:
        AssertionError e = thrown()
        e.message.contains(message)

        where:
        type                     | targetName        | linkName    || message
        'target is null'         | null              | '/somthing' || 'target'
        'link is null'           | '/tmp'            | null        || 'link'
        'target is not absolute' | 'tmp'             | '/somthing' || 'target.absolute'
        'link is not absolute'   | '/tmp'            | 'somthing'  || 'link.absolute'
        'target does not exist'  | '/somthingTarget' | '/somthing' || 'Files.exists(target)'
        'link does exist'        | '/tmp'            | '/tmp'      || '!Files.exists(link, LinkOption.NOFOLLOW_LINKS)'
    }

    void "createRelativeLink, if input is valid, then create link"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path target = basePath.resolve('target')
        Path link = basePath.resolve('link')

        target.text = 'text'

        when:
        fileService.createLink(link, target, null)

        then:
        Files.isSymbolicLink(link)
        !Files.readSymbolicLink(link).absolute
        link.parent.resolve(Files.readSymbolicLink(link)).normalize() == target
    }

    @Unroll
    void "createRelativeLink, if input is #type, then throw an assertion"() {
        given:
        Path target = targetName ? Paths.get(targetName) : null
        Path link = linkName ? Paths.get(linkName) : null

        when:
        fileService.createLink(link, target, null)

        then:
        AssertionError e = thrown()
        e.message.contains(message)

        where:
        type                     | targetName        | linkName    || message
        'target is null'         | null              | '/somthing' || 'target'
        'link is null'           | '/tmp'            | null        || 'link'
        'target is not absolute' | 'tmp'             | '/somthing' || 'target.absolute'
        'link is not absolute'   | '/tmp'            | 'somthing'  || 'link.absolute'
        'target does not exist'  | '/somthingTarget' | '/somthing' || 'Files.exists(target)'
        'link does exist'        | '/tmp'            | '/tmp'      || '!Files.exists(link, LinkOption.NOFOLLOW_LINKS)'
    }

    @Unroll
    void "createRelativeLink, if linkPath already exist as '#name' and DELETE_EXISTING_FILE is given, then delete it and create link"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path target = basePath.resolve('target')
        Path link = basePath.resolve('link')

        target.text = 'text'

        callback(link)

        when:
        fileService.createLink(link, target, null, CreateLinkOption.DELETE_EXISTING_FILE)

        then:
        Files.isSymbolicLink(link)
        !Files.readSymbolicLink(link).absolute
        link.parent.resolve(Files.readSymbolicLink(link)).normalize() == target

        where:
        name   | callback
        'file' | { Path p -> p.text = 'File' }
        'link' | { Path p -> Files.createSymbolicLink(p, Paths.get('test')) }
    }

    @Unroll
    void "createRelativeLink, if linkPath already exist and is dir and DELETE_EXISTING_FILE is not given, then fail with assert"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path target = basePath.resolve('target')
        Path link = basePath.resolve('link')

        target.text = 'text'

        Files.createDirectory(link)

        when:
        fileService.createLink(link, target, null, CreateLinkOption.DELETE_EXISTING_FILE)

        then:
        AssertionError e = thrown()
        e.message.contains('!Files.exists(link, LinkOption.NOFOLLOW_LINKS)')
    }

    @Unroll
    void "createRelativeLink, if linkPath already exist as '#name' and DELETE_EXISTING_FILE is not given, then fail with assert"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path target = basePath.resolve('target')
        Path link = basePath.resolve('link')

        target.text = 'text'

        callback(link)

        when:
        fileService.createLink(link, target, null)

        then:
        AssertionError e = thrown()
        e.message.contains('!Files.exists(link, LinkOption.NOFOLLOW_LINKS)')

        where:
        name              | callback
        'target'          | { Path p -> p.text = 'File' }
        'link'            | { Path p -> Files.createSymbolicLink(p, Paths.get('test')) }
        'dir'             | { Path p -> Files.createDirectory(p) }
        'dir with target' | { Path p -> Files.createDirectory(p); p.resolve('child').text = 'ChildFile' }
        'dir with dir'    | { Path p -> Files.createDirectories(p.resolve('child')) }
    }

    //----------------------------------------------------------------------------------------------------
    // test for correctPathPermissionRecursive

    void "correctPathPermissionRecursive, correct permission of output folder"() {
        given:
        mockRemoteShellHelper()
        Path filePath = temporaryFolder.newFolder().toPath()
        String group = new TestConfigService().testingGroup
        Path dir1 = filePath.resolve('dir1')
        Path subDir = dir1.resolve('subDir')

        Path dir2 = filePath.resolve('dir2')
        Path file = dir2.resolve('file')
        Path bamFile = dir2.resolve('file.bam')
        Path baiFile = dir2.resolve('file.bam.bai')
        Realm realm = new Realm()

        Path link = dir2.resolve('link')

        List<Path> paths = [
                dir1,
                dir2,
                subDir,
        ].each {
            Files.createDirectory(it)
            Files.setPosixFilePermissions(it, FileService.OWNER_DIRECTORY_PERMISSION)
        } + [
                file,
                bamFile,
                baiFile,
        ].each {
            it.text = 'text'
            Files.setPosixFilePermissions(it, [] as Set)
        }

        Files.createSymbolicLink(link, file)

        when:
        fileService.correctPathPermissionAndGroupRecursive(filePath, realm, group)

        then:
        Files.getPosixFilePermissions(filePath) == POSIX_DIRECTORY_PERMISSION_PART
        Files.getPosixFilePermissions(dir1) == POSIX_DIRECTORY_PERMISSION_PART
        Files.getPosixFilePermissions(dir2) == POSIX_DIRECTORY_PERMISSION_PART

        Files.getPosixFilePermissions(file) == FileService.DEFAULT_FILE_PERMISSION
        Files.getPosixFilePermissions(baiFile) == FileService.DEFAULT_BAM_FILE_PERMISSION
        Files.getPosixFilePermissions(baiFile) == FileService.DEFAULT_BAM_FILE_PERMISSION

        paths.each {
            assert Files.getFileAttributeView(it, PosixFileAttributeView, LinkOption.NOFOLLOW_LINKS).readAttributes().group().name == group
        }
    }

    void "findFileInPath, file can be found with regular expression"() {
        given:
        Path expected = temporaryFolder.newFile("Test1_XYZ.csv").toPath()
        temporaryFolder.newFile("Test1_ABC.csv")

        String matcher = "Test[0-9]_XYZ.csv"

        expect:
        FileService.findFileInPath(temporaryFolder.root.toPath(), matcher) == expected
    }

    void "findFileInPath, file can not be found with regular expression"() {
        given:
        temporaryFolder.newFile("file.txt")

        when:
        FileService.findFileInPath(temporaryFolder.root.toPath(), "not-matching")

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any file with the filename matching/
    }

    void "findFileInPath, only matches filename, not absolute path"() {
        given:
        String matcher = "this-matches"
        Path parent = temporaryFolder.newFolder("${matcher}").toPath()
        temporaryFolder.newFile("${matcher}/file.txt")

        when:
        FileService.findFileInPath(parent, matcher)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any file with the filename matching/
    }

    @Unroll
    void "findAllFilesInPath, finds all files with regex: #matcher"() {
        given:
        List<Path> files = ["file1.txt", "file2.txt", "record3.txt"].collect {
            temporaryFolder.newFile(it).toPath()
        }

        when:
        List<Path> output = FileService.findAllFilesInPath(temporaryFolder.root.toPath(), matcher)

        then:
        CollectionUtils.containSame(files[expectedFiles], output)

        where:
        matcher || expectedFiles
        ".*"    || [0, 1, 2]
        "fi.*"  || [0, 1]
        "r.*"   || [2]
    }

    void "findAllFilesInPath, cannot find a file"() {
        given:
        temporaryFolder.newFile("fileNotFound.txt").toPath()

        when:
        FileService.findAllFilesInPath(temporaryFolder.root.toPath(), "wantToFindFile.txt")

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any files with their filenames matching/
    }

    void "findAllFilesInPath, only matches filename, not absolute path"() {
        given:
        String matcher = "this-matches"
        Path parent = temporaryFolder.newFolder("${matcher}").toPath()
        temporaryFolder.newFile("${matcher}/file1.txt")
        temporaryFolder.newFile("${matcher}/file2.txt")

        when:
        FileService.findAllFilesInPath(parent, matcher)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any files with their filenames matching/
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

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "ensureFileIsReadable, fails when the file does not exist"() {
        given:
        File file = temporaryFolder.newFile()
        file.delete()

        when:
        FileService.ensureFileIsReadable(file.toPath())

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadable, fails when file is no regular file"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()

        when:
        FileService.ensureFileIsReadable(path)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadable, fails when the file is not readable"() {
        given:
        File file = temporaryFolder.newFile()
        file.readable = false

        when:
        FileService.ensureFileIsReadable(file.toPath())

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    //test for isFileReadable

    void "isFileReadable, returns true if file is readable"() {
        given:
        Path path = temporaryFolder.newFile().toPath()

        expect:
        FileService.isFileReadable(path)
    }

    void "isFileReadable, returns false if file is not readable"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.toFile().readable = false

        expect:
        !FileService.isFileReadable(path)
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

    void "ensureDirIsReadableAndExecutable, succeed when isReadable and isExecutable"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        path.toFile().executable = true

        when:
        FileService.ensureDirIsReadableAndExecutable(path)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "ensureDirIsReadableAndExecutable, fail when file #errorCase"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        path.toFile().executable = executable
        path.toFile().readable = readable

        when:
        FileService.ensureDirIsReadableAndExecutable(path)

        then:
        thrown(AssertionError)

        where:
        errorCase        || executable | readable
        "not executable" || false      | true
        "not readable"   || true       | false
        "neither"        || false      | false
    }

    //----------------------------------------------------------------------------------------------------
    // test for ensurePathIsReadable

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "ensurePathIsReadable, fails when the path does not exist"() {
        given:
        Path file = temporaryFolder.newFolder().toPath()
        file.toFile().delete()

        when:
        FileService.ensurePathIsReadable(file)

        then:
        thrown(AssertionError)
    }

    void "ensurePathIsReadable, fails when the path is not readable"() {
        given:
        Path file = temporaryFolder.newFolder().toPath()
        file.toFile().readable = false

        when:
        FileService.ensurePathIsReadable(file)

        then:
        thrown(AssertionError)
    }

    void "ensurePathIsReadable, succeed when path is a file"() {
        given:
        Path file = temporaryFolder.newFile().toPath()

        when:
        FileService.ensurePathIsReadable(file)

        then:
        noExceptionThrown()
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

    void "readFileToString, returns the file content as a String"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = content

        expect:
        content == FileService.readFileToString(path, StandardCharsets.US_ASCII)

        where:
        content << [SOME_CONTENT, ""]
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
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName, new Realm())

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
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName, new Realm())

        then:
        newFile.text.empty
    }

    void "createOrOverwriteScriptOutputFile, new script is editable+executable for both user and group"() {
        given:
        String newName = "new-script-file"
        Path newFolder = temporaryFolder.newFolder().toPath()
        Path newFile = newFolder.resolve(newName)

        when:
        fileService.createOrOverwriteScriptOutputFile(newFolder, newName, new Realm())

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

    void "fileSizeExceeded, false if fileSize is larger than threshold"() {
        Path newFile = temporaryFolder.newFile().toPath()
        newFile.text = SOME_CONTENT

        expect:
        fileService.fileSizeExceeded(newFile.toFile(), 1)
    }

    void "fileSizeExceeded, true if fileSize is smaller than threshold"() {
        Path newFile = temporaryFolder.newFile().toPath()

        expect:
        !fileService.fileSizeExceeded(newFile.toFile(), newFile.size() + 1)
    }
}

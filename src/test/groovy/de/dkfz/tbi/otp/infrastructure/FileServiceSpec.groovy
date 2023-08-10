/*
 * Copyright 2011-2023 The OTP authors
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
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.*

import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions

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

    static private final Set<PosixFilePermission> PERMISSIONS_OWNER = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
    ].toSet().asImmutable()

    static private final Set<PosixFilePermission> PERMISSIONS_OWNER_GROUP = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
    ].toSet().asImmutable()

    FileService fileService = new FileService()

    @TempDir
    Path tempDir

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
        Path basePath = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        Set<PosixFilePermission> permissions = [permission] as Set

        when:
        fileService.setPermission(basePath, permissions)

        then:
        Files.getPosixFilePermissions(basePath) == permissions

        where:
        permission << PosixFilePermission.values()
    }

    void "setPermission, if permission can't be changed, then throw ChangeFilePermissionException"() {
        given:
        mockRemoteShellHelper()
        Set<PosixFilePermission> permissions = ['invalid'] as Set

        when:
        fileService.setPermission(tempDir, permissions)

        then:
        ChangeFilePermissionException e = thrown()
        e.message.contains(tempDir.toString())
        e.message.contains('invalid')
    }

    @Unroll
    void "setPermissionViaBash, if permission set to #permissionString, then path has expected permissionPosix"() {
        given:
        mockRemoteShellHelper()
        Realm realm = new Realm()

        when:
        fileService.setPermissionViaBash(tempDir, realm, permissionString)

        then:
        //sticky bit can't be checked via PosixFilePermission, so only posix part is checked
        TestCase.assertContainSame(Files.getPosixFilePermissions(tempDir), permissionPosix)

        where:
        permissionString || permissionPosix
        '500'            || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE]
        '550'            || [PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE]
        '700'            || PERMISSIONS_OWNER
        '2700'           || PERMISSIONS_OWNER
    }

    void "setPermissionViaBash, if permission can't be changed, then throw ChangeFilePermissionException"() {
        given:
        String permissionString = HelperUtils.uniqueString
        mockRemoteShellHelper()
        Realm realm = new Realm()

        when:
        fileService.setPermissionViaBash(tempDir, realm, permissionString)

        then:
        ChangeFilePermissionException e = thrown()
        e.message.contains(tempDir.toString())
        e.message.contains(permissionString)
    }

    @Unroll
    void "setGroupViaBash, if group is set to #group, then path has expected group"() {
        given:
        mockRemoteShellHelper()
        Realm realm = new Realm()

        when:
        fileService.setGroupViaBash(tempDir, realm, group)

        then:
        Files.getFileAttributeView(tempDir, PosixFileAttributeView, LinkOption.NOFOLLOW_LINKS).readAttributes().group().name == group

        where:
        group << [
                new TestConfigService().testingGroup,
                new TestConfigService().workflowProjectUnixGroup,
        ]
    }

    void "setGroupViaBash, if group is set to unknown group, then throw ChangeFileGroupException"() {
        given:
        mockRemoteShellHelper()
        Realm realm = new Realm()
        String group = HelperUtils.uniqueString

        when:
        fileService.setGroupViaBash(tempDir, realm, group)

        then:
        ChangeFileGroupException e = thrown()
        e.message.contains(tempDir.toString())
        e.message.contains(group)
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

    void "createDirectoryRecursivelyAndSetPermissionsViaBash, if path does not exist, then create it and set group and permission"() {
        given:
        mockRemoteShellHelper()
        TestConfigService configService = new TestConfigService()

        Realm realm = new Realm()
        String group = firstGroup ? configService.testingGroup : configService.workflowProjectUnixGroup

        Path directory1 = tempDir.resolve('directory1')
        Path directory2 = directory1.resolve('directory2')
        Path directory3 = directory2.resolve('directory3')

        when:
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(directory3, realm, group, permission)

        then:
        [
                directory1,
                directory2,
                directory3,
        ].each {
            assert Files.exists(it)
            assert Files.isDirectory(it)
            assert Files.getPosixFilePermissions(it) == permissionPosix
            assert Files.getFileAttributeView(it, PosixFileAttributeView, LinkOption.NOFOLLOW_LINKS).readAttributes().group().name == group
        }

        where:
        firstGroup | permission || permissionPosix
        true       | '700'      || PERMISSIONS_OWNER
        true       | '770'      || PERMISSIONS_OWNER_GROUP
        false      | '700'      || PERMISSIONS_OWNER
        false      | '770'      || PERMISSIONS_OWNER_GROUP
    }

    @Unroll
    void "createDirectoryRecursivelyAndSetPermissionsViaBash, when directory exist and #name, then throw #exception"() {
        given:
        mockRemoteShellHelper()
        TestConfigService configService = new TestConfigService()

        Realm realm = new Realm()
        String group = validGroup ? configService.testingGroup : HelperUtils.randomMd5sum

        Path filePath = tempDir.resolve("folder")
        Path directory = filePath.resolve(directoryName)

        CreateFileHelper.createFile(filePath.resolve('file'))

        when:
        fileService.createDirectoryRecursivelyAndSetPermissionsViaBash(directory, realm, group, permission)

        then:
        OtpFileSystemException e = thrown()
        e.class == exception
        e.message.contains(filePath.toString())

        cleanup:
        Set<PosixFilePermission> allPermissions = PosixFilePermissions.fromString("rwxrwxrwx")
        Files.setPosixFilePermissions(directory.parent, allPermissions)

        where:
        name                          | directoryName | validGroup | permission               || exception
        'directory is file'           | 'file'        | true       | '666'                    || CreateDirectoryException
        'directory based on file'     | 'file/dir'    | true       | '666'                    || CreateDirectoryException
        'group change fail'           | 'dir/dir'     | false      | '666'                    || ChangeFileGroupException
        'permission change fail'      | 'dir/dir'     | true       | HelperUtils.randomMd5sum || ChangeFilePermissionException
        'directory has no permission' | 'dir/dir'     | true       | '000'                    || CreateDirectoryException
    }

    //----------------------------------------------------------------------------------------------------
    // test for deleteDirectoryRecursively

    void "deleteDirectoryRecursively, if path does not exist, then do nothing"() {
        given:
        Path file = tempDir.resolve('file')

        when:
        fileService.deleteDirectoryRecursively(file)

        then:
        noExceptionThrown()
    }

    void "deleteDirectoryRecursively, if path is an empty directory, then delete it"() {
        when:
        fileService.deleteDirectoryRecursively(tempDir)

        then:
        !Files.exists(tempDir)
    }

    void "deleteDirectoryRecursively, if path is an file, then delete it"() {
        given:
        Path filePath = CreateFileHelper.createFile(tempDir.resolve("test.txt"))

        when:
        fileService.deleteDirectoryRecursively(filePath)

        then:
        !Files.exists(filePath)
    }

    void "deleteDirectoryRecursively, if path is link, then delete it"() {
        given:
        Path file = Files.createFile(tempDir.resolve('file'))
        Path link = tempDir.resolve('link')
        Files.createSymbolicLink(link, file)

        when:
        fileService.deleteDirectoryRecursively(link)

        then:
        !Files.exists(link)
    }

    void "deleteDirectoryRecursively, if path contains a directory structure, then delete it with all content recursively"() {
        given:
        Path basePath = Files.createDirectory(tempDir.resolve("folder"))
        Path linkedFolder = Files.createDirectory(tempDir.resolve("linkedFolder"))
        Path linkedFile = Files.createFile(tempDir.resolve("linkedFile.txt"))

        Path dir1 = Files.createDirectory(basePath.resolve('dir1'))
        Path subDir = Files.createDirectory(dir1.resolve('subDir'))

        Path dir2 = Files.createDirectory(basePath.resolve('dir2'))
        Path file = Files.createFile(dir2.resolve('file'))

        Path linkToFolder = dir2.resolve('linkToDir')
        Path linkToFile = dir2.resolve('linkToFile')

        file.text = 'text'
        Files.createSymbolicLink(linkToFolder, linkedFolder)
        Files.createSymbolicLink(linkToFile, linkedFile)

        when:
        fileService.deleteDirectoryRecursively(basePath)

        then:
        !Files.exists(basePath)
        !Files.exists(dir1)
        !Files.exists(subDir)
        !Files.exists(dir2)
        !Files.exists(file)
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
    // test for deleteDirectoryContent

    void "deleteDirectoryContent, if path does not exist, then do nothing"() {
        given:
        Path file = tempDir.resolve('file')

        when:
        fileService.deleteDirectoryContent(file)

        then:
        noExceptionThrown()
    }

    void "deleteDirectoryRecursively, if path is an empty directory, then do nothing"() {
        when:
        fileService.deleteDirectoryContent(tempDir)

        then:
        Files.exists(tempDir)
    }

    void "deleteDirectoryRecursively, if path is an file, then do nothing"() {
        given:
        Path filePath = CreateFileHelper.createFile(tempDir.resolve("test.txt"))

        when:
        fileService.deleteDirectoryContent(filePath)

        then:
        Files.exists(filePath)
    }

    void "deleteDirectoryRecursively, if path is link, then do nothing"() {
        given:
        Path file = Files.createFile(tempDir.resolve("file"))
        Path link = tempDir.resolve('link')
        Files.createSymbolicLink(link, file)

        when:
        fileService.deleteDirectoryContent(link)

        then:
        Files.exists(link)
        Files.exists(file)
        Files.isSymbolicLink(link)
    }

    void "deleteDirectoryContent, if path contains a directory structure, then delete all content recursively"() {
        given:
        Path basePath = Files.createDirectory(tempDir.resolve("folder"))
        Path file1 = Files.createFile(basePath.resolve('file1'))
        Path file2 = Files.createFile(basePath.resolve('file2'))

        Path linkedFolder = Files.createDirectory(tempDir.resolve("linkedFolder"))
        Path linkedFile = Files.createFile(tempDir.resolve("linkedFile.txt"))

        Path dir1 = Files.createDirectory(basePath.resolve('dir1'))
        Path subDir = Files.createDirectory(dir1.resolve('subDir'))

        Path dir2 = Files.createDirectory(basePath.resolve('dir2'))
        Path file = Files.createFile(dir2.resolve('file'))

        Path linkToFolder = dir2.resolve('linkToDir')
        Path linkToFile = dir2.resolve('linkToFile')

        file.text = 'text'
        Files.createSymbolicLink(linkToFolder, linkedFolder)
        Files.createSymbolicLink(linkToFile, linkedFile)

        when:
        fileService.deleteDirectoryContent(basePath)

        then:
        Files.exists(basePath)

        !Files.exists(dir1)
        !Files.exists(subDir)
        !Files.exists(dir2)
        !Files.exists(file)
        !Files.exists(file1)
        !Files.exists(file2)
        Files.exists(linkedFile)
        Files.exists(linkedFolder)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createFileWithContent using characters

    void "createFileWithContent, if file does not exist, then create file with given context"() {
        given:
        Path newFile = tempDir.resolve('newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT, new Realm())

        then:
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    void "createFileWithContent, if parent directory and file do not exist, then create directory and file"() {
        given:
        mockRemoteShellHelper()
        Path newFile = tempDir.resolve('newFolder/newFile')

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

    void "createFileWithContent, if file already exists and overwrite is false, then throw assertion"() {
        given:
        mockRemoteShellHelper()
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_CONTENT, new Realm())

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent, if file already exists and overwrite is true, then overwrite file content"() {
        given:
        String oldContent = "OLD CONTENT"
        mockRemoteShellHelper()
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"), oldContent)
        assert Files.exists(path)
        assert Files.isRegularFile(path)
        assert path.text == oldContent

        when:
        fileService.createFileWithContent(path, SOME_CONTENT, new Realm(), FileService.DEFAULT_FILE_PERMISSION, true)

        then:
        path.text == SOME_CONTENT
    }

    void "createFileWithContent, if a parent of path is a file, then throw CreateDirectoryException"() {
        given:
        mockRemoteShellHelper()
        Path filePath = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_CONTENT, new Realm())

        then:
        thrown(CreateDirectoryException)
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
        Path newFile = tempDir.resolve('newFile')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT, new Realm())

        then:
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    void "createFileWithContent (byte), if parent directory and file do not exist, then create directory and file"() {
        given:
        mockRemoteShellHelper()
        Path newFile = tempDir.resolve('newFolder/newFile')

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
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        assert Files.exists(path)
        assert Files.isRegularFile(path)

        when:
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT, new Realm())

        then:
        thrown(AssertionError)
    }

    void "createFileWithContent (byte), if file already exists and overwrite is true, then overwrite file content"() {
        given:
        mockRemoteShellHelper()
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        assert Files.exists(path)
        assert Files.isRegularFile(path)
        assert path.bytes != SOME_BYTE_CONTENT

        when:
        fileService.createFileWithContent(path, SOME_BYTE_CONTENT, new Realm(), FileService.DEFAULT_FILE_PERMISSION, true)

        then:
        path.bytes == SOME_BYTE_CONTENT
    }

    void "createFileWithContent (byte), if a parent of path is a file, then throw CreateDirectoryException"() {
        given:
        mockRemoteShellHelper()
        Path filePath = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        assert Files.exists(filePath)
        assert Files.isRegularFile(filePath)
        Path newFile = filePath.resolve('newDirectory')

        when:
        fileService.createFileWithContent(newFile, SOME_BYTE_CONTENT, new Realm())

        then:
        thrown(CreateDirectoryException)
    }

    //----------------------------------------------------------------------------------------------------
    // test for createLink

    void "createLink, if input is valid, then create link"() {
        given:
        Path target = tempDir.resolve('target')
        Path link = tempDir.resolve('link')

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
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(Realm, String) >> { realm, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

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
        Path target = tempDir.resolve('target')
        Path link = tempDir.resolve('link')

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
        Path target = tempDir.resolve('target')
        Path link = tempDir.resolve('link')

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
        Path target = tempDir.resolve('target')
        Path link = tempDir.resolve('link')

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
        Path target = tempDir.resolve('target')
        Path link = tempDir.resolve('link')

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
        String group = new TestConfigService().testingGroup
        Path dir1 = tempDir.resolve('dir1')
        Path subDir = dir1.resolve('subDir')

        Path dir2 = tempDir.resolve('dir2')
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
        fileService.correctPathPermissionAndGroupRecursive(tempDir, realm, group)

        then:
        Files.getPosixFilePermissions(tempDir) == POSIX_DIRECTORY_PERMISSION_PART
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
        Path expected = CreateFileHelper.createFile(tempDir.resolve("Test1_XYZ.csv"))
        CreateFileHelper.createFile(tempDir.resolve("Test1_ABC.csv"))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        String matcher = "Test[0-9]_XYZ.csv"

        expect:
        fileService.findFileInPath(tempDir, matcher, realm) == expected
    }

    void "findFileInPath, file can not be found with regular expression"() {
        given:
        CreateFileHelper.createFile(tempDir.resolve("file.txt"))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.findFileInPath(tempDir, "not-matching", realm)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any file with the filename matching/
    }

    void "findFileInPath, only matches filename, not absolute path"() {
        given:
        String matcher = "this-matches"
        Path parent = tempDir.resolve("${matcher}")
        CreateFileHelper.createFile(tempDir.resolve("${matcher}/file.txt"))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.findFileInPath(parent, matcher, realm)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any file with the filename matching/
    }

    @Unroll
    void "findAllFilesInPath, finds all files with regex: #matcher"() {
        given:
        Realm realm = new Realm()
        List<Path> files = ["file1.txt", "file2.txt", "record3.txt"].collect {
            CreateFileHelper.createFile(tempDir.resolve(it))
        }
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        List<Path> output = fileService.findAllFilesInPath(tempDir, matcher, realm)

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
        CreateFileHelper.createFile(tempDir.resolve("fileNotFound.txt"))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.findAllFilesInPath(tempDir, "wantToFindFile.txt", realm)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any files with their filenames matching/
    }

    void "findAllFilesInPath, only matches filename, not absolute path"() {
        given:
        String matcher = "this-matches"
        Path parent = tempDir.resolve("${matcher}")
        CreateFileHelper.createFile(tempDir.resolve("${matcher}/file1.txt"))
        CreateFileHelper.createFile(tempDir.resolve("${matcher}/file2.txt"))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.findAllFilesInPath(parent, matcher, realm)

        then:
        AssertionError e = thrown()
        e.message =~ /Cannot find any files with their filenames matching/
    }

    //----------------------------------------------------------------------------------------------------
    // test for isFileReadableAndNotEmpty

    void "isFileReadableAndNotEmpty, if file exists and has content, then return true"() {
        given:
        Path file = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        Realm realm = new Realm()
        file.text = SOME_CONTENT
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        expect:
        fileService.isFileReadableAndNotEmpty(file, realm)
    }

    void "isFileReadableAndNotEmpty, if file exists but is empty, then return false"() {
        given:
        Realm realm = new Realm()
        Path file = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        file.text = ''
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        expect:
        !fileService.isFileReadableAndNotEmpty(file, realm)
    }

    void "isFileReadableAndNotEmpty, if file exists and has content, but is not readable, then return false"() {
        given:
        Realm realm = new Realm()
        Path file = CreateFileHelper.createFile(tempDir.resolve("test.txt"), SOME_CONTENT)
        Files.setPosixFilePermissions(file, [] as Set)
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        expect:
        !fileService.isFileReadableAndNotEmpty(file, realm)
    }

    void "isFileReadableAndNotEmpty, if file does not exist, then return false"() {
        given:
        Realm realm = new Realm()
        Path nonExistingFile = tempDir.resolve('i-shouldnt-exist.tmp')

        expect:
        !fileService.isFileReadableAndNotEmpty(nonExistingFile, realm)
    }

    void "isFileReadableAndNotEmpty, if path is a directory, then return false"() {
        expect:
        Realm realm = new Realm()
        !fileService.isFileReadableAndNotEmpty(tempDir, realm)
    }

    void "isFileReadableAndNotEmpty, if path is a link to a file, then return false"() {
        given:
        Realm realm = new Realm()
        Path path = Files.createDirectory(tempDir.resolve("folder"))
        Path file = Files.createFile(path.resolve("file"))
        Path link = path.resolve("link")
        Files.createSymbolicLink(link, file)
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        expect:
        !fileService.isFileReadableAndNotEmpty(link, realm)
    }

    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    void "ensureFileIsReadable, fails when the file does not exist"() {
        given:
        File file = CreateFileHelper.createFile(tempDir.resolve("test.txt")).toFile()
        file.delete()
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadable(file.toPath(), realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadable, fails when file is no regular file"() {
        given:
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadable(tempDir, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadable, fails when the file is not readable"() {
        given:
        File file = CreateFileHelper.createFile(tempDir.resolve("test.txt")).toFile()
        file.readable = false
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureFileIsReadable(file.toPath(), realm)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------
    //test for isFileReadableAndNotEmpty

    void "ensureFileIsReadableAndNotEmpty, if file exists and has content, then return without error"() {
        given:
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        path.text = SOME_CONTENT
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureFileIsReadableAndNotEmpty(path, realm)

        then:
        noExceptionThrown()
    }

    void "ensureFileIsReadableAndNotEmpty, if file exists but is empty, then throw an assertion"() {
        given:
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        path.text = ''
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureFileIsReadableAndNotEmpty(path, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if file exists and has content, but is not readable, then throw an assertion"() {
        given:
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        path.text = SOME_CONTENT
        Files.setPosixFilePermissions(path, [] as Set)
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureFileIsReadableAndNotEmpty(path, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if file does not exist, then throw an assertion"() {
        given:
        Path file = tempDir.resolve('file')
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadableAndNotEmpty(file, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if path is a directory, then throw an assertion"() {
        given:
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadableAndNotEmpty(tempDir, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if path is a link to a folder, then throw an assertion"() {
        given:
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Path link = tempDir.resolve('link')
        Files.createSymbolicLink(link, dir)
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadableAndNotEmpty(link, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureFileIsReadableAndNotEmpty, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()
        Realm realm = new Realm()

        when:
        fileService.ensureFileIsReadableAndNotEmpty(path, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if directory exists and has content, then return without error"() {
        given:
        Files.createDirectory(tempDir.resolve('newFolder'))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadableAndNotEmpty(tempDir, realm)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadableAndNotEmpty, if directory exists but is empty, then throw an assertion"() {
        given:
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadableAndNotEmpty(tempDir, realm)

        then:
        thrown(AssertionError)
    }

    void "fileIsReadable, if directory is readable, then return true"() {
        given:
        mockRemoteShellHelper()
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Realm realm = new Realm()

        expect:
        fileService.fileIsReadable(dir, realm)
    }

    void "fileIsReadable, if file is readable, then return true"() {
        given:
        mockRemoteShellHelper()
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Realm realm = new Realm()

        expect:
        fileService.fileIsReadable(Files.createFile(dir.resolve("file")), realm)
    }

    void "fileIsReadable, if file is not readable, then return false"() {
        given:
        mockRemoteShellHelper()
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Path file = Files.createFile(dir.resolve("file"))
        Set<PosixFilePermission> noReadPermissions = PosixFilePermissions.fromString("-wx-wx-wx")
        Files.setPosixFilePermissions(file, noReadPermissions)
        Realm realm = new Realm()

        expect:
        !fileService.fileIsReadable(file, realm)
    }

    void "fileIsReadable, if directory is not readable, then return false"() {
        given:
        mockRemoteShellHelper()
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Set<PosixFilePermission> noReadPermissions = PosixFilePermissions.fromString("-wx-wx-wx")
        Files.setPosixFilePermissions(dir, noReadPermissions)
        Realm realm = new Realm()

        expect:
        !fileService.fileIsReadable(dir, realm)

        cleanup:
        // directory has to be set readable, that the tempDir can be deleted afterwards
        Set<PosixFilePermission> allPermissions = PosixFilePermissions.fromString("rwxrwxrwx")
        Files.setPosixFilePermissions(dir, allPermissions)
    }

    void "fileIsReadable, if path not exists, then return false"() {
        given:
        mockRemoteShellHelper()
        Realm realm = new Realm()

        expect:
        !fileService.fileIsReadable(Paths.get("/path/not/exists"), realm)
    }

    void "ensureDirIsReadableAndNotEmpty, if directory exist and has content, but is not readable, then throw an assertion"() {
        given:
        Set<PosixFilePermission> noReadPermissions = PosixFilePermissions.fromString("-wx-wx-wx")
        Path dir = Files.createDirectory(tempDir.resolve("folder"))
        Files.createFile(dir.resolve("file"))
        Files.setPosixFilePermissions(dir, noReadPermissions)
        assert Files.isDirectory(dir)
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadableAndNotEmpty(dir, realm)

        then:
        thrown(AssertionError)

        cleanup:
        // directory has to be set readable, that the tempDir can be deleted afterwards
        Set<PosixFilePermission> allPermissions = PosixFilePermissions.fromString("rwxrwxrwx")
        Files.setPosixFilePermissions(dir, allPermissions)
    }

    void "ensureDirIsReadableAndNotEmpty, if directory does not exist, then throw an assertion"() {
        given:
        Path newFolder = tempDir.resolve('newFolder')
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadableAndNotEmpty(newFolder, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if path is a file, then throw an assertion"() {
        given:
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadableAndNotEmpty(path, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if path is a link to a file, then throw an assertion"() {
        given:
        Path file = tempDir.resolve('link')
        Files.createSymbolicLink(file, CreateFileHelper.createFile(tempDir.resolve("test.txt")))
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadableAndNotEmpty(file, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndNotEmpty, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadableAndNotEmpty(path, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadableAndExecutable, succeed when isReadable and isExecutable"() {
        given:
        tempDir.toFile().executable = true
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadableAndExecutable(tempDir, realm)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "ensureDirIsReadableAndExecutable, fail when file #errorCase"() {
        given:
        Path path = tempDir.resolve("folder")
        path.toFile().executable = executable
        path.toFile().readable = readable
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadableAndExecutable(path, realm)

        then:
        thrown(AssertionError)

        where:
        errorCase        || executable | readable
        "not executable" || false      | true
        "not readable"   || true       | false
        "neither"        || false      | false
    }

    //----------------------------------------------------------------------------------------------------
    // test for ensureDirIsReadable

    void "ensureDirIsReadable, if directory exists and has content, then return without error"() {
        given:
        Files.createDirectory(tempDir.resolve('newFolder'))
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadable(tempDir, realm)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadable, if directory exists and is empty, then return without error"() {
        given:
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadable(tempDir, realm)

        then:
        noExceptionThrown()
    }

    void "ensureDirIsReadable, if directory exists and has content, but is not readable, then throw an assertion"() {
        given:
        Path path = Files.createDirectory(tempDir.resolve("folder"))
        Files.setPosixFilePermissions(path, [] as Set)
        Realm realm = new Realm()
        fileService.remoteShellHelper = Mock(RemoteShellHelper) {
            executeCommandReturnProcessOutput(_, _) >> { realm1, cmd -> LocalShellHelper.executeAndWait(cmd) }
        }

        when:
        fileService.ensureDirIsReadable(path, realm)

        then:
        thrown(AssertionError)

        cleanup:
        // directory has to be set readable, that the tempDir can be deleted afterwards
        Set<PosixFilePermission> allPermissions = PosixFilePermissions.fromString("rwxrwxrwx")
        Files.setPosixFilePermissions(path, allPermissions)
    }

    void "ensureDirIsReadable, if directory does not exist, then throw an assertion"() {
        given:
        Path newFolder = tempDir.resolve('newFolder')
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadable(newFolder, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadable, if path is a file, then throw an assertion"() {
        given:
        Path file = Files.createFile(tempDir.resolve("file.md"))
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadable(file, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadable, if path is a link to a file, then throw an assertion"() {
        given:
        Path link = tempDir.resolve('link')
        Path file = Files.createFile(tempDir.resolve("file.md"))
        Files.createSymbolicLink(link, file)
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadable(link, realm)

        then:
        thrown(AssertionError)
    }

    void "ensureDirIsReadable, if path is not absolute, then throw an assertion"() {
        given:
        Path path = new File('relativePath').toPath()
        Realm realm = new Realm()

        when:
        fileService.ensureDirIsReadable(path, realm)

        then:
        thrown(AssertionError)
    }

    void "readFileToString, returns the file content as a String"() {
        given:
        Path path = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        path.text = content

        expect:
        content == fileService.readFileToString(path, StandardCharsets.US_ASCII)

        where:
        content << [SOME_CONTENT, ""]
    }

    //----------------------------------------------------------------------------------------------------
    // test for createOrOverwriteScriptOutputFile

    void "createOrOverwriteScriptOutputFile, creates file if not already there"() {
        given:
        String newName = "new-script-file"
        Path newFile = tempDir.resolve(newName)
        assert !Files.exists(newFile)

        when:
        fileService.createOrOverwriteScriptOutputFile(tempDir, newName, new Realm())

        then:
        Files.exists(newFile)
    }

    void "createOrOverwriteScriptOutputFile, replaces pre-existing files"() {
        given:
        String newName = "new-script-file"
        Path newFile = tempDir.resolve(newName)
        newFile << SOME_CONTENT
        assert newFile.text == SOME_CONTENT

        when:
        fileService.createOrOverwriteScriptOutputFile(tempDir, newName, new Realm())

        then:
        newFile.text.empty
    }

    void "createOrOverwriteScriptOutputFile, new script is editable+executable for both user and group"() {
        given:
        String newName = "new-script-file"
        Path newFile = tempDir.resolve(newName)

        when:
        fileService.createOrOverwriteScriptOutputFile(tempDir, newName, new Realm())

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
        Path newFile = CreateFileHelper.createFile(tempDir.resolve("test.txt"))
        newFile.text = SOME_CONTENT

        expect:
        fileService.fileSizeExceeded(newFile, 1)
    }

    void "fileSizeExceeded, true if fileSize is smaller than threshold"() {
        Path newFile = CreateFileHelper.createFile(tempDir.resolve("test.txt"))

        expect:
        !fileService.fileSizeExceeded(newFile, newFile.size() + 1)
    }
}

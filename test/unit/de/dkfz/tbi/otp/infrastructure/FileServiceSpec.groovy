package de.dkfz.tbi.otp.infrastructure

import org.junit.*
import org.junit.rules.*
import spock.lang.*

import java.nio.file.*
import java.nio.file.attribute.*

class FileServiceSpec extends Specification {

    static final String SOME_CONTENT = 'SomeContent'

    static final byte[] SOME_BYTE_CONTENT = "SomeByteContent".bytes

    @Rule
    TemporaryFolder temporaryFolder


    void "createDirectoryRecursively, if directory does not exist, but the parent directory exists, then create directory"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newPath = basePath.resolve('newFolder')

        when:
        new FileService().createDirectoryRecursively(newPath)

        then:
        assertDirectory(newPath)
    }

    void "createDirectoryRecursively, if directory and parent directory do not exist, then create directory recursively"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newPath = basePath.resolve('newFolder/newSubFolder')

        when:
        new FileService().createDirectoryRecursively(newPath)

        then:
        assertDirectory(newPath.parent)
        assertDirectory(newPath)
    }

    void "createDirectoryRecursively, if directory already exists, then do not fail"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        assert Files.exists(path)

        when:
        new FileService().createDirectoryRecursively(path)

        then:
        noExceptionThrown()
    }

    @Unroll
    void "createDirectoryRecursively, if parameter is #cases, throw assertion"() {
        when:
        new FileService().createDirectoryRecursively(path)

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
        new FileService().createDirectoryRecursively(filePath)

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
        new FileService().createDirectoryRecursively(newDirectory)

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


    void "createFileWithContent, if file does not exist, then create file with given context"() {
        given:

        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFile')

        when:
        new FileService().createFileWithContent(newFile, SOME_CONTENT)

        then:
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    void "createFileWithContent, if parent directory and file do not exist, then create directory and file"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        new FileService().createFileWithContent(newFile, SOME_CONTENT)

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.text == SOME_CONTENT
    }

    @Unroll
    void "createFileWithContent, if parameter is #cases, throw assertion"() {
        when:
        new FileService().createFileWithContent(path, SOME_CONTENT)

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
        new FileService().createFileWithContent(path, SOME_CONTENT)

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
        new FileService().createFileWithContent(newFile, SOME_CONTENT)

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


    void "createFileWithContent (byte), if file does not exist, then create file with given context"() {
        given:

        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFile')

        when:
        new FileService().createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    void "createFileWithContent (byte), if parent directory and file do not exist, then create directory and file"() {
        given:
        Path basePath = temporaryFolder.newFolder().toPath()
        Path newFile = basePath.resolve('newFolder/newFile')

        when:
        new FileService().createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        assertDirectory(newFile.parent)
        assertFile(newFile)
        newFile.bytes == SOME_BYTE_CONTENT
    }

    @Unroll
    void "createFileWithContent (byte), if parameter is #cases, throw assertion"() {
        when:
        new FileService().createFileWithContent(path, SOME_BYTE_CONTENT)

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
        new FileService().createFileWithContent(path, SOME_BYTE_CONTENT)

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
        new FileService().createFileWithContent(newFile, SOME_BYTE_CONTENT)

        then:
        thrown(AssertionError)
    }

    //----------------------------------------------------------------------------------------------------


    void "isFileReadableAndNotEmpty, if file exists and has content, then return true"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = SOME_CONTENT

        expect:
        FileService.isFileReadableAndNotEmpty(path)
    }

    void "isFileReadableAndNotEmpty, if file exists but is empty, then return false"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = ''

        expect:
        !FileService.isFileReadableAndNotEmpty(path)
    }

    void "isFileReadableAndNotEmpty, if file exists and has content, but is not readable, then return false"() {
        given:
        Path path = temporaryFolder.newFile().toPath()
        path.text = SOME_CONTENT
        Files.setPosixFilePermissions(path, [] as Set)

        expect:
        !FileService.isFileReadableAndNotEmpty(path)
    }

    void "isFileReadableAndNotEmpty, if file does not exist, then return false"() {
        given:
        Path path = temporaryFolder.newFolder().toPath()
        Path file = path.resolve('file')

        expect:
        !FileService.isFileReadableAndNotEmpty(file)
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

        println Files.isSymbolicLink(file)

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
        println Files.isSymbolicLink(file)

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

}

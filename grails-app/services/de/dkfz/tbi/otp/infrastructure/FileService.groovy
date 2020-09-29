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

import com.github.robtimus.filesystems.sftp.SFTPFileSystemProvider
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.StaticApplicationContextWrapper
import de.dkfz.tbi.otp.utils.ThreadUtils

import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.*
import java.time.Duration
import java.util.stream.Stream

/**
 * Helper methods to work with file paths
 */
@Transactional
class FileService {

    /**
     * Special logger for logging of errors during waiting.
     * Since that can produce much output, it should go to its own logging file. Therefore, a special name is used.
     */
    @SuppressWarnings('LoggerForDifferentClass')
    static final Logger WAIT_LOG = LoggerFactory.getLogger("${FileService.name}.WAITING")

    RemoteShellHelper remoteShellHelper

    /**
     * Time in milliseconds between checks.
     *
     * @see #waitUntilExists(Path)
     * @see #waitUntilDoesNotExist(Path)
     */
    static final int MILLIS_BETWEEN_RETRIES = 50

    /**
     * The default directory permissions (2750) with setgid bit
     */
    static final String DEFAULT_DIRECTORY_PERMISSION_STRING = "2750"

    /**
     * The directory permissions only accessible for owner (2700) with setgid bit
     */
    static final String OWNER_DIRECTORY_PERMISSION_STRING = "2700"

    /**
     * The old default directory permissions (750).
     *
     * Please use now {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}, which has also the setgid bit
     *
     * @Deprecated The directory permission should be 2740, which can not be done via {@link PosixFilePermission}
     */
    @Deprecated
    static final Set<PosixFilePermission> DEFAULT_DIRECTORY_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
    ].toSet().asImmutable()

    /**
     * Owner and Group read/write (770)
     */
    static final Set<PosixFilePermission> OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
    ].toSet().asImmutable()

    /**
     * The directory permissions only accessible for owner (700)
     */
    static final Set<PosixFilePermission> OWNER_DIRECTORY_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
    ].toSet().asImmutable()

    /**
     * The default file permissions (440)
     */
    static final Set<PosixFilePermission> DEFAULT_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.GROUP_READ,
    ].toSet().asImmutable()

    /**
     * User read write group read file permission (640)
     */
    static final Set<PosixFilePermission> OWNER_READ_WRITE_GROUP_READ_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
    ].toSet().asImmutable()

    /**
     * The default file permissions for bam/bai (444).
     *
     * Some tools require read access for others to work.
     *
     * The extension to use is defined in {@link #BAM_FILE_EXTENSIONS}
     */
    static final Set<PosixFilePermission> DEFAULT_BAM_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ,
    ].toSet().asImmutable()

    /**
     * File extension for which {@link #DEFAULT_BAM_FILE_PERMISSION} should be used
     */
    static final Collection<String> BAM_FILE_EXTENSIONS = [
            '.bam',
            '.bam.bai',
    ].asImmutable()

    /**
     * Convert a Path to a File object
     * This method is necessary because the {@link Path#toFile} method is not supported on Paths not backed
     * by the default FileSystemProvider, such as {@link com.github.robtimus.filesystems.sftp.SFTPPath}s.
     */
    @SuppressWarnings(['JavaIoPackageAccess', 'UnnecessaryCollectCall'])
    File toFile(Path path) {
        assert path.absolute
        new File(File.separator + path*.toString().join(File.separator))
    }

    /**
     * Convert a File to a Path object using the given fileSystem
     * This method is necessary because the {@link File#toPath} do not allow to define the file system but use always
     * the default file system.
     */
    Path toPath(File file, FileSystem fileSystem) {
        assert file
        assert file.absolute
        assert fileSystem

        fileSystem.getPath(file.path)
    }

    Path changeFileSystem(Path path, FileSystem fileSystem) {
        toPath(toFile(path), fileSystem)
    }

    @SuppressWarnings('EmptyCatchBlock')
    static boolean isFileReadableAndNotEmpty(final Path file) {
        assert file.absolute
        try {
            waitUntilExists(file)
        } catch (AssertionError e) {
        }
        return Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file) && Files.size(file) > 0L
    }

    /**
     * Finds and returns first available file with the filename matching the given regex
     */
    static Path findFileInPath(final Path dir, final String fileRegex) {
        ensureDirIsReadable(dir)
        Path match = null
        assert ThreadUtils.waitFor({
            Stream<Path> stream = null
            try {
                stream = Files.list(dir)
                match = stream.find { Path path ->
                    path.fileName =~ fileRegex
                } as Path
            } finally {
                stream?.close()
            }
        }, timeout.toMillis(), MILLIS_BETWEEN_RETRIES): "Cannot find any file with the filename matching '${fileRegex}'"
        return match
    }

    /**
     * Finds and returns all available files with filenames matching the given regex
     */
    static List<Path> findAllFilesInPath(final Path dir, final String fileRegex = ".*") {
        ensureDirIsReadable(dir)
        List<Path> matches = []
        assert ThreadUtils.waitFor({
            Stream<Path> stream = null
            try {
                stream = Files.list(dir)
                matches = stream.findAll { Path path ->
                    path.fileName =~ fileRegex
                } as List<Path>
            } finally {
                stream?.close()
            }
        }, timeout.toMillis(), MILLIS_BETWEEN_RETRIES): "Cannot find any files with their filenames matching '${fileRegex}'"
        return matches
    }

    /**
     * Finds first available file using the given regex and ensures match is readable and not empty.
     */
    Path getFoundFileInPathEnsureIsReadableAndNotEmpty(final File workDirectory, final String regex, final FileSystem fileSystem) {
        Path foundFile = findFileInPath(toPath(workDirectory, fileSystem), regex)
        ensureFileIsReadableAndNotEmpty(foundFile)
        return foundFile
    }

    static void ensureFileIsReadableAndNotEmpty(final Path file) {
        ensureFileIsReadable(file)
        assert Files.size(file) > 0L
    }

    static void ensureFileIsReadable(final Path file) {
        assert file.absolute
        waitUntilExists(file)
        assert Files.isRegularFile(file)
        assert Files.isReadable(file)
    }

    static void ensurePathIsReadable(final Path file) {
        assert file.absolute
        waitUntilExists(file)
        assert Files.isReadable(file)
    }

    static boolean isFileReadable(final Path file) {
        try {
            ensureFileIsReadable(file)
        } catch (AssertionError ignored) {
            return false
        }
        return true
    }

    static void ensureDirIsReadableAndNotEmpty(final Path dir) {
        ensureDirIsReadable(dir)
        Stream<Path> stream = null
        try {
            stream = Files.list(dir)
            assert stream.count() != 0
        } finally {
            stream?.close()
        }
    }

    static void ensureDirIsReadable(final Path dir) {
        assert dir.absolute
        waitUntilExists(dir)
        assert Files.isDirectory(dir)
        assert Files.isReadable(dir)
    }

    static void ensureDirIsReadableAndExecutable(final Path dir) {
        ensureDirIsReadable(dir)
        assert Files.isExecutable(dir)
    }

    static String readFileToString(Path path, Charset encoding) throws IOException {
        return new String(Files.readAllBytes(path), encoding)
    }

    /**
     * It looks like exists() is cached (in NFS?). The cache can be cleared by calling canRead() for files and
     * by calling list() (for directories canRead() does not clear the cache in all cases)
     * - at least in the cases that we observed.
     * To make the methods work with both files and directories, both are called.
     */

    /**
     * Waits until the specified file system object exists or the specified timeout elapsed.
     */
    static void waitUntilExists(Path file) {
        waitForPath(file, true)
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    static void waitUntilDoesNotExist(Path file) {
        waitForPath(file, false)
    }

    private static void waitForPath(Path file, boolean shouldExist) {
        long timeoutInMs = timeout.toMillis()
        Stream<Path> stream = null
        assert ThreadUtils.waitFor({
            try {
                if (Files.isDirectory(file)) {
                    stream = Files.list(file)
                } else {
                    Files.isReadable(file)
                }
            } catch (NoSuchFileException | AccessDeniedException logged) {
                WAIT_LOG.debug('Exception during waiting', logged)
            } finally {
                stream?.close()
                stream = null
            }
            Files.exists(file) == shouldExist
        }, timeoutInMs, MILLIS_BETWEEN_RETRIES):
                "${file} on ${file.fileSystem == FileSystems.default ? 'local' : 'remote'} filesystem " +
                        "${shouldExist ? 'is not accessible or does not exist' : 'still exists'}"
    }

    static Duration getTimeout() {
        return (Environment.current == Environment.TEST) ? Duration.ZERO : Duration.ofMinutes(
                StaticApplicationContextWrapper.context.processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.FILESYSTEM_TIMEOUT)
        )
    }

    boolean fileSizeExceeded(File file, long limitInBytes) {
        return file.size() > limitInBytes
    }

    /**
     * Set the permission of the path to the given permission.
     *
     * The path have to be absolute and have to exist,
     *
     * For directories with setgid bit you need to use {@link #setPermissionViaBash(Path, Realm, String)},
     * since that is not possible with {@link PosixFilePermission}
     */
    void setPermission(Path path, Set<PosixFilePermission> permissions) {
        assert path
        assert Files.exists(path)

        Files.setPosixFilePermissions(path, permissions)

        assert Files.getPosixFilePermissions(path) == permissions
    }

    /**
     * Create the requested directory (absolute path) and all missing parent directories. The group and permissions are set via bash.
     *
     * It won't fail if the directory already exist, but then the group and permissions are not changed.
     */
    void createDirectoryRecursivelyAndSetPermissionsViaBash(Path path, Realm realm, String groupString = '',
                                                            String permissions = DEFAULT_DIRECTORY_PERMISSION_STRING) {
        assert path
        assert path.absolute

        createDirectoryRecursivelyAndSetPermissionsViaBashInternal(path, realm, groupString, permissions)
    }

    private void createDirectoryRecursivelyAndSetPermissionsViaBashInternal(Path path, Realm realm, String groupString, String permissions) {
        if (Files.exists(path)) {
            assert Files.isDirectory(path): "The path ${path} already exist, but is not a directory"
        } else {
            createDirectoryRecursivelyAndSetPermissionsViaBashInternal(path.parent, realm, groupString, permissions)

            createDirectoryHandlingParallelCreationOfSameDirectory(path)

            // chgrp needs to be done before chmod, as chgrp resets setgid and setuid
            if (groupString) {
                setGroupViaBash(path, realm, groupString)
            }
            setPermissionViaBash(path, realm, permissions)
        }
    }

    void setDefaultDirectoryPermissionViaBash(Path path, Realm realm) {
        setPermissionViaBash(path, realm, DEFAULT_DIRECTORY_PERMISSION_STRING)
    }

    void setPermissionViaBash(Path path, Realm realm, String permissions) {
        remoteShellHelper.executeCommandReturnProcessOutput(realm, "chmod ${permissions} ${path}").assertExitCodeZeroAndStderrEmpty()
    }

    void setGroupViaBash(Path path, Realm realm, String groupString) {
        remoteShellHelper.executeCommandReturnProcessOutput(realm, "chgrp -h ${groupString} ${path}").assertExitCodeZeroAndStderrEmpty()
    }

    /**
     * Create the requested directory (absolute path) and all missing parent directories with the permission defined in {@link #DEFAULT_DIRECTORY_PERMISSION}.
     *
     * It won't fail if the directory already exist, but then the permissions are not changed.
     *
     * @Deprecated use{@link #createDirectoryRecursivelyAndSetPermissionsViaBash} instead since this method do not set the setgid bit
     */
    @Deprecated
    void createDirectoryRecursively(Path path) {
        assert path
        assert path.absolute

        createDirectoryRecursivelyInternal(path)
    }

    @Deprecated
    private void createDirectoryRecursivelyInternal(Path path) {
        if (Files.exists(path)) {
            assert Files.isDirectory(path): "The path ${path} already exist, but is not a directory"
        } else {
            createDirectoryRecursivelyInternal(path.parent)

            createDirectoryHandlingParallelCreationOfSameDirectory(path)
            setPermission(path, DEFAULT_DIRECTORY_PERMISSION)
        }
    }

    /**
     * Helper to create a directory and handle case, were multiple threads try to create directory parallel.
     */
    private void createDirectoryHandlingParallelCreationOfSameDirectory(Path path) {
        try {
            Files.createDirectory(path)
        } catch (FileSystemException e) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                return //directory was created by another thread running parallel
                //Because SFTP does not use the more specific FileAlreadyExistsException, using of Files.createDirectories(path) does not help,
                //which catches only FileAlreadyExistsException.
            }
            throw e
        }
    }

    /**
     * Delete the requested directory inclusive all entries recursively
     *
     * It won't fail if the directory does not exist.
     */
    void deleteDirectoryRecursively(Path path) {
        assert path
        assert path.absolute

        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file)
                    return FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                    if (exception == null) {
                        Files.delete(directory)
                        return FileVisitResult.CONTINUE
                    }
                    throw exception
                }
            })
        }
    }

    /**
     * Create the requested file with the given content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path, String content, Realm realm, Set<PosixFileAttributes> filePermission = DEFAULT_FILE_PERMISSION) {
        assert path
        assert path.absolute
        assert !Files.exists(path)

        createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent, realm)

        path.text = content
        setPermission(path, filePermission)
    }

    /**
     * Create the requested file with the given byte content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path, byte[] content, Realm realm, Set<PosixFileAttributes> filePermission = DEFAULT_FILE_PERMISSION) {
        assert path
        assert path.absolute
        assert !Files.exists(path)

        createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent, realm)

        path.bytes = content
        setPermission(path, filePermission)
    }

    /**
     * Creates a new, group-writable, group-executable file to hold generated console and/or bash scripts.
     *
     * pre-existing files of the same name will be overwritten without asking.
     */
    Path createOrOverwriteScriptOutputFile(Path outputFolder, String fileName) {
        Path p = outputFolder.resolve(fileName)

        if (Files.exists(p)) {
            Files.delete(p)
        }

        Files.createFile(p, PosixFilePermissions.asFileAttribute(FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION))
        // system default umasks prevent 'dangerous' group permissions like group-writable directly upon file creation,
        // set permissions AGAIN to teach 'umask' a lesson who is boss!
        Files.setPosixFilePermissions(p, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)
        return p
    }

    /**
     * Create a link from linkPath to existingPath.
     *
     * The destination has to exist, the link may only exist if option {@link CreateLinkOption#DELETE_EXISTING_FILE} is given.
     * Both parameters have to be absolute.
     * Missing parent directories are created automatically with the {@link #DEFAULT_DIRECTORY_PERMISSION}.
     *
     * By default a relative link is created, by passing {@link CreateLinkOption#ABSOLUTE} an absolute link is created.
     *
     * If {@link CreateLinkOption#DELETE_EXISTING_FILE} is given and the link exist and is of type symbolic link or regularfile, it will be deleted
     * and the link is recreated.
     *
     * If the existingPath is a directory, linkPath will be a link to that directory,
     * if the existingPath is a regular file, linkPath will a link to that file,
     * it is NOT possible to use this method like {code ln -s /dir1/file.txt /dir2/}.
     *
     * @param linkPath the path of the link
     * @param existingPath the exiting path the link point to
     * @param realm the realm to use for remote access
     * @param groupString the name of the unix group of the associated project
     * @param options Option to adapt the behavior, see {@link CreateLinkOption}
     */
    @SuppressWarnings('Instanceof')
    void createLink(Path linkPath, Path existingPath, Realm realm, String groupString = '', CreateLinkOption... options) {
        assert linkPath
        assert existingPath
        assert linkPath.absolute
        assert existingPath.absolute
        assert Files.exists(existingPath)
        if (options.contains(CreateLinkOption.DELETE_EXISTING_FILE)) {
            assert !Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS) || Files.isRegularFile(linkPath, LinkOption.NOFOLLOW_LINKS) ||
                    Files.isSymbolicLink(linkPath)
        } else {
            assert !Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)
        }
        assert linkPath.fileSystem == existingPath.fileSystem
        //SimpleAbstractPath doesn't take special meaning of "." and ".." into consideration
        assert linkPath.every { it.toString() != ".." && it.toString() != "." }

        if (linkPath == existingPath) {
            return
        }

        if (options.contains(CreateLinkOption.DELETE_EXISTING_FILE) && Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(linkPath)
        }

        Path existing = (options.contains(CreateLinkOption.ABSOLUTE)) ?
                existingPath :
                linkPath.parent.relativize(existingPath)

        createDirectoryRecursivelyAndSetPermissionsViaBash(linkPath.parent, realm, groupString)

        // SFTP does not support creating symbolic links
        if (linkPath.fileSystem.provider() instanceof SFTPFileSystemProvider) {
            // use -T option so behaviour is the same as createSymbolicLink()
            remoteShellHelper.executeCommandReturnProcessOutput(realm, "ln -Ts '${existing}' '${linkPath}'")
        } else {
            Files.createSymbolicLink(linkPath, existing)
        }
    }

    /**
     * Move the file from source to destination.
     *
     * Both have to be absolute, the source have to be exist, the destination may not be exist.
     *
     * Needed parent directories of the destination will be created automatically with {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void moveFile(Path source, Path destination, Realm realm) {
        assert source
        assert destination
        assert source.absolute
        assert destination.absolute

        assert Files.exists(source)
        assert !Files.exists(destination)

        createDirectoryRecursivelyAndSetPermissionsViaBash(destination.parent, realm)
        Files.move(source, destination)
        assert Files.exists(destination)
    }

    /**
     * Correct the group and permission recursive for the directory structure.
     *
     * The permissions are set:
     * - directories are set to: {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}
     * - bam/bai files to: {@link #DEFAULT_BAM_FILE_PERMISSION}
     * - other files to: {@link #DEFAULT_FILE_PERMISSION}
     */
    void correctPathPermissionAndGroupRecursive(Path path, Realm realm, String group) {
        assert path
        assert path.absolute
        assert Files.exists(path)
        assert realm
        assert group

        correctPathAndGroupPermissionRecursiveInternal(path, realm, group)
    }

    @SuppressWarnings('ThrowRuntimeException')
    private void correctPathAndGroupPermissionRecursiveInternal(Path path, Realm realm, String group) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Stream<Path> stream = null
            try {
                stream = Files.list(path)
                stream.each {
                    correctPathAndGroupPermissionRecursiveInternal(it, realm, group)
                }
                setGroupViaBash(path, realm, group)
                setDefaultDirectoryPermissionViaBash(path, realm)
            } finally {
                stream?.close()
            }
        } else if (Files.isSymbolicLink(path)) {
            setGroupViaBash(path, realm, group)
        } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            setGroupViaBash(path, realm, group)
            if (useBamFilePermission(path)) {
                setPermission(path, DEFAULT_BAM_FILE_PERMISSION)
            } else {
                setPermission(path, DEFAULT_FILE_PERMISSION)
            }
        } else {
            throw new RuntimeException("'${path} is neither directory, nor file nor link")
        }
    }

    @SuppressWarnings('UnnecessaryToString')
    private boolean useBamFilePermission(Path path) {
        String fileName = path.toString()
        return BAM_FILE_EXTENSIONS.any {
            fileName.endsWith(it)
        }
    }
}

enum CreateLinkOption {
    /** Create an absolute link instead of a relative link */
    ABSOLUTE,
    /** If a regular file or a link exist, delete it. For directories it will still fail. */
    DELETE_EXISTING_FILE,

    private CreateLinkOption() {
    }
}

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

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.StaticApplicationContextWrapper
import de.dkfz.tbi.otp.utils.ThreadUtils

import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
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

    ConfigService configService

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
     * The directory permissions accessible for owner and group members (2770) with setgid bit
     */
    static final String OWNER_AND_GROUP_DIRECTORY_PERMISSION_STRING = "2770"

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
     * User read write group read write file permission (660)
     */
    static final Set<PosixFilePermission> OWNER_READ_WRITE_GROUP_READ_WRITE_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
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
     * The default header to use for bash scripts.
     *
     * It mark the script as bash scripts and enable verbose and exit on error (including pipefail).
     */
    static final String BASH_HEADER = "#!/bin/bash\n\nset -evo pipefail\n"

    /**
     * Convert a Path to a File object
     * This method is necessary because the {@link Path#toFile} method is not supported on Paths not backed
     * by the default FileSystemProvider, such as {@link com.github.robtimus.filesystems.sftp.SFTPPath}s.
     */
    @SuppressWarnings(['JavaIoPackageAccess', 'UnnecessaryCollectCall'])
    File toFile(Path path) {
        assert path.absolute
        return new File(File.separator + path*.toString().join(File.separator))
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

        return fileSystem.getPath(file.path)
    }

    Path changeFileSystem(Path path, FileSystem fileSystem) {
        return toPath(toFile(path), fileSystem)
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
    Path getFoundFileInPathEnsureIsReadableAndNotEmpty(final Path workDirectory, final String regex) {
        Path foundFile = findFileInPath(workDirectory, regex)
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

    String getPermissionViaBash(Path path, Realm realm, LinkOption... options) {
        return remoteShellHelper.executeCommandReturnProcessOutput(realm, "stat ${LinkOption.NOFOLLOW_LINKS in options ? "" : "-L"} -c %a ${path}")
                .assertExitCodeZeroAndStderrEmpty().stdout.trim()
    }

    void setGroupViaBash(Path path, Realm realm, String groupString) {
        remoteShellHelper.executeCommandReturnProcessOutput(realm, "chgrp -h ${groupString} ${path}").assertExitCodeZeroAndStderrEmpty()
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

    String convertPermissionsToOctalString(Set<PosixFilePermission> permissions) {
        Map<PosixFilePermission, Integer> mapping = [
                (PosixFilePermission.OWNER_READ)    : 400,
                (PosixFilePermission.OWNER_WRITE)   : 200,
                (PosixFilePermission.OWNER_EXECUTE) : 100,
                (PosixFilePermission.GROUP_READ)    : 40,
                (PosixFilePermission.GROUP_WRITE)   : 20,
                (PosixFilePermission.GROUP_EXECUTE) : 10,
                (PosixFilePermission.OTHERS_READ)   : 4,
                (PosixFilePermission.OTHERS_WRITE)  : 2,
                (PosixFilePermission.OTHERS_EXECUTE): 1,
        ]
        return permissions.sum { PosixFilePermission it -> mapping[it] }
    }

    /**
     * Delete the requested directory inclusive all entries recursively
     *
     * It won't fail if the directory does not exist.
     */
    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
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
     * Create the requested file with the given content and permission over the default realm.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContentOnDefaultRealm(Path path, String content, Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION) {
        createFileWithContent(path, content, configService.defaultRealm, filePermission)
    }

    /**
     * Create the requested file with the given content and permission over the given realm.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path, String content, Realm realm, Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION) {
        createFileWithContentCommonPartHelper(path, realm, filePermission) {
            path.text = content
        }
    }

    /**
     * Create the requested file with the given byte content and permission over the default realm.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContentOnDefaultRealm(Path path, byte[] content, Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION) {
        createFileWithContent(path, content, configService.defaultRealm, filePermission)
    }

    /**
     * Create the requested file with the given byte content and permission over the given realm.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path, byte[] content, Realm realm, Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION) {
        createFileWithContentCommonPartHelper(path, realm, filePermission) {
            path.bytes = content
        }
    }

    private void createFileWithContentCommonPartHelper(Path path, Realm realm, Set<PosixFilePermission> filePermission, Closure closure) {
        assert path
        assert path.absolute
        assert !Files.exists(path)

        createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent, realm)

        try {
            closure()
        } catch (IOException e) {
            //IOExceptions are not RuntimeExceptions. Thus, together with Java which requires to declare them properly, they make problems.
            //Otherwise the are replaced by java.lang.reflect.UndeclaredThrowableException without the origin exception
            throw new CreateFileException("Creating of file ${path} failed", e)
        }
        setPermission(path, filePermission)
    }

    /**
     * Creates a new, group-writable, group-executable file to hold generated console and/or bash scripts.
     *
     * pre-existing files of the same name will be overwritten without asking.
     *
     * When outputFolder not exist yet, it is created automatically including missing parent directories with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    //false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    Path createOrOverwriteScriptOutputFile(Path outputFolder, String fileName, Realm realm) {
        Path p = outputFolder.resolve(fileName)

        createDirectoryRecursivelyAndSetPermissionsViaBash(outputFolder, realm)

        if (Files.exists(p)) {
            Files.delete(p)
        }

        //sftp does not support setting permission during creation, so it needs to be done afterwards.
        Files.createFile(p)
        Files.setPosixFilePermissions(p, FileService.OWNER_AND_GROUP_READ_WRITE_EXECUTE_PERMISSION)
        return p
    }

    /**
     * Create a link from link to target.
     *
     * The destination has to exist, the link may only exist if option {@link CreateLinkOption#DELETE_EXISTING_FILE} is given.
     * Both parameters have to be absolute.
     * Missing parent directories are created automatically with the {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     *
     * By default a relative link is created, by passing {@link CreateLinkOption#ABSOLUTE} an absolute link is created.
     *
     * If {@link CreateLinkOption#DELETE_EXISTING_FILE} is given and the link exist and is of type symbolic link or regularfile, it will be deleted
     * and the link is recreated.
     *
     * If the target is a directory, link will be a link to that directory,
     * if the target is a regular file, link will a link to that file,
     * it is NOT possible to use this method like {code ln -s /dir1/file.txt /dir2/}.
     *
     * @param link the path of the link
     * @param target the exiting path the link point to
     * @param realm the realm to use for remote access
     * @param groupString the name of the unix group of the associated project
     * @param options Option to adapt the behavior, see {@link CreateLinkOption}
     */
    //false positives, since rule can not recognize calling class
    @SuppressWarnings(['ExplicitFlushForDeleteRule', 'Instanceof'])
    void createLink(Path link, Path target, Realm realm, String groupString = '', CreateLinkOption... options) {
        assert link
        assert target
        assert link.absolute
        assert target.absolute
        assert Files.exists(target)
        if (options.contains(CreateLinkOption.DELETE_EXISTING_FILE)) {
            assert !Files.exists(link, LinkOption.NOFOLLOW_LINKS) || Files.isRegularFile(link, LinkOption.NOFOLLOW_LINKS) ||
                    Files.isSymbolicLink(link)
        } else {
            assert !Files.exists(link, LinkOption.NOFOLLOW_LINKS)
        }
        assert link.fileSystem == target.fileSystem
        //SimpleAbstractPath doesn't take special meaning of "." and ".." into consideration
        assert link.every { it.toString() != ".." && it.toString() != "." }

        if (link == target) {
            return
        }

        if (options.contains(CreateLinkOption.DELETE_EXISTING_FILE) && Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(link)
        }

        Path targetPath = (options.contains(CreateLinkOption.ABSOLUTE)) ?
                target :
                link.parent.relativize(target)

        createDirectoryRecursivelyAndSetPermissionsViaBash(link.parent, realm, groupString)

        // SFTP does not support creating symbolic links
        if (link.fileSystem.provider() instanceof SFTPFileSystemProvider) {
            // use -T option so behaviour is the same as createSymbolicLink()
            remoteShellHelper.executeCommandReturnProcessOutput(realm, "ln -Ts '${targetPath}' '${link}'")
        } else {
            Files.createSymbolicLink(link, targetPath)
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

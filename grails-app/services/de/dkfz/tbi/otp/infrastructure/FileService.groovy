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

import com.github.robtimus.filesystems.sftp.SFTPFileSystemProvider
import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.job.processing.ProcessingException
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.*

import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration
import java.util.stream.Stream

/**
 * Helper methods to work with file paths
 */
@Slf4j
@CompileDynamic
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
     * The directory permissions allowing all to access (2755) with setgid bit
     */
    static final String DIRECTORY_WITH_OTHER_PERMISSION_STRING = "2755"

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
     * All read/write (777)
     */
    static final Set<PosixFilePermission> ALL_READ_WRITE_EXECUTE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE,
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

    boolean isFileReadableAndNotEmpty(final Path file) {
        assert file.absolute
        try {
            waitUntilExists(file)
        } catch (AssertionError ignored) {
        }
        return Files.exists(file) && Files.isRegularFile(file) && fileIsReadable(file) && Files.size(file) > 0L
    }

    /**
     * Tests at command line level whether a path is readable.
     *
     * @param path Path to test
     * @return True, if the path was tested as readable, otherwise false
     */
    boolean fileIsReadable(Path path) {
        ProcessOutput output
        try {
            output = remoteShellHelper.executeCommandReturnProcessOutput("test -r ${path}")
            return output.exitCode == 0
        } catch (ProcessingException e) {
            log.error("error while trying to read file: ${e.message}")
        }
        return false
    }

    /**
     * Finds and returns first available file with the filename matching the given regex
     */
    Path findFileInPath(final Path dir, final String fileRegex) {
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
    List<Path> findAllFilesInPath(final Path dir, final String fileRegex = ".*") {
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

    /**
     * @deprecated Only used in the old workflow system
     *
     * use non-static version.
     */
    @Deprecated
    static void ensureFileIsReadableAndNotEmptyStatic(final Path file) {
        ensureFileIsReadableStatic(file)
        assert Files.size(file) > 0L
    }

    void ensureFileIsReadableAndNotEmpty(final Path file) {
        ensureFileIsReadable(file)
        assert Files.size(file) > 0L
    }

    /**
     * @deprecated Only used in the old workflow system
     *
     * use non-static version.
     */
    @Deprecated
    static void ensureFileIsReadableStatic(final Path file) {
        assert file.absolute
        waitUntilExists(file)
        assert Files.isRegularFile(file)
        assert Files.isReadable(file) // codenarc-disable NoFilesReadableRule
    }

    void ensureFileIsReadable(final Path file) {
        assert file.absolute
        waitUntilExists(file)
        assert Files.isRegularFile(file)
        assert fileIsReadable(file)
    }

    /**
     * @deprecated Only used in the old workflow system
     *
     * use non-static version.
     */
    @Deprecated
    static void ensureDirIsReadableAndNotEmptyStatic(final Path dir) {
        ensureDirIsReadableStatic(dir)
        Stream<Path> stream = null
        try {
            stream = Files.list(dir)
            assert stream.count() != 0
        } finally {
            stream?.close()
        }
    }

    void ensureDirIsReadableAndNotEmpty(final Path dir) {
        ensureDirIsReadable(dir)
        Stream<Path> stream = null
        try {
            stream = Files.list(dir)
            assert stream.count() != 0
        } finally {
            stream?.close()
        }
    }

    /**
     * @deprecated Only used in the old workflow system
     *
     * use non-static version.
     */
    @Deprecated
    static void ensureDirIsReadableStatic(final Path dir) {
        assert dir.absolute
        waitUntilExists(dir)
        assert Files.isDirectory(dir)
        assert Files.isReadable(dir) // codenarc-disable NoFilesReadableRule
    }

    void ensureDirIsReadable(final Path dir) {
        assert dir.absolute
        waitUntilExists(dir)
        assert Files.isDirectory(dir)
        assert fileIsReadable(dir)
    }

    void ensureDirIsReadableAndExecutable(final Path dir) {
        ensureDirIsReadable(dir)
        assert Files.isExecutable(dir)
    }

    String readFileToString(Path path, Charset encoding) throws IOException {
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

    boolean fileSizeExceeded(Path file, long limitInBytes) {
        return Files.size(file) > limitInBytes
    }

    /**
     * Set the permission of the path to the given permission.
     *
     * The path have to be absolute and have to exist,
     *
     * For directories with setgid bit you need to use {@link #setPermissionViaBash(Path, String)},
     * since that is not possible with {@link PosixFilePermission}
     */
    void setPermission(Path path, Set<PosixFilePermission> permissions) {
        assert path
        assert Files.exists(path)
        try {
            Files.setPosixFilePermissions(path, permissions)
        } catch (IOException | UnsupportedOperationException | ClassCastException | SecurityException e) {
            throw new ChangeFilePermissionException("Failed to change permission to ${permissions} for path ${path}", e)
        }

        assert Files.getPosixFilePermissions(path) == permissions
    }

    /**
     * Create the requested directory (absolute path) and all missing parent directories. The group and permissions are set via bash.
     *
     * It won't fail if the directory already exist, but then the group and permissions are not changed.
     */
    void createDirectoryRecursivelyAndSetPermissionsViaBash(Path path, String groupString = '',
                                                            String permissions = DEFAULT_DIRECTORY_PERMISSION_STRING) {
        assert path
        assert path.absolute

        createDirectoryRecursivelyAndSetPermissionsViaBashInternal(path, groupString, permissions)
    }

    private void createDirectoryRecursivelyAndSetPermissionsViaBashInternal(Path path, String groupString, String permissions) {
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                throw new CreateDirectoryException("The path ${path} already exist, but is not a directory")
            }
        } else {
            createDirectoryRecursivelyAndSetPermissionsViaBashInternal(path.parent, groupString, permissions)

            try {
                createDirectoryHandlingParallelCreationOfSameDirectory(path)
            } catch (IOException e) {
                throw new CreateDirectoryException("Failed to create directory ${path}", e)
            }

            // chgrp needs to be done before chmod, as chgrp resets setgid and setuid
            if (groupString) {
                setGroupViaBash(path, groupString)
            }
            setPermissionViaBash(path, permissions)
        }
    }

    void setDefaultDirectoryPermissionViaBash(Path path) {
        setPermissionViaBash(path, DEFAULT_DIRECTORY_PERMISSION_STRING)
    }

    void setPermissionViaBash(Path path, String permissions) throws ChangeFilePermissionException {
        try {
            remoteShellHelper.executeCommandReturnProcessOutput("chmod ${permissions} ${path}").assertExitCodeZeroAndStderrEmpty()
        } catch (ProcessingException | AssertionError e) {
            throw new ChangeFilePermissionException("Failed to change permission to ${permissions} for path ${path}", e)
        }
    }

    String getPermissionViaBash(Path path, LinkOption... options) {
        return remoteShellHelper.executeCommandReturnProcessOutput("stat ${LinkOption.NOFOLLOW_LINKS in options ? "" : "-L"} -c %a ${path}")
                .assertExitCodeZeroAndStderrEmpty().stdout.trim()
    }

    void setGroupViaBash(Path path, String groupString) throws ChangeFileGroupException {
        try {
            remoteShellHelper.executeCommandReturnProcessOutput("chgrp -h ${groupString} ${path}").assertExitCodeZeroAndStderrEmpty()
        } catch (ProcessingException | AssertionError e) {
            throw new ChangeFileGroupException("Failed to change group to ${groupString} for path ${path}", e)
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
                return // directory was created by another thread running parallel
                // Because SFTP does not use the more specific FileAlreadyExistsException, using of Files.createDirectories(path) does not help,
                // which catches only FileAlreadyExistsException.
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
    // false positives, since rule can not recognize calling class
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
     * Delete the whole content within the given directory. The directory is kept.
     * It writes error message into dev log and silently does nothing if the given
     * path is not a directory
     *
     * @param dir the directory,which content should be deleted
     */
    void deleteDirectoryContent(Path dir) {
        assert dir

        // Log the errors into dev log
        if (!Files.exists(dir)) {
            log.info("The given directory ${dir} doesn't exist")
            return
        }

        if (!Files.isDirectory(dir)) {
            log.info("The given directory ${dir} is not a directory.")
            return
        }

        // Now delete its content
        Files.list(dir).each { Path path ->
            // Files.delete() deletes folder if it is already empty. Use it only to delete files
            Files.isDirectory(path) ? deleteDirectoryRecursively(path) : Files.delete(path)
        }
    }

    /**
     * Create the requested file with the given content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path,
                               String content,
                               Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION,
                               boolean overwrite = false) {
        createFileWithContentCommonPartHelper(path, filePermission, overwrite) {
            path.text = content
        }
    }

    /**
     * Create the requested file with the given byte content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}.
     */
    void createFileWithContent(Path path,
                               byte[] content,
                               Set<PosixFilePermission> filePermission = DEFAULT_FILE_PERMISSION,
                               boolean overwrite = false) {
        createFileWithContentCommonPartHelper(path, filePermission, overwrite) {
            path.bytes = content
        }
    }

    // delete is for file system, not on domain class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    private void createFileWithContentCommonPartHelper(Path path,
                                                       Set<PosixFilePermission> filePermission,
                                                       boolean overwrite,
                                                       Closure closure) {
        assert path
        assert path.absolute
        if (overwrite) {
            if (Files.exists(path)) {
                Files.delete(path)
            }
        } else {
            assert !Files.exists(path)
        }

        createDirectoryRecursivelyAndSetPermissionsViaBash(path.parent)

        try {
            closure()
        } catch (IOException e) {
            // IOExceptions are not RuntimeExceptions. Thus, together with Java which requires to declare them properly, they make problems.
            // Otherwise the are replaced by java.lang.reflect.UndeclaredThrowableException without the origin exception
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
    // false positives, since rule can not recognize calling class
    @SuppressWarnings('ExplicitFlushForDeleteRule')
    Path createOrOverwriteScriptOutputFile(Path outputFolder, String fileName) {
        Path p = outputFolder.resolve(fileName)

        createDirectoryRecursivelyAndSetPermissionsViaBash(outputFolder)

        if (Files.exists(p)) {
            Files.delete(p)
        }

        // sftp does not support setting permission during creation, so it needs to be done afterwards.
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
     * @param groupString the name of the unix group of the associated project
     * @param options Option to adapt the behavior, see {@link CreateLinkOption}
     */
    // false positives, since rule can not recognize calling class
    @SuppressWarnings(['ExplicitFlushForDeleteRule', 'Instanceof'])
    void createLink(Path link, Path target, String groupString = '', CreateLinkOption... options) {
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
        // SimpleAbstractPath doesn't take special meaning of "." and ".." into consideration
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

        createDirectoryRecursivelyAndSetPermissionsViaBash(link.parent, groupString)

        // SFTP does not support creating symbolic links
        if (link.fileSystem.provider() instanceof SFTPFileSystemProvider) {
            // use -T option so behaviour is the same as createSymbolicLink()
            remoteShellHelper.executeCommandReturnProcessOutput("ln -Ts '${targetPath}' '${link}'")
        } else {
            Files.createSymbolicLink(link, targetPath)
        }
    }

    /**
     * Correct the group and permission recursive for the directory structure.
     *
     * The permissions are set:
     * - directories are set to: {@link #DEFAULT_DIRECTORY_PERMISSION_STRING}
     * - bam/bai files to: {@link #DEFAULT_BAM_FILE_PERMISSION}
     * - other files to: {@link #DEFAULT_FILE_PERMISSION}
     */
    void correctPathPermissionAndGroupRecursive(Path path, String group) {
        assert path
        assert path.absolute
        assert Files.exists(path)
        assert group

        correctPathAndGroupPermissionRecursiveInternal(path, group)
    }

    @SuppressWarnings('ThrowRuntimeException')
    private void correctPathAndGroupPermissionRecursiveInternal(Path path, String group) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Stream<Path> stream = null
            try {
                stream = Files.list(path)
                stream.each {
                    correctPathAndGroupPermissionRecursiveInternal(it, group)
                }
                setGroupViaBash(path, group)
                defaultDirectoryPermissionViaBash = path
            } finally {
                stream?.close()
            }
        } else if (Files.isSymbolicLink(path)) {
            setGroupViaBash(path, group)
        } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            setGroupViaBash(path, group)
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

    /**
     * Calculate the size of the path.
     *
     * For files its simple return the file size.
     * For directories its iterates over all children recursively and summarize the size of all found regular files and directories.
     * Links are ignored (including the size of the link itself).
     */
    long calculateSizeRecursive(Path path) {
        assert path
        assert path.absolute
        assert Files.exists(path)

        long size = 0
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Files.size() can not be used, since it always follow symbolic links,
                // but we want count symbolic links only with the size of the link, not the size of the linked file
                size += Files.readAttributes(file, BasicFileAttributes, LinkOption.NOFOLLOW_LINKS).size()
                return FileVisitResult.CONTINUE
            }

            @Override
            FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                size += Files.size(dir)
                return FileVisitResult.CONTINUE
            }
        })
        return size
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

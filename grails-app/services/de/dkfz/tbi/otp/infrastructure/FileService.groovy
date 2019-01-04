package de.dkfz.tbi.otp.infrastructure

import de.dkfz.tbi.otp.utils.*
import grails.util.*

import java.nio.file.*
import java.nio.file.attribute.*
import java.time.*

/**
 * Helper methods to work with file paths
 */
class FileService {

    /**
     * Time in milliseconds between checks.
     *
     * @see #waitUntilExists(Path)
     * @see #waitUntilDoesNotExist(Path)
     */
    static final int MILLIS_BETWEEN_RETRIES = 50

    /**
     * The default directory permissions (750)
     */
    static final Set<PosixFileAttributes> DEFAULT_DIRECTORY_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
    ].toSet().asImmutable()

    /**
     * The directory permissions only accessible for owner (700)
     */
    static final Set<PosixFileAttributes> OWNER_DIRECTORY_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
    ].toSet().asImmutable()

    /**
     * The default file permissions (440)
     */
    static final Set<PosixFileAttributes> DEFAULT_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.GROUP_READ,
    ].toSet().asImmutable()

    /**
     * The default file permissions for bam/bai (444).
     *
     * Some tools require read access for others to work.
     *
     * The extension to use is defind in {@link #BAM_FILE_EXTENSIONS}
     */
    static final Set<PosixFileAttributes> DEFAULT_BAM_FILE_PERMISSION = [
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
    @SuppressWarnings('JavaIoPackageAccess')
    File toFile(Path path) {
        assert path.isAbsolute()
        new File(File.separator + path.collect { Path part ->
            part.toString()
        }.join(File.separator))
    }

    /**
     * Convert a File to a Path object using the given fileSystem
     * This method is necessary because the {@link File#toPath} do not allow to define the file system but use always
     * the default file system.
     */
    Path toPath(File file, FileSystem fileSystem) {
        assert file
        assert file.isAbsolute()
        assert fileSystem

        fileSystem.getPath(file.path)
    }

    @SuppressWarnings('EmptyCatchBlock')
    static boolean isFileReadableAndNotEmpty(final Path file) {
        assert file.isAbsolute()
        try {
            waitUntilExists(file)
        } catch (AssertionError e) {
        }
        return Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file) && Files.size(file) > 0L
    }

    static void ensureFileIsReadableAndNotEmpty(final Path file) {
        assert file.isAbsolute()
        waitUntilExists(file)
        assert Files.isRegularFile(file)
        assert Files.isReadable(file)
        assert Files.size(file) > 0L
    }

    static void ensureDirIsReadableAndNotEmpty(final Path dir) {
        ensureDirIsReadable(dir)
        assert Files.list(dir).count() != 0
    }

    static void ensureDirIsReadable(final Path dir) {
        assert dir.isAbsolute()
        waitUntilExists(dir)
        assert Files.isDirectory(dir)
        assert Files.isReadable(dir)
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
        assert ThreadUtils.waitFor({
            try {
                Files.isDirectory(file) ? Files.list(file) : Files.isReadable(file)
            } catch (NoSuchFileException ignored) {
            } catch (AccessDeniedException ignored) {
            }
            Files.exists(file)
        }, timeout.toMillis(), MILLIS_BETWEEN_RETRIES): "${file} not found."
    }

    /**
     * Waits until the specified file system object does not exist or the specified timeout elapsed.
     */
    static void waitUntilDoesNotExist(Path file) {
        assert ThreadUtils.waitFor({
            try {
                Files.isDirectory(file) ? Files.list(file) : Files.isReadable(file)
            } catch (AccessDeniedException ignored) {
            } catch (NoSuchFileException ignored) {
                return true
            }
            !Files.exists(file)
        }, timeout.toMillis(), MILLIS_BETWEEN_RETRIES): "${file} still exists."
    }

    private static Duration getTimeout() {
        (Environment.current == Environment.TEST) ? Duration.ZERO : Duration.ofMinutes(2)
    }

    /**
     * Set the permission of the path to the given permission.
     *
     * The path have to be absolute and have to exist,
     */
    void setPermission(Path path, Set<PosixFileAttributes> permissions) {
        assert path
        assert Files.exists(path)

        Files.setPosixFilePermissions(path, permissions)

        assert Files.getPosixFilePermissions(path) == permissions
    }

    /**
     * Create the requested directory (absolute path) and all missing parent directories with the permission defined in {@link #DEFAULT_DIRECTORY_PERMISSION}.
     *
     * It won't fail if the directory already exist, but then the permissions are not changed.
     */
    void createDirectoryRecursively(Path path) {
        assert path
        assert path.isAbsolute()

        createDirectoryRecursivelyIntern(path)
    }

    private void createDirectoryRecursivelyIntern(Path path) {
        if (Files.exists(path)) {
            assert Files.isDirectory(path): "The path ${path} already exist, but is not a directory"
        } else {
            createDirectoryRecursivelyIntern(path.parent)

            Files.createDirectory(path)
            setPermission(path, DEFAULT_DIRECTORY_PERMISSION)
        }
    }

    /**
     * Delete the requested directory inclusive all entries recursively
     *
     * It won't fail if the directory does not exist.
     */
    void deleteDirectoryRecursively(Path path) {
        assert path
        assert path.isAbsolute()
        deleteDirectoryRecursivelyInternal(path)
    }

    private void deleteDirectoryRecursivelyInternal(Path path) {
        if (Files.exists(path)) {
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                Files.list(path).each {
                    deleteDirectoryRecursivelyInternal(it)
                }
            }
            Files.delete(path)
        }
    }

    /**
     * Create the requested file with the given content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION}.
     */
    void createFileWithContent(Path path, String content, Set<PosixFileAttributes> filePermission = DEFAULT_FILE_PERMISSION) {
        assert path
        assert path.isAbsolute()
        assert !Files.exists(path)

        createDirectoryRecursively(path.parent)

        path.text = content
        setPermission(path, filePermission)
    }

    /**
     * Create the requested file with the given byte content and permission.
     *
     * The path have to be absolute and may not exist yet. Missing parent directories are created automatically with the
     * {@link #DEFAULT_DIRECTORY_PERMISSION}.
     */
    void createFileWithContent(Path path, byte[] content, Set<PosixFileAttributes> filePermission = DEFAULT_FILE_PERMISSION) {
        assert path
        assert path.isAbsolute()
        assert !Files.exists(path)

        createDirectoryRecursively(path.parent)

        path.bytes = content
        setPermission(path, filePermission)
    }

    /**
     * Create a link from linkPath to existingPath.
     *
     * The destination have to exist, the link may not exist. Both parameter have to be absolute.
     * Missing parent directories are created automatically with the {@link #DEFAULT_DIRECTORY_PERMISSION}.
     *
     * @param linkPath the path of the link
     * @param existingPath the exiting path the link point to
     */
    void createLink(Path linkPath, Path existingPath) {
        assert linkPath
        assert existingPath
        assert linkPath.absolute
        assert existingPath.absolute
        assert Files.exists(existingPath)
        assert !Files.exists(linkPath)

        createLinkIntern(linkPath, existingPath)
    }

    /**
     * Calculate and create a relative link from linkPath to existingPath.
     *
     * The destination have to exist, the link may not exist. Both parameter have to be absolute.
     * Missing parent directories are created automatically with the {@link #DEFAULT_DIRECTORY_PERMISSION}.
     *
     * The relative path is calculated via {@link Path#relativize(Path)}
     *
     * @param linkPath the path of the link
     * @param existingPath the exiting path the link point to
     */
    void createRelativeLink(Path linkPath, Path existingPath) {
        assert linkPath
        assert existingPath
        assert linkPath.absolute
        assert existingPath.absolute
        assert Files.exists(existingPath)
        assert !Files.exists(linkPath)

        createLinkIntern(linkPath, linkPath.parent.relativize(existingPath))
    }

    private void createLinkIntern(Path linkPath, Path existingPath) {
        createDirectoryRecursively(linkPath.parent)

        Files.createSymbolicLink(linkPath, existingPath)
    }

    /**
     * Move the file from source to destination.
     *
     * Both have to be absolute, the source have to be exist, the destination may not be exist.
     *
     * Needed parent directories of the destination will be created automatically
     */
    void moveFile(Path source, Path destination) {
        assert source
        assert destination
        assert source.absolute
        assert destination.absolute

        assert Files.exists(source)
        assert !Files.exists(destination)

        createDirectoryRecursively(destination.parent)
        Files.move(source, destination)
        assert Files.exists(destination)
    }

    /**
     * Correct the permission recursive for the directory structure.
     *
     * The permissions are set:
     * - directories are set to: {@link #DEFAULT_DIRECTORY_PERMISSION}
     * - bam/bai files to: {@link #DEFAULT_BAM_FILE_PERMISSION}
     * - other files to: {@link #DEFAULT_FILE_PERMISSION}
     */
    void correctPathPermissionRecursive(Path path) {
        assert path
        assert path.absolute
        assert Files.exists(path)

        correctPathPermissionRecursiveInternal(path)
    }

    @SuppressWarnings('ThrowRuntimeException')
    private void correctPathPermissionRecursiveInternal(Path path) {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.list(path).each {
                correctPathPermissionRecursiveInternal(it)
            }
            setPermission(path, DEFAULT_DIRECTORY_PERMISSION)
        } else if (Files.isSymbolicLink(path)) {
            //since a link itself has no permission, nothing is to do
            return
        } else if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
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

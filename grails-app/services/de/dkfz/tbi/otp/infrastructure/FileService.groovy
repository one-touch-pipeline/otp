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
     * The default file permissions (440)
     */
    static final Set<PosixFileAttributes> DEFAULT_FILE_PERMISSION = [
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.GROUP_READ,
    ].toSet().asImmutable()

    /**
     * Convert a Path to a File object
     * This method is necessary because the {@link Path#toFile} method is not supported on Paths not backed
     * by the default FileSystemProvider, such as {@link com.github.robtimus.filesystems.sftp.SFTPPath}s.
     */
    File toFile(Path path) {
        assert path.isAbsolute()
        new File(File.separator + path.collect { Path part ->
            part.toString()
        }.join(File.separator))
    }

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
        }, timeout.toMillis(), 50): "${file} not found."
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
        }, timeout.toMillis(), 50): "${file} still exists."
    }

    private static Duration getTimeout() {
        (Environment.current == Environment.TEST) ? Duration.ZERO : Duration.ofMinutes(2)
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
            Files.setPosixFilePermissions(path, DEFAULT_DIRECTORY_PERMISSION)
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
        Files.setPosixFilePermissions(path, filePermission)
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
        Files.setPosixFilePermissions(path, filePermission)
    }
}

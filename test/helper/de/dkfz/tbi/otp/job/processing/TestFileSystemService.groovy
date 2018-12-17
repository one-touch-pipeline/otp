package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.FileSystems

class TestFileSystemService extends FileSystemService {
    @Override
    FileSystem getRemoteFileSystem(Realm realm) throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForProcessingForRealm(Realm realm) throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForConfigFileChecksForRealm(Realm realm) throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForFastqImport() throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForBamImport() throws Throwable {
        return FileSystems.default
    }
}

package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

class TestFileSystemService extends FileSystemService {
    @Override
    FileSystem getFilesystemForRealm(Realm realm) throws Throwable {
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

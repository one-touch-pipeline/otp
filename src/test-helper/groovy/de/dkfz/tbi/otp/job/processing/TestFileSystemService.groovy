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
package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.FileSystems

class TestFileSystemService extends FileSystemService {

    @Override
    FileSystem getRemoteFileSystemOnDefaultRealm() throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getRemoteFileSystem(Realm realm) throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForProcessingForRealm() throws Throwable {
        return FileSystems.default
    }

    @Override
    FileSystem getFilesystemForConfigFileChecksForRealm() throws Throwable {
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

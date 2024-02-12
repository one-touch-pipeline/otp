/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.FileService

import java.nio.file.Files
import java.nio.file.Path

trait RoddyResultServiceTrait<T extends RoddyResult> implements ArtefactFileService<T> {

    static final String RODDY_EXECUTION_STORE_DIR = "roddyExecutionStore"
    static final String RODDY_EXECUTION_DIR_PATTERN = /exec_\d{6}_\d{8,9}_.+_.+/

    Path getWorkExecutionStoreDirectory(T rr) {
        return getDirectoryPath(rr).resolve(RODDY_EXECUTION_STORE_DIR)
    }

    List<Path> getWorkExecutionDirectories(T rr) {
        return rr.roddyExecutionDirectoryNames.collect {
            getWorkExecutionStoreDirectory(rr).resolve(it)
        }
    }

    /**
     * @returns subdirectory of {@link #getWorkExecutionStoreDirectory} corresponding to the latest roddy call
     * Example:
     * exec_150625_102449388_SOMEUSER_WGS
     * exec_yyMMdd_HHmmssSSS_user_analysis
     */
    Path getLatestWorkExecutionDirectory(T rr) {
        assert rr.roddyExecutionDirectoryNames: "No roddyExecutionDirectoryNames have been stored in the database for ${this}."

        String latestDirectoryName = rr.roddyExecutionDirectoryNames.last()
        assert latestDirectoryName == rr.roddyExecutionDirectoryNames.max()
        assert latestDirectoryName ==~ RODDY_EXECUTION_DIR_PATTERN

        Path latestWorkDirectory = getWorkExecutionStoreDirectory(rr).resolve(latestDirectoryName)
        FileService.waitUntilExists(latestWorkDirectory)
        assert Files.isDirectory(latestWorkDirectory)

        return latestWorkDirectory
    }
}

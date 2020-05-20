/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.WaitingFileUtils

/**
 * This interface must be implemented by all result objects which are created with a Roddy workflow.
 * With this interface it is ensured that all information, needed to call Roddy out of OTP, are provided.
 */
@SuppressWarnings('JavaIoPackageAccess')
trait RoddyResult {

    static final String RODDY_EXECUTION_STORE_DIR = "roddyExecutionStore"
    static final String RODDY_EXECUTION_DIR_PATTERN = /exec_\d{6}_\d{8,9}_.+_.+/

    List<String> roddyExecutionDirectoryNames = []

    abstract Project getProject()

    abstract Individual getIndividual()

    abstract SeqType getSeqType()

    abstract Pipeline getPipeline()

    abstract RoddyWorkflowConfig getConfig()

    abstract File getWorkDirectory()

    abstract File getBaseDirectory()

    abstract ReferenceGenome getReferenceGenome()

    File getWorkExecutionStoreDirectory() {
        return new File(workDirectory, RODDY_EXECUTION_STORE_DIR)
    }

    List<File> getWorkExecutionDirectories() {
        roddyExecutionDirectoryNames.collect {
            new File(workExecutionStoreDirectory, it)
        }
    }

    /**
     @returns subdirectory of {@link #getWorkExecutionStoreDirectory} corresponding to the latest roddy call
     */
    // Example:
    // exec_150625_102449388_SOMEUSER_WGS
    // exec_yyMMdd_HHmmssSSS_user_analysis
    File getLatestWorkExecutionDirectory() {
        if (!roddyExecutionDirectoryNames) {
            throw new RuntimeException("No roddyExecutionDirectoryNames have been stored in the database for ${this}.")
        }

        String latestDirectoryName = roddyExecutionDirectoryNames.last()
        assert latestDirectoryName == roddyExecutionDirectoryNames.max()
        assert latestDirectoryName ==~ RODDY_EXECUTION_DIR_PATTERN

        File latestWorkDirectory = new File(workExecutionStoreDirectory, latestDirectoryName)
        WaitingFileUtils.waitUntilExists(latestWorkDirectory)
        assert latestWorkDirectory.directory

        return latestWorkDirectory
    }
}

package de.dkfz.tbi.otp.dataprocessing.roddyExecution

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.WaitingFileUtils

/**
 * This interface must be implemented by all result objects which are created with a Roddy workflow.
 * With this interface it is ensured that all information, needed to call Roddy out of OTP, are provided.
 */

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

    short getProcessingPriority() {
        return project.processingPriority
    }

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
        assert latestWorkDirectory.isDirectory()

        return latestWorkDirectory
    }
}

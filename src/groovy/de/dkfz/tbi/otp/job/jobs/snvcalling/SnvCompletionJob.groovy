package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates.FINISHED

/**
 * Last job of the SNV calling pipeline.
 * <p>
 * This job cleans up the temporary data in the staging directory and sets the processing state of the
 * SnvCallingInstance to FINISHED.
 * </p>
 */
class SnvCompletionJob extends AbstractEndStateAwareJobImpl {

    @Override
    void execute() throws Exception {
        final SnvCallingInstance snvCallingInstance = getProcessParameterObject()

        assert snvCallingInstance

        deleteStagingDirectory snvCallingInstance
        snvCallingInstance.updateProcessingState FINISHED

        succeed()
    }

    private void deleteStagingDirectory(SnvCallingInstance instance) {
        File stagingPath = instance.snvInstancePath.absoluteStagingPath
        File parentPath = stagingPath.parentFile

        // Check if the parent directory contains any objects that are not part of the current instance
        def isDirty = parentPath.listFiles().toList().find { it != stagingPath }

        // TODO: Use proper exceptions when switching to Java 7 or later (OTP-1145)
        if (isDirty) {
            throw new IOException("The parent directory of path '${stagingPath}' for SnvCallingInstance '${instance}' contains unknown files. This should not happen. Please resolve manually.")
        }

        if (!parentPath.deleteDir()) {
            throw new IOException("Unable to delete path '${parentPath}' for SnvCallingInstance '${instance}'")
        }
    }
}

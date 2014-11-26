package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates.FINISHED

/**
 * Last job of the SNV calling pipeline.
 * <p>
 * This job cleans up the temporary data in the staging directory and sets the processing state of the
 * SnvCallingInstance to FINISHED.
 * </p>
 */
class SnvCompletionJob extends AbstractEndStateAwareJobImpl {
    @Autowired
    ExecutionService executionService
    @Autowired
    ConfigService configService
    @Autowired
    CreateClusterScriptService scriptService
    @Autowired
    LsdfFilesService filesService

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

        Realm realm = configService.getRealmDataProcessing(instance.project)
        filesService.deleteDirectoryRecursive(realm, parentPath)
    }
}

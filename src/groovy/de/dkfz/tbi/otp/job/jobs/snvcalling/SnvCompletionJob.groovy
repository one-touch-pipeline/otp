package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.ast.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import static de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates.*

/**
 * Last job of the SNV calling pipeline.
 * <p>
 * This job cleans up the temporary data in the staging directory and sets the processing state of the
 * SnvCallingInstance to FINISHED.
 * </p>
 */
@Component
@Scope("prototype")
@UseJobLog
class SnvCompletionJob extends AbstractEndStateAwareJobImpl implements AutoRestartableJob {
    @Autowired
    ExecutionService executionService
    @Autowired
    ConfigService configService
    @Autowired
    CreateClusterScriptService scriptService
    @Autowired
    LsdfFilesService filesService
    @Autowired
    LinkFileUtils linkFileUtils

    @Override
    void execute() throws Exception {
        final SnvCallingInstance snvCallingInstance = getProcessParameterObject()
        assert snvCallingInstance.processingState == IN_PROGRESS

        deleteStagingDirectory snvCallingInstance
        snvCallingInstance.updateProcessingState FINISHED

        succeed()
    }

    protected void deleteStagingDirectory(SnvCallingInstance instance) {
        File stagingPath = instance.instancePath.absoluteStagingPath
        File parentPath = stagingPath.parentFile

        Realm realm = configService.getRealmDataProcessing(instance.project)
        filesService.deleteDirectoryRecursive(realm, parentPath)
    }
}

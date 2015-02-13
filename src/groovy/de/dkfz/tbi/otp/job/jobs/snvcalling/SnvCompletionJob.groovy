package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingStep
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvConfig
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.utils.LinkFileUtils
import groovy.io.FileType
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance
import de.dkfz.tbi.otp.job.processing.AbstractEndStateAwareJobImpl
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import static org.springframework.util.Assert.*

import static de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvProcessingStates.IN_PROGRESS
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
    @Autowired
    LinkFileUtils linkFileUtils

    @Override
    void execute() throws Exception {
        final SnvCallingInstance snvCallingInstance = getProcessParameterObject()

        assert snvCallingInstance.processingState == IN_PROGRESS

        /**
         * link the result files of the snvCallingInstance and the config files to the sample pair folder
         */
        linkResultFiles(snvCallingInstance)
        linkConfigFiles(snvCallingInstance)

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

    /**
     * This is a helper method which links all files in the SNV instance folder to the sample pair folder.
     * Links which exist already are overwritten.
     */
    protected void linkResultFiles(SnvCallingInstance instance) {
        notNull(instance, "The input instance must not be null")

        File directory = instance.snvInstancePath.absoluteDataManagementPath
        Map<File, File> sourceLinkMap = [:]
        File configFile = instance.configFilePath.absoluteDataManagementPath
        directory.eachFileRecurse (FileType.FILES) { File file ->
            if (file.getName() != configFile.getName()) {
                File linkToFile = new OtpPath(instance.samplePair.samplePairPath, file.getName()).absoluteDataManagementPath
                sourceLinkMap.put(file, linkToFile)
            }
        }
        linkFileUtils.createAndValidateLinks(sourceLinkMap, configService.getRealmDataProcessing(instance.project))
    }

    /**
     * This is a helper method which creates one link of the config file in the instance folder to the sample pair folder
     * for each job which was processed in this instance.
     */
    protected void linkConfigFiles(SnvCallingInstance instance) {
        notNull(instance, "The input instance must not be null")

        File configFile = instance.configFilePath.absoluteDataManagementPath
        SnvConfig config = instance.config.evaluate()
        SnvCallingStep.values().each {
            if (config.getExecuteStepFlag(it)) {
                File linkToFile = instance.getStepConfigFileLinkedPath(it).absoluteDataManagementPath
                linkFileUtils.createAndValidateLinks([(configFile): linkToFile], configService.getRealmDataProcessing(instance.project))
            }
        }
    }
}

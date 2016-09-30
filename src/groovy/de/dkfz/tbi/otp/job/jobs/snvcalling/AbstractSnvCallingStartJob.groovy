package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*
import org.springframework.scheduling.annotation.*

abstract class AbstractSnvCallingStartJob extends AbstractStartJobImpl implements RestartableStartJob {

    @Autowired
    SnvCallingService snvCallingService

    @Autowired
    ConfigService configService

    @Autowired
    ExecutionService executionService

    @Scheduled(fixedDelay = 60000l)
    void execute() {

        short minPriority = minimumProcessingPriorityForOccupyingASlot
        if (minPriority > ProcessingPriority.MAXIMUM_PRIORITY) {
            return
        }
        SnvConfig.withTransaction {
            SamplePair samplePair = snvCallingService.samplePairForSnvProcessing(minPriority, getConfigClass())
            if (samplePair) {

                ConfigPerProject config = getConfig(samplePair)

                AbstractMergedBamFile sampleType1BamFile = samplePair.mergingWorkPackage1.processableBamFileInProjectFolder
                AbstractMergedBamFile sampleType2BamFile = samplePair.mergingWorkPackage2.processableBamFileInProjectFolder

                SnvCallingInstance snvCallingInstance = getInstanceClass().newInstance(
                        samplePair: samplePair,
                        instanceName: getInstanceName(config),
                        config: config,
                        sampleType1BamFile: sampleType1BamFile,
                        sampleType2BamFile: sampleType2BamFile,
                        latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(sampleType1BamFile, sampleType2BamFile),
                )
                snvCallingInstance.save(flush: true)
                trackingService.setStartedForSeqTracks(snvCallingInstance.getContainedSeqTracks(), OtrsTicket.ProcessingStep.SNV)
                samplePair.processingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
                samplePair.save()
                createProcess(snvCallingInstance)
                log.debug "SnvCallingStartJob started for: ${snvCallingInstance.toString()}"
            }
        }
    }


    @Override
    Process restart(Process process) {
        assert process

        ProcessParameter parameter = CollectionUtils.exactlyOneElement(ProcessParameter.findAllByProcess(process))

        SnvCallingInstance failedInstance = SnvCallingInstance.get(parameter.value)

        tryToDeleteResultsFilesOfFailedInstance(failedInstance)

        SnvCallingInstance.withTransaction {

            SnvCallingInstance newInstance = getInstanceClass().newInstance(
                    samplePair: failedInstance.samplePair,
                    instanceName: getInstanceName(failedInstance.config),
                    config: failedInstance.config,
                    sampleType1BamFile: failedInstance.sampleType1BamFile,
                    sampleType2BamFile: failedInstance.sampleType2BamFile,
                    latestDataFileCreationDate: failedInstance.latestDataFileCreationDate,
            )
            assert newInstance.save(flush: true)
            return createProcess(newInstance)
        }
    }


    private void tryToDeleteResultsFilesOfFailedInstance(SnvCallingInstance instance) {
        final Realm realm = configService.getRealmDataManagement(instance.project)
        String deleteFiles = "rm -rf ${instance.snvInstancePath.absoluteDataManagementPath} ${instance.snvInstancePath.absoluteStagingPath}"

        executionService.executeCommandReturnProcessOutput(realm, deleteFiles)
    }


    protected abstract ConfigPerProject getConfig(SamplePair samplePair)
    protected abstract String getInstanceName(ConfigPerProject config)
    protected abstract Class<? extends ConfigPerProject> getConfigClass()

    protected abstract Class<? extends SnvCallingInstance> getInstanceClass()
}

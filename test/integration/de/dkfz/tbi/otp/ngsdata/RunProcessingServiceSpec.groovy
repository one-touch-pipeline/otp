package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import spock.lang.Specification

class RunProcessingServiceSpec extends Specification {

    RunProcessingService runProcessingService = new RunProcessingService()


    Run createData(Map properties) {
        Project project = DomainFactory.createProject([
                processingPriority:  properties.processingPriority ?: ProcessingPriority.NORMAL_PRIORITY
        ])
        Run run = DomainFactory.createRun()
        RunSegment runSegment1 = DomainFactory.createRunSegment([
                run: run,
                filesStatus: properties.filesStatus ?: RunSegment.FilesStatus.NEEDS_INSTALLATION,
                metaDataStatus: properties.metaDataStatus ?: RunSegment.Status.COMPLETE,
        ])
        DataFile dataFile1 = DomainFactory.createDataFile([
                run: run,
                runSegment: runSegment1,
                project: project,
        ])
        RunSegment runSegment2 = DomainFactory.createRunSegment([
                run: run,
                filesStatus: properties.filesStatusSecondRunSegment ?: RunSegment.FilesStatus.FILES_CORRECT,
                metaDataStatus: RunSegment.Status.COMPLETE,
        ])
        DataFile dataFile2 = DomainFactory.createDataFile([
                run: run,
                runSegment: runSegment2,
        ])
        return run
    }


    void test_runReadyToInstall_Fine_ShouldFindRun() {
        when:
        Run run = createData([
                filesStatusSecondRunSegment: filesStatusSecondRunSegment,
                processingPriority: ProcessingPriority.FAST_TRACK_PRIORITY
        ])

        then:
        run == runProcessingService.runReadyToInstall(minPriority)

        where:
        minPriority | filesStatusSecondRunSegment
        ProcessingPriority.NORMAL_PRIORITY | RunSegment.FilesStatus.FILES_CORRECT
        ProcessingPriority.NORMAL_PRIORITY | RunSegment.FilesStatus.NEEDS_INSTALLATION
        ProcessingPriority.NORMAL_PRIORITY | RunSegment.FilesStatus.PROCESSING_UNPACK
        ProcessingPriority.NORMAL_PRIORITY | RunSegment.FilesStatus.NEEDS_UNPACK
        ProcessingPriority.FAST_TRACK_PRIORITY | RunSegment.FilesStatus.FILES_CORRECT
    }

    void test_runReadyToInstall_Fine_ShouldNotFindRun() {
        when:
        Run run = createData([
                filesStatus: filesStatus,
                metaDataStatus: metaDataStatus,
                filesStatusSecondRunSegment: filesStatusSecondRunSegment,
        ])

        then:
        null == runProcessingService.runReadyToInstall(minPriority)

        where:
        filesStatus                                    | metaDataStatus               | minPriority                            | filesStatusSecondRunSegment
        //wrong file status
        RunSegment.FilesStatus.FILES_CORRECT           | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.FILES_MISSING           | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.NEEDS_CHECKING          | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.NEEDS_UNPACK            | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.PROCESSING_CHECKING     | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.PROCESSING_INSTALLATION | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.PROCESSING_UNPACK       | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT

        //wrong run segment status
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.BLOCKED    | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.NEW        | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.PROCESSING | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_CORRECT

        //wrong priority
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.COMPLETE   | ProcessingPriority.FAST_TRACK_PRIORITY | RunSegment.FilesStatus.FILES_CORRECT

        //other run segment of the run is in progress
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.PROCESSING_INSTALLATION
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.NEEDS_CHECKING
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.PROCESSING_CHECKING
        RunSegment.FilesStatus.NEEDS_INSTALLATION      | RunSegment.Status.COMPLETE   | ProcessingPriority.NORMAL_PRIORITY     | RunSegment.FilesStatus.FILES_MISSING
    }

    void test_runReadyToInstall_ShouldFindCorrectRun() {
        when:
        Run run1 = createData([
                processingPriority: processingPriority1,
        ])
        Run run2 = createData([
                processingPriority: processingPriority2,
        ])

        Run run = runNumber == 1 ? run1 : run2

        then:
        run == runProcessingService.runReadyToInstall(ProcessingPriority.NORMAL_PRIORITY)

        where:
        processingPriority1 | processingPriority2 || runNumber
        0 | 0  || 1
        0 | 1  || 2
        1 | 0  || 1
    }
}

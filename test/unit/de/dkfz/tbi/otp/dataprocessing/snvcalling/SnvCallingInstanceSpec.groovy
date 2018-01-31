package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        DataFile,
        ExternalScript,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProcessedBamFile,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SequencingKitLabel,
        SeqTrack,
        SeqType,
        SnvCallingInstance,
        SnvConfig,
        SnvJobResult,
        SoftwareTool,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class SnvCallingInstanceSpec extends Specification {


    void "withdraw, change also jobs to withdrawn"() {
        given:
        SnvJobResult snvJobResult = DomainFactory.createSnvJobResultWithRoddyBamFiles()
        SnvCallingInstance snvCallingInstance = snvJobResult.snvCallingInstance


        when:
        LogThreadLocal.withThreadLog(System.out) {
            snvCallingInstance.withdraw()
        }

        then:
        snvCallingInstance.withdrawn
        snvJobResult.withdrawn
    }

}

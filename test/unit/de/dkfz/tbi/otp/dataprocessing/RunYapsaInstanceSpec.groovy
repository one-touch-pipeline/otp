package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AlignmentPass,
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingPass,
        MergingSet,
        MergingSetAssignment,
        MergingWorkPackage,
        Pipeline,
        ProcessedBamFile,
        ProcessedMergedBamFile,
        ProcessingOption,
        ProcessingThresholds,
        Project,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
        RoddySnvCallingInstance,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
        RunYapsaConfig,
        RunYapsaInstance,
        Sample,
        SamplePair,
        SampleType,
        SampleTypePerProject,
        SeqCenter,
        SeqPlatform,
        SeqPlatformGroup,
        SeqPlatformModelLabel,
        SeqTrack,
        SeqType,
        SequencingKitLabel,
        SoftwareTool,
])
class RunYapsaInstanceSpec extends Specification {

    SamplePair samplePair
    String samplePairPath

    void setup() {
        samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()

        samplePairPath = "${samplePair.sampleType1.name}_${samplePair.sampleType2.name}"


        SamplePair.metaClass.getRunYapsaSamplePairPath = {
            return new OtpPath(samplePair.project, samplePairPath)
        }
    }

    void cleanup() {
        SamplePair.metaClass = null
    }

    void "test getInstance"() {
        given:
        BamFilePairAnalysis instance = createBamFilePairAnalysis()

        when:
        OtpPath path = instance.getInstancePath()

        then:
        instance.project == path.project
        new File(getInstancePathHelper(instance)) == path.relativePath
    }

    void "test withdraw"() {
        given:
        BamFilePairAnalysis instance = createBamFilePairAnalysis()

        when:
        LogThreadLocal.withThreadLog(System.out) {
            instance.withdraw()
        }

        then:
        instance.withdrawn
    }

    private String getInstancePathHelper(BamFilePairAnalysis instance) {
        return "${samplePairPath}/${instance.instanceName}"
    }

    BamFilePairAnalysis createBamFilePairAnalysis() {
        return DomainFactory.createRunYapsaInstanceWithRoddyBamFiles([
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair        : samplePair,
        ])
    }
}

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.mixin.*
import spock.lang.*

@Mock([
        AlignmentPass,
        AceseqInstance,
        DataFile,
        ExternalMergingWorkPackage,
        ExternallyProcessedMergedBamFile,
        FileType,
        Individual,
        IndelCallingInstance,
        LibraryPreparationKit,
        MergingCriteria,
        MergingPass,
        MergingSet,
        MergingSetAssignment,
        MergingWorkPackage,
        Pipeline,
        ProcessedBamFile,
        ProcessedMergedBamFile,
        Project,
        ProjectCategory,
        Realm,
        ReferenceGenome,
        ReferenceGenomeProjectSeqType,
        RoddyBamFile,
        RoddySnvCallingInstance,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
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
        SoftwareTool,
        SophiaInstance,
])
class BamFilePairAnalysisSpec extends Specification {

    void "test constraints when everything is fine, should be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        expect:
        assert bamFilePairAnalysis.validate()
    }

    @Unroll
    void "test constraints when #property is null, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysis[property] = null

        then:
        !bamFilePairAnalysis.validate()

        where:
        property << [
                "config",
                "samplePair",
                "instanceName",
                "sampleType1BamFile",
                "sampleType2BamFile",
        ]
    }


    void "test the constraints when instanceName is blank, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysis.instanceName = ''

        then:
        !bamFilePairAnalysis.validate()
    }


    void "test the constraints when instanceName is not unique, should not be valid"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()
        MockBamFilePairAnalysis bamFilePairAnalysisSameName = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysisSameName.instanceName = bamFilePairAnalysis.instanceName
        bamFilePairAnalysisSameName.samplePair = bamFilePairAnalysis.samplePair

        then:
        !bamFilePairAnalysisSameName.validate()
    }

    @Unroll
    void "withdraw, the flag withdraw should be true"() {
        given:
        BamFilePairAnalysis analysis = createAnalysisClosure()


        when:
        LogThreadLocal.withThreadLog(System.out) {
            analysis.withdraw()
        }

        then:
        analysis.withdrawn

        where:
        createAnalysisClosure << [
                {
                    DomainFactory.createRoddySnvInstanceWithRoddyBamFiles()
                },
                {
                    DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
                },
                {
                    DomainFactory.createSophiaInstanceWithRoddyBamFiles()
                },
                {
                    DomainFactory.createAceseqInstanceWithRoddyBamFiles()
                },
        ]
    }


    @Unroll
    void "check different bamFiles classes: #classBamFile1, #classBamFile2"() {
        given:
        AbstractMergedBamFile bamFile1 = createBamFile(classBamFile1)
        AbstractMergedBamFile bamFile2 = createBamFile(classBamFile2, [
                seqType: bamFile1.seqType,
                sample : DomainFactory.createSample([individual: bamFile1.individual]),
        ])

        DomainFactory.createSampleTypePerProjectForMergingWorkPackage(bamFile1.mergingWorkPackage)


        expect:
        DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(
                sampleType1BamFile: bamFile1,
                sampleType2BamFile: bamFile2,
        )


        where:
        classBamFile1                    | classBamFile2
        RoddyBamFile                     | RoddyBamFile
        ProcessedMergedBamFile           | ProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | ExternallyProcessedMergedBamFile
        RoddyBamFile                     | ExternallyProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | RoddyBamFile
        ProcessedMergedBamFile           | ExternallyProcessedMergedBamFile
        ExternallyProcessedMergedBamFile | ProcessedMergedBamFile
        RoddyBamFile                     | ProcessedMergedBamFile
        ProcessedMergedBamFile           | RoddyBamFile
    }

    private <E> AbstractMergedBamFile createBamFile(Class<E> clazz, Map propertiesForMergingWorkPackage = [:]) {
        Map properties = [
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                md5sum             : DomainFactory.DEFAULT_MD5_SUM,
                fileSize           : ++DomainFactory.counter,
        ]
        switch (clazz) {
            case RoddyBamFile:
                return DomainFactory.createRoddyBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createPanCanPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            case ProcessedMergedBamFile:
                return DomainFactory.createProcessedMergedBamFile([
                        workPackage: DomainFactory.createMergingWorkPackage([
                                pipeline: DomainFactory.createDefaultOtpPipeline(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            case ExternallyProcessedMergedBamFile:
                return DomainFactory.createExternallyProcessedMergedBamFile([
                        workPackage: DomainFactory.createExternalMergingWorkPackage([
                                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                                seqType : DomainFactory.createWholeGenomeSeqType(),
                        ] + propertiesForMergingWorkPackage)
                ] + properties)
            default:
                assert false
        }
    }


    static MockBamFilePairAnalysis createMockBamFilePairAnalysis() {
        Pipeline alignmentPipeline = DomainFactory.createPanCanPipeline()
        Pipeline snvPipeline = DomainFactory.createRoddySnvPipelineLazy()

        MergingWorkPackage controlWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: alignmentPipeline, statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)
        SamplePair samplePair = DomainFactory.createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = DomainFactory.createRoddyBamFile([workPackage: diseaseWorkPackage])
        RoddyBamFile control = DomainFactory.createRoddyBamFile([workPackage: controlWorkPackage, config: disease.config])

        return new MockBamFilePairAnalysis([
                instanceName      : "2014-08-25_15h32",
                samplePair        : samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                config            : DomainFactory.createRoddyWorkflowConfig([seqType: samplePair.seqType, pipeline: snvPipeline]),
        ])
    }


}


class MockBamFilePairAnalysis extends BamFilePairAnalysis {
    @Override
    OtpPath getInstancePath() {
        return new OtpPath("somePath")
    }
}

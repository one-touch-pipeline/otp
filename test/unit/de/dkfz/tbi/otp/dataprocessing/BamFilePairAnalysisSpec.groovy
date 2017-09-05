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
        AceseqInstance,
        DataFile,
        ExternalScript,
        FileType,
        Individual,
        IndelCallingInstance,
        LibraryPreparationKit,
        MergingSet,
        MergingWorkPackage,
        Pipeline,
        Project,
        ProjectCategory,
        ReferenceGenome,
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
        SeqTrack,
        SeqType,
        SoftwareTool,
        SophiaInstance,
        SnvJobResult,
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
        property << ["config", "samplePair", "latestDataFileCreationDate", "instanceName"]
    }


    @Unroll
    void "test constraints when one of the bam files is null, should fail"() {
        given:
        MockBamFilePairAnalysis bamFilePairAnalysis = createMockBamFilePairAnalysis()

        when:
        bamFilePairAnalysis[property] = null
        bamFilePairAnalysis.validate()

        then:
        IllegalArgumentException e = thrown()
        e.message.contains("At least one of the specified BAM files is null")

        where:
        property << ["sampleType1BamFile", "sampleType2BamFile"]
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


    static MockBamFilePairAnalysis createMockBamFilePairAnalysis() {
        Pipeline alignmentPipeline = DomainFactory.createPanCanPipeline()
        Pipeline snvPipeline = DomainFactory.createRoddySnvPipelineLazy()

        MergingWorkPackage controlWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: alignmentPipeline, statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME)
        SamplePair samplePair = DomainFactory.createDisease(controlWorkPackage)
        MergingWorkPackage diseaseWorkPackage = samplePair.mergingWorkPackage1

        RoddyBamFile disease = DomainFactory.createRoddyBamFile([workPackage: diseaseWorkPackage])
        RoddyBamFile control = DomainFactory.createRoddyBamFile([workPackage: controlWorkPackage, config: disease.config])

        return new MockBamFilePairAnalysis([
                instanceName: "2014-08-25_15h32",
                samplePair: samplePair,
                sampleType1BamFile: disease,
                sampleType2BamFile: control,
                config: DomainFactory.createRoddyWorkflowConfig([seqType: samplePair.seqType, pipeline: snvPipeline]),
                latestDataFileCreationDate: AbstractBamFile.getLatestSequenceDataFileCreationDate(disease, control),
        ])
    }

}


class MockBamFilePairAnalysis extends BamFilePairAnalysis {
    @Override
    OtpPath getInstancePath() {
        return new OtpPath("somePath")
    }
}

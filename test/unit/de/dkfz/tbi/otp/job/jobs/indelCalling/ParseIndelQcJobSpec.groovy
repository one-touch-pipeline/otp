package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        AbstractMergedBamFile,
        DataFile,
        FileType,
        IndelCallingInstance,
        IndelSampleSwapDetection,
        IndelQualityControl,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        ProcessingOption,
        ProcessingStep,
        Project,
        ProjectCategory,
        QcThreshold,
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
        SoftwareTool,
        ReferenceGenome,
        ReferenceGenomeEntry,
        ReferenceGenomeProjectSeqType,
        Realm,
        RoddyBamFile,
        RoddyWorkflowConfig,
        Run,
        RunSegment,
])
class ParseIndelQcJobSpec extends Specification {

    TestConfigService configService

    @Rule
    TemporaryFolder temporaryFolder

    IndelCallingInstance indelCallingInstance
    ParseIndelQcJob job

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        job = [
                getProcessParameterObject: { -> indelCallingInstance },
        ] as ParseIndelQcJob
        job.qcTrafficLightService = new QcTrafficLightService()
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test execute method, throw error since #notAvailable does not exist"() {
        given:
        if (available == "sampleSwapJsonFile") {
            DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)
        } else {
            DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        }

        when:
        job.execute()

        then:
        AssertionError e = thrown()
        e.message.contains("${indelCallingInstance."${notAvailable}"} not found")

        where:
        available            | notAvailable
        "sampleSwapJsonFile" | "indelQcJsonFile"
        "indelQcJsonFile"    | "sampleSwapJsonFile"
    }

    void "test execute method when both files available"() {
        given:
        DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)

        when:
        job.execute()

        then:
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list()).indelCallingInstance == indelCallingInstance
        CollectionUtils.exactlyOneElement(IndelQualityControl.list()).indelCallingInstance == indelCallingInstance
        indelCallingInstance.processingState == AnalysisProcessingStates.FINISHED
        indelCallingInstance.sampleType1BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        indelCallingInstance.sampleType2BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
    }


}

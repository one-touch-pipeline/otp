package de.dkfz.tbi.otp.job.jobs.indelCalling

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
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

    void setup() {
        configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test execute method, throw error since #notAvailable does not exist"() {
        given:
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement([name: indelCallingInstance.project.realmName])

        if (available == "sampleSwapJsonFile") {
            DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)
        } else {
            DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        }
        ParseIndelQcJob job = [
                getProcessParameterObject: { -> indelCallingInstance },
        ] as ParseIndelQcJob

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
        IndelCallingInstance indelCallingInstance = DomainFactory.createIndelCallingInstanceWithRoddyBamFiles()
        DomainFactory.createRealmDataManagement([name: indelCallingInstance.project.realmName])

        DomainFactory.createIndelQcFileOnFileSystem(indelCallingInstance.indelQcJsonFile)
        DomainFactory.createIndelSampleSwapDetectionFileOnFileSystem(indelCallingInstance.sampleSwapJsonFile, indelCallingInstance.individual)
        ParseIndelQcJob job = [
                getProcessParameterObject: { -> indelCallingInstance },
        ] as ParseIndelQcJob

        when:
        job.execute()

        then:
        CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list()).indelCallingInstance == indelCallingInstance
        CollectionUtils.exactlyOneElement(IndelQualityControl.list()).indelCallingInstance == indelCallingInstance
        indelCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }


}

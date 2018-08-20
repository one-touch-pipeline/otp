package de.dkfz.tbi.otp.job.jobs.sophia

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*
import grails.test.mixin.*
import org.junit.*
import org.junit.rules.*
import spock.lang.*

@Mock([
        DataFile,
        FileType,
        Individual,
        LibraryPreparationKit,
        MergingCriteria,
        MergingWorkPackage,
        Pipeline,
        Project,
        QcThreshold,
        Realm,
        ReferenceGenome,
        RoddyBamFile,
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
        SophiaQc
])
class ParseSophiaQcJobSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void "test execute"() {
        given:
        File temporaryFile = temporaryFolder.newFolder()
        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFile.path])

        SophiaInstance instance = DomainFactory.createSophiaInstanceWithRoddyBamFiles()

        DomainFactory.createSophiaQcFileOnFileSystem(instance.getQcJsonFile())

        ParseSophiaQcJob job = [
                getProcessParameterObject: { -> instance },
        ] as ParseSophiaQcJob
        job.qcTrafficLightService = new QcTrafficLightService()

        when:
        job.execute()

        then:
        SophiaQc.findAllBySophiaInstance(instance).size() == 1
        SophiaQc qc = SophiaQc.findAllBySophiaInstance(instance).first()
        qc.controlMassiveInvPrefilteringLevel == 0
        qc.tumorMassiveInvFilteringLevel == 0
        qc.rnaContaminatedGenesMoreThanTwoIntron == "PRKRA;ACTG2;TYRO3;COL18A1;"
        qc.rnaContaminatedGenesCount == 4
        qc.rnaDecontaminationApplied == false

        instance.processingState == AnalysisProcessingStates.FINISHED
        instance.sampleType1BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        instance.sampleType2BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED

        cleanup:
        configService.clean()
    }
}

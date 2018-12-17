package de.dkfz.tbi.otp.job.jobs.aceseq

import grails.test.mixin.Mock
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightService
import de.dkfz.tbi.otp.utils.CreateFileHelper

@Mock([
        AbstractMergedBamFile,
        AceseqInstance,
        AceseqQc,
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
])
class ParseAceseqQcJobSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    void "test execute"() {
        given:
        File temporaryFile = temporaryFolder.newFolder()
        File aceseqOutputFile = new File(temporaryFile, "aceseqOutputFile.txt")
        CreateFileHelper.createFile(aceseqOutputFile)

        AceseqInstance instance = DomainFactory.createAceseqInstanceWithRoddyBamFiles()

        instance.metaClass.getAllFiles = {
            return [aceseqOutputFile]

        }

        TestConfigService configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFile.path])

        DomainFactory.createAceseqQaFileOnFileSystem(instance.getQcJsonFile())

        ParseAceseqQcJob job = [
                getProcessParameterObject: { -> instance },
        ] as ParseAceseqQcJob
        job.qcTrafficLightService = new QcTrafficLightService()

        when:
        job.execute()

        then:
        AceseqQc.findAllByAceseqInstance(instance).size() == 2
        TestCase.containSame(AceseqQc.findAllByAceseqInstance(instance)*.number, [1, 2])
        def qc1 = AceseqQc.findByAceseqInstanceAndNumber(instance, 1)
        qc1.tcc == 0.5d
        qc1.ploidyFactor == "2.27"
        qc1.ploidy == 2
        qc1.goodnessOfFit == 0.904231625835189d
        qc1.gender == "male"
        qc1.solutionPossible == 3
        def qc2 = AceseqQc.findByAceseqInstanceAndNumber(instance, 2)
        qc2.tcc == 0.7d
        qc2.ploidyFactor == "1.27"
        qc2.ploidy == 5
        qc2.goodnessOfFit == 0.12345d
        qc2.gender == "female"
        qc2.solutionPossible == 4

        instance.processingState == AnalysisProcessingStates.FINISHED
        instance.sampleType1BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED
        instance.sampleType2BamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED

        cleanup:
        configService.clean()
    }
}

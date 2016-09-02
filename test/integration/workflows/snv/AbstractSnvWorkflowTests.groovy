package workflows.snv

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.joda.time.*
import org.junit.*
import workflows.*

import static org.junit.Assert.*

abstract class AbstractSnvWorkflowTests extends WorkflowTestCase {

    final Double COVERAGE = 30.0

    final String PID = 'stds' //name have to be the same as in the reference data for OTP snv


    ProcessedMergedBamFileService processedMergedBamFileService

    LinkFileUtils linkFileUtils


    AbstractMergedBamFile bamFileControl
    AbstractMergedBamFile bamFileTumor
    ConfigPerProject config
    Individual individual
    Project project
    SamplePair samplePair
    SampleType sampleTypeControl
    SampleType sampleTypeTumor
    SeqType seqType


    abstract ConfigPerProject createConfig()


    final Map createProcessMergedBamFileProperties() {
        DomainFactory.randomProcessedBamFileProperties + [
                coverage: COVERAGE,
        ]
    }


    void setupRoddyBamFile() {
        bamFileTumor = DomainFactory.createRoddyBamFile(createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createRoddyBamFile(createProcessMergedBamFileProperties() + [
                workPackage: DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                config: bamFileTumor.config,
        ])

        commonBamFileSetup()
    }


    void setupProcessMergedBamFile() {
        bamFileTumor = DomainFactory.createProcessedMergedBamFile(createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                createProcessMergedBamFileProperties())

        commonBamFileSetup()
    }


    private void commonBamFileSetup() {
        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        seqType = bamFileTumor.seqType

        project.realmName = realm.name
        assert project.save(flush: true)

        individual.pid = PID
        assert individual.save(flush: true)

        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)

        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        createSnvSpecificSetup()
        createThresholds()
        setupBamFilesInFileSystem()
    }


    @Test
    void testWholeSnvWorkflow() {
        createConfig()

        execute()
        check()
    }


    void createSnvSpecificSetup() {
        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeTumor,
                category: SampleType.Category.DISEASE,
        )

        DomainFactory.createSampleTypePerProject(
                project: project,
                sampleType: sampleTypeControl,
                category: SampleType.Category.CONTROL,
        )

        DomainFactory.createProcessingOption(name: 'timeZone', type: null, value: 'Europe/Berlin')
        samplePair = DomainFactory.createSamplePair(bamFileTumor.mergingWorkPackage, bamFileControl.mergingWorkPackage)
    }


    void createThresholds() {
        DomainFactory.createProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeTumor,
                coverage: COVERAGE,
                numberOfLanes: null,
        )

        DomainFactory.createProcessingThresholds(
                project: project,
                seqType: seqType,
                sampleType: sampleTypeControl,
                coverage: COVERAGE,
                numberOfLanes: null,
        )
    }


    void setupBamFilesInFileSystem() {
        File inputDirectory = new File(getWorkflowData(), 'inputFiles')
        File inputDiseaseBamFile = new File(inputDirectory, 'tumor_SOMEPID_merged.mdup.bam')
        File inputDiseaseBaiFile = new File(inputDirectory, 'tumor_SOMEPID_merged.mdup.bam.bai')
        File inputControlBamFile = new File(inputDirectory, 'control_SOMEPID_merged.mdup.bam')
        File inputControlBaiFile = new File(inputDirectory, 'control_SOMEPID_merged.mdup.bam.bai')

        File diseaseBamFile = bamFileTumor.pathForFurtherProcessing
        File diseaseBaiFile = new File(diseaseBamFile.parentFile, bamFileTumor.baiFileName)
        File controlBamFile = bamFileControl.pathForFurtherProcessing
        File controlBaiFile = new File(controlBamFile.parentFile, bamFileControl.baiFileName)

        linkFileUtils.createAndValidateLinks([
                (inputDiseaseBamFile): diseaseBamFile,
                (inputDiseaseBaiFile): diseaseBaiFile,
                (inputControlBamFile): controlBamFile,
                (inputControlBaiFile): controlBaiFile,
        ], realm)

        bamFileTumor.fileSize = inputDiseaseBamFile.size()
        assert bamFileTumor.save(flush: true)

        bamFileControl.fileSize = inputControlBamFile.size()
        assert bamFileControl.save(flush: true)
    }


    final void check() {
        checkInstanceFinished()
        checkSpecific()
    }


    abstract void checkSpecific()


    void checkInstanceFinished() {
        SnvCallingInstance createdInstance = SnvCallingInstance.listOrderById().last()
        assert createdInstance.processingState == SnvProcessingStates.FINISHED
        assert createdInstance.config == config
        assert createdInstance.sampleType1BamFile == bamFileTumor
        assert createdInstance.sampleType2BamFile == bamFileControl
    }


    File getWorkflowData() {
        new File(getDataDirectory(), 'snv')
    }


    @Override
    Duration getTimeout() {
        Duration.standardMinutes(30)
    }
}

package workflows.analysis.pair

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import workflows.*

abstract class AbstractBamFilePairAnalysisWorkflowTests extends WorkflowTestCase {

    final Double COVERAGE = 30.0

    final String PID = 'stds' //name have to be the same as in the reference data for OTP snv


    ProcessedMergedBamFileService processedMergedBamFileService


    AbstractMergedBamFile bamFileControl
    AbstractMergedBamFile bamFileTumor
    ConfigPerProject config
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    SamplePair samplePair
    SampleType sampleTypeControl
    SampleType sampleTypeTumor
    SeqType seqType


    abstract ConfigPerProject createConfig()

    abstract ReferenceGenome createReferenceGenome()


    final Map createProcessMergedBamFileProperties() {
        DomainFactory.randomProcessedBamFileProperties + [
                coverage: COVERAGE,
        ]
    }


    void setupRoddyWgsBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenome()
        )
        setupRoddyBamFile(tumorMwp)
    }

    void setupRoddyWesBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createExomeSeqType(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenome()
        )
        setupRoddyBamFile(tumorMwp)
    }

    private void setupRoddyBamFile(MergingWorkPackage tumorMwp) {
        bamFileTumor = DomainFactory.createRoddyBamFile([workPackage: tumorMwp] + createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createRoddyBamFile(createProcessMergedBamFileProperties() + [
                workPackage: DomainFactory.createMergingWorkPackage(tumorMwp),
                config: bamFileTumor.config,
        ])

        commonBamFileSetup(tumorMwp.seqType)
    }


    void setupProcessMergedWgsBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createWholeGenomeSeqType(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
                referenceGenome: createReferenceGenome()
        )
        setupProcessMergedBamFile(tumorMwp)
    }

    void setupProcessMergedWesBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: DomainFactory.createExomeSeqType(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
                referenceGenome: createReferenceGenome()
        )
        setupProcessMergedBamFile(tumorMwp)
    }

    private void setupProcessMergedBamFile(MergingWorkPackage tumorMwp) {
        bamFileTumor = DomainFactory.createProcessedMergedBamFile(tumorMwp, createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                createProcessMergedBamFileProperties())

        commonBamFileSetup(tumorMwp.seqType)
    }


    private void commonBamFileSetup(SeqType seqType) {
        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        this.seqType = bamFileTumor.seqType

        project.realmName = realm.name
        assert project.save(flush: true)

        individual.pid = PID
        assert individual.save(flush: true)

        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)

        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        createAnalysisSpecificSetup()
        createThresholds()
        File inputDirectory = new File(getDataDirectory(), 'processedMergedBamFiles')
        if (seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            setupWesBamFilesInFileSystem(inputDirectory)
        } else {
            setupWgsBamFilesInFileSystem(inputDirectory)
        }
    }


    void createAnalysisSpecificSetup() {
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

    void setupWesBamFilesInFileSystem(File inputDirectory) {
        File exomeDirectory = new File(inputDirectory, 'exome')

        File inputDiseaseBamFile = new File(exomeDirectory, 'PLASMA_SOMEPID_EXON_PAIRED_merged.mdup.bam')
        File inputDiseaseBaiFile = new File(exomeDirectory, 'PLASMA_SOMEPID_EXON_PAIRED_merged.mdup.bai')
        File inputControlBamFile = new File(exomeDirectory, 'BLOOD_SOMEPID_EXON_PAIRED_merged.mdup.bam')
        File inputControlBaiFile  = new File(exomeDirectory, 'BLOOD_SOMEPID_EXON_PAIRED_merged.mdup.bai')

        setupBamFilesInFileSystem(inputDiseaseBamFile, inputDiseaseBaiFile, inputControlBamFile, inputControlBaiFile)
    }

    void setupWgsBamFilesInFileSystem(File inputDirectory) {
        File inputDiseaseBamFile = new File(inputDirectory, 'tumor_SOMEPID_merged.mdup.bam')
        File inputDiseaseBaiFile = new File(inputDirectory, 'tumor_SOMEPID_merged.mdup.bam.bai')
        File inputControlBamFile = new File(inputDirectory, 'control_SOMEPID_merged.mdup.bam')
        File inputControlBaiFile = new File(inputDirectory, 'control_SOMEPID_merged.mdup.bam.bai')

        setupBamFilesInFileSystem(inputDiseaseBamFile, inputDiseaseBaiFile, inputControlBamFile, inputControlBaiFile)
    }

    void setupBamFilesInFileSystem(File inputDiseaseBamFile, File inputDiseaseBaiFile, File inputControlBamFile, File inputControlBaiFile) {
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


    abstract File getWorkflowData()

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(30)
    }
}

package workflows.analysis.pair

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*
import org.joda.time.*
import workflows.*
import workflows.analysis.pair.bamfiles.*

abstract class AbstractBamFilePairAnalysisWorkflowTests extends WorkflowTestCase implements SeqTypeAndInputBamFiles {

    final Double COVERAGE = 30.0

    final String PID = 'stds' //name have to be the same as in the reference data for OTP snv


    ProcessedMergedBamFileService processedMergedBamFileService
    BedFileService bedFileService


    AbstractMergedBamFile bamFileControl
    AbstractMergedBamFile bamFileTumor
    ConfigPerProjectAndSeqType config
    Individual individual
    Project project
    ReferenceGenome referenceGenome
    SamplePair samplePair
    SampleType sampleTypeControl
    SampleType sampleTypeTumor
    SeqType seqType


    abstract ConfigPerProjectAndSeqType createConfig()

    abstract ReferenceGenome createReferenceGenome()


    final Map createProcessMergedBamFileProperties() {
        DomainFactory.randomProcessedBamFileProperties + [
                coverage: COVERAGE,
                qcTrafficLightStatus: AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED,
        ]
    }

    void setupRoddyBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createPanCanPipeline(),
                referenceGenome: createReferenceGenome()
        )
        bamFileTumor = DomainFactory.createRoddyBamFile([workPackage: tumorMwp] + createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createRoddyBamFile(createProcessMergedBamFileProperties() + [
                workPackage: DomainFactory.createMergingWorkPackage(tumorMwp),
                config     : bamFileTumor.config,
        ])

        //The qa values are taken from the wgs alignment workflow with one lane
        Map qaValues = [
                insertSizeMedian  : 406,
                insertSizeCV      : 23,
                properlyPaired    : 1919,
                pairedInSequencing: 2120,
        ]

        DomainFactory.createRoddyMergedBamQa(bamFileTumor, qaValues)
        DomainFactory.createRoddyMergedBamQa(bamFileControl, qaValues)

        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }


    void setupProcessedMergedBamFile() {
        MergingWorkPackage tumorMwp = DomainFactory.createMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createDefaultOtpPipeline(),
                referenceGenome: createReferenceGenome()
        )
        bamFileTumor = DomainFactory.createProcessedMergedBamFile(tumorMwp, createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createProcessedMergedBamFile(
                DomainFactory.createMergingWorkPackage(bamFileTumor.mergingWorkPackage),
                createProcessMergedBamFileProperties())

        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }

    void setupExternalBamFile() {
        ExternalMergingWorkPackage tumorMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: seqTypeToUse(),
                pipeline: DomainFactory.createExternallyProcessedPipelineLazy(),
                referenceGenome: createReferenceGenome()
        )
        ExternalMergingWorkPackage controlMwp = DomainFactory.createExternalMergingWorkPackage(
                seqType: tumorMwp.seqType,
                pipeline: tumorMwp.pipeline,
                referenceGenome: tumorMwp.referenceGenome,
                sample: DomainFactory.createSample([
                        individual: tumorMwp.individual,
                ]),
        )

        bamFileTumor = DomainFactory.createExternallyProcessedMergedBamFile([
                workPackage      : tumorMwp,
                insertSizeFile   : 'tumor_insertsize_plot.png_qcValues.txt',
                maximumReadLength: 101,
        ] + createProcessMergedBamFileProperties())

        bamFileControl = DomainFactory.createExternallyProcessedMergedBamFile(createProcessMergedBamFileProperties() + [
                workPackage      : controlMwp,
                insertSizeFile   : 'control_insertsize_plot.png_qcValues.txt',
                maximumReadLength: 101,
        ])
        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }


    private void commonBamFileSetup() {
        adaptSampleTypes()

        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        seqType = bamFileTumor.seqType
        referenceGenome = bamFileControl.referenceGenome

        project.realm = realm
        assert project.save(flush: true)

        individual.pid = PID
        assert individual.save(flush: true)

        bamFileTumor.workPackage.bamFileInProjectFolder = bamFileTumor
        assert bamFileTumor.workPackage.save(flush: true)

        bamFileControl.workPackage.bamFileInProjectFolder = bamFileControl
        assert bamFileControl.workPackage.save(flush: true)

        createSampleTypeCategories()
        createThresholds()
        setupBamFilesInFileSystem()
    }

    void adaptSampleTypes() {}


    void createSampleTypeCategories() {
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
        BamFileSet bamFileSet = getBamFileSet()

        File diseaseBamFile = bamFileTumor.pathForFurtherProcessing
        File diseaseBaiFile = new File(diseaseBamFile.parentFile, bamFileTumor.baiFileName)
        File controlBamFile = bamFileControl.pathForFurtherProcessing
        File controlBaiFile = new File(controlBamFile.parentFile, bamFileControl.baiFileName)

        linkFileUtils.createAndValidateLinks([
                (bamFileSet.diseaseBamFile): diseaseBamFile,
                (bamFileSet.diseaseBaiFile): diseaseBaiFile,
                (bamFileSet.controlBamFile): controlBamFile,
                (bamFileSet.controlBaiFile): controlBaiFile,
        ], realm)

        bamFileTumor.fileSize = bamFileSet.diseaseBamFile.size()
        assert bamFileTumor.save(flush: true)

        bamFileControl.fileSize = bamFileSet.controlBamFile.size()
        assert bamFileControl.save(flush: true)
    }


    abstract File getWorkflowData()

    File getBamFilePairBaseDirectory() {
        new File(getDataDirectory(), 'bamFiles')
    }

    void createBedFileAndLibPrepKit () {
        LibraryPreparationKit kit = DomainFactory.createLibraryPreparationKit(name: "Agilent5withoutUTRs")
        DomainFactory.createBedFile(
                fileName: "Agilent5withoutUTRs_plain.bed",
                libraryPreparationKit: kit,
                referenceGenome: referenceGenome,
        )
        bamFileTumor.containedSeqTracks*.libraryPreparationKit = kit
        bamFileTumor.containedSeqTracks*.save(flush: true)
        bamFileControl.containedSeqTracks*.libraryPreparationKit = kit
        bamFileControl.containedSeqTracks*.save(flush: true)
        bamFileTumor.workPackage.libraryPreparationKit = kit
        bamFileControl.workPackage.libraryPreparationKit = kit
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(90)
    }
}

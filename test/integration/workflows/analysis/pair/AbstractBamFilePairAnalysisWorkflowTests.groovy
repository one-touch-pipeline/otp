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

        commonBamFileSetup()
        createBedFileAndLibPrepKit()
    }


    void setupProcessMergedBamFile() {
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


    private void commonBamFileSetup() {
        adaptSampleTypes()

        individual = bamFileTumor.individual
        project = individual.project
        sampleTypeControl = bamFileControl.sampleType
        sampleTypeTumor = bamFileTumor.sampleType
        seqType = bamFileTumor.seqType
        referenceGenome = bamFileTumor.referenceGenome

        project.realmName = realm.name
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

        DomainFactory.createProcessingOption(name: 'TIME_ZONE', type: null, value: 'Europe/Berlin')
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
        BedFile bedFile = DomainFactory.createBedFile(
                fileName: "Agilent5withoutUTRs_plain.bed",
                libraryPreparationKit: kit,
                referenceGenome: referenceGenome,
        )
        bamFileTumor.containedSeqTracks*.libraryPreparationKit = kit
        bamFileTumor.containedSeqTracks*.save(flush: true)
    }

    @Override
    Duration getTimeout() {
        Duration.standardMinutes(90)
    }
}

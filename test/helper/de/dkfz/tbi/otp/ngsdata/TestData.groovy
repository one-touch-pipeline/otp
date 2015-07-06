package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AlignmentPass.AlignmentState
import de.dkfz.tbi.otp.ngsqc.FastqcBasicStatistics

import static org.junit.Assert.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.ngsdata.RunSegment.DataFormat
import de.dkfz.tbi.otp.ngsdata.RunSegment.FilesStatus

class TestData {

    final static ARBITRARY_LENGTH_FOR_REFERENCE_GENOME = 100

    String referenceGenomePath
    File directory
    File file

    Realm realm
    Project project
    Individual individual
    SampleType sampleType
    Sample sample
    SeqType seqType
    SeqType exomeSeqType
    SeqCenter seqCenter
    SeqPlatform seqPlatform
    Run run
    RunSegment runSegment
    SoftwareTool softwareTool
    SeqTrack seqTrack
    FileType fileType
    DataFile dataFile
    ReferenceGenome referenceGenome
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    /**
     * @deprecated Use the <code>build()</code> method from the test data plugin or the static methods in this class or
     * in {@link DomainFactory}.
     */
    @Deprecated
    void createObjects() {
        referenceGenomePath = "/tmp/reference_genomes/referenceGenome/"

        directory = new File(referenceGenomePath)
        if (!directory.exists()) {
            assertTrue(directory.mkdirs())
        }

        file = new File("${referenceGenomePath}prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        realm = DomainFactory.createRealmDataProcessingDKFZ([
            processingRootPath: TestCase.uniqueNonExistentPath.path,
        ])
        assertNotNull(realm.save(flush: true))

        project = createProject([
            name : "otp_test_project",
            dirName : "otp_test_project",
            realmName : realm.name,
        ])
        assertNotNull(project.save(flush: true))

        individual = createIndividual()
        assertNotNull(individual.save(flush: true))

        sampleType = createSampleType()
        assertNotNull(sampleType.save(flush: true))

        sample = createSample()
        assertNotNull(sample.save(flush: true))

        seqType = new SeqType()
        seqType.name = "WHOLE_GENOME"
        seqType.libraryLayout = "PAIRED"
        seqType.dirName = "whole_genome_sequencing"
        assertNotNull(seqType.save(flush: true))

        exomeSeqType = new SeqType()
        exomeSeqType.name = SeqTypeNames.EXOME.seqTypeName
        exomeSeqType.libraryLayout = "PAIRED"
        exomeSeqType.dirName = "exome_sequencing"
        assertNotNull(exomeSeqType.save(flush: true))

        seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        assertNotNull(seqCenter.save(flush: true))

        seqPlatform = SeqPlatform.build()

        run = createRun("testname1")
        assertNotNull(run.save(flush: true))

        runSegment = createRunSegment(run)
        assertNotNull(runSegment.save(flush: true))

        softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.qualityCode = null
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        assertNotNull(softwareTool.save(flush: true))

        seqTrack = createSeqTrack()
        assertNotNull(seqTrack.save(flush: true))

        dataFile = createDataFile(seqTrack, runSegment)
        assertNotNull(dataFile.save(flush: true))

        fileType = createFileType(FileType.Type.SEQUENCE)

        referenceGenome = createReferenceGenome()
        assertNotNull(referenceGenome.save(flush: true))

        referenceGenomeProjectSeqType = createReferenceGenomeProjectSeqType()
        assertNotNull(referenceGenomeProjectSeqType.save(flush: true))
    }

    static Project createProject(Map properties = [:]) {
        return new Project([
            name: "project",
            dirName: "dirName",
            realmName: "DKFZ",
            alignmentDeciderBeanName: 'dummyNonExistentAlignmentDecider',
        ] + properties)
    }

    Individual createIndividual(Map properties = [:]) {
        return new Individual([
            pid: "654321",
            mockPid: "PID",
            mockPid: "PID",
            type: Individual.Type.REAL,
            project: project,
        ] + properties)
    }

    SampleType createSampleType(Map properties = [:]) {
        return new SampleType([
            name: "TUMOR",
            specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ] + properties)
    }


    Sample createSample(Map properties = [:]) {
        return new Sample([
            individual: individual,
            sampleType: sampleType,
        ] + properties)
    }


    static SeqType createSeqType(Map properties = [:]) {
        return new SeqType([
            name : "WHOLE_GENOME",
            libraryLayout : "PAIRED",
            dirName : "whole_genome_sequencing",
        ] + properties)
    }

    static SeqPlatform findOrSaveSeqPlatform() {
        SeqPlatform seqPlatform = SeqPlatform.findOrSaveWhere(
            name: 'Illumina',
            model: 'LoSeq9999',
            seqPlatformGroup: SeqPlatformGroup.findOrSaveWhere(name: 'LoSeq9XXX'),
        )
        assert seqPlatform
        return seqPlatform
    }

    SeqTrack createSeqTrack(Map properties = [:]) {
        return new SeqTrack([
            laneId: "123",
            seqType: seqType,
            sample: sample,
            run: run,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool,
        ] + properties)
    }

    FastqcProcessedFile createFastqcProcessedFile(Map properties = [:]) {
        return new FastqcProcessedFile([
            fileExists: true,
            contentUploaded: true,
            dataFile: dataFile,
        ] + properties)
    }

    FastqcBasicStatistics createFastqcBasicStatistics(Map properties = [:]) {
        return new FastqcBasicStatistics([
            fileType         : 'Conventional base calls',
            encoding         : 'Sanger / Illumina 1.9',
            totalSequences   : 1,
            filteredSequences: 1,
            sequenceLength   : 1,
        ] + properties)
    }

    DataFile createDataFile(SeqTrack seqTrack, RunSegment runSegment, FileType fileType = this.fileType) {
        return createDataFile(
                seqTrack: seqTrack,
                runSegment: runSegment,
                fileType: fileType,
                )
    }

    DataFile createDataFile(Map properties = [:]) {
        return new DataFile([
            fileName: "datafile",
            fileExists: true,
            fileSize: 1,
            fileType: fileType,
            seqTrack: seqTrack,
            runSegment: runSegment,
            run: run,
            fileWithdrawn: false,
        ] + properties)
    }

    static ReferenceGenome findOrSaveReferenceGenome() {
        return ReferenceGenome.find{true} ?: createReferenceGenome().save(failOnError: true)
    }

    public static ReferenceGenome createReferenceGenome(Map properties = [:]) {
        return new ReferenceGenome([
            name :"hg19_1_24",
            path: "referenceGenome",
            fileNamePrefix: "prefixName",
            length: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
            lengthWithoutN: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
            lengthRefChromosomes: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
            lengthRefChromosomesWithoutN: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
        ] + properties)
    }


    ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:]) {
        return new ReferenceGenomeProjectSeqType([
            project: project,
            seqType: seqType,
            referenceGenome: referenceGenome,
        ] + properties)
    }

    RunSegment createRunSegment(Run run) {
        return createRunSegment(run: run)
    }

    RunSegment createRunSegment(Map properties = [:]) {
        return new RunSegment([
            filesStatus: FilesStatus.FILES_CORRECT,
            metaDataStatus: RunSegment.Status.COMPLETE,
            run: run,
            initialFormat: DataFormat.FILES_IN_DIRECTORY,
            currentFormat: DataFormat.FILES_IN_DIRECTORY,
            dataPath: "/tmp/",
            mdPath: "/tmp/",
        ] + properties)
    }

    Run createRun(String name) {
        return createRun(name: name)
    }

    Run createRun(Map properties = [:]) {
        return new Run([
            name: "TestRun",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform,
            storageRealm: Run.StorageRealm.DKFZ,
        ] + properties)
    }


    ExomeSeqTrack createExomeSeqTrack(Run run) {
        ExomeSeqTrack exomeSeqTrack = new ExomeSeqTrack(
                laneId: "laneId",
                run: run,
                sample: sample,
                seqType: exomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                libraryPreparationKit: null
                )
        assertNotNull(exomeSeqTrack.save())
        return exomeSeqTrack
    }


    LibraryPreparationKit createLibraryPreparationKit(String name) {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: name
                )
        assertNotNull(libraryPreparationKit.save())
        return libraryPreparationKit
    }


    BedFile createBedFile(ReferenceGenome referenceGenome, LibraryPreparationKit libraryPreparationKit) {
        BedFile bedFile = new BedFile (
                fileName: "BedFile",
                targetSize: 10000000,
                referenceGenome: referenceGenome,
                libraryPreparationKit: libraryPreparationKit,
                )
        assertNotNull(bedFile.save())
        return bedFile
    }

    FileType createFileType(FileType.Type type) {
        fileType = new FileType(
                type: type
                )
        assertNotNull(fileType.save())
        return fileType
    }


    void addKitToExomeSeqTrack(ExomeSeqTrack exomeSeqTrack, LibraryPreparationKit sameLibraryPreparationKit) {
        exomeSeqTrack.libraryPreparationKit = sameLibraryPreparationKit
        exomeSeqTrack.kitInfoReliability = InformationReliability.KNOWN
        assertNotNull(exomeSeqTrack.save(flush: true))
    }

    static AlignmentPass createAndSaveAlignmentPass(Map properties = [:]) {
        AlignmentPass alignmentPass = new TestData().createAlignmentPass(properties)
        assert alignmentPass.save(failOnError: true)
        return alignmentPass
    }

    AlignmentPass createAlignmentPass(Map properties = [:]) {
        final SeqTrack seqTrack = properties.get('seqTrack') ?: seqTrack ?: SeqTrack.build()
        final MergingWorkPackage workPackage = findOrSaveMergingWorkPackage(
                seqTrack,
                properties.get('referenceGenome'),
                properties.get('workflow')
        )
        assert workPackage.save(failOnError: true)
        final AlignmentPass alignmentPass = new AlignmentPass([
            identifier: AlignmentPass.nextIdentifier(seqTrack),
            seqTrack: seqTrack,
            workPackage: workPackage,
            alignmentState: AlignmentState.FINISHED,
        ] + properties)
        return alignmentPass
    }

    ProcessedSaiFile createProcessedSaiFile(Map properties = [:]) {
        return new ProcessedSaiFile([
            fileExists: true,
            dataFile: dataFile,
        ] + properties)
    }

    /**
     * No default alignment provided, therefore the alignment needs to be passed always or <code>null</code> is used.
     */
    static ProcessedBamFile createProcessedBamFile(Map properties = [:]) {
        return new ProcessedBamFile([
            type: AbstractBamFile.BamType.SORTED,
            withdrawn: false,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED,
            status: AbstractBamFile.State.DECLARED,
        ] + properties)
    }

    static Workflow findOrSaveWorkflow() {
        return Workflow.findOrCreateByNameAndType(Workflow.Name.DEFAULT_OTP, Workflow.Type.ALIGNMENT)
                .save(failOnError: true, flush: true)
    }


    static MergingWorkPackage createMergingWorkPackage(Map properties = [:]) {
        final MergingWorkPackage mergingWorkPackage = new MergingWorkPackage([
                seqPlatformGroup: properties.get('seqPlatformGroup') ?: SeqPlatformGroup.build(),
                referenceGenome: properties.get('referenceGenome') ?: findOrSaveReferenceGenome(),
                workflow: properties.get('workflow') ?: findOrSaveWorkflow(),
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        ] + properties)
        return mergingWorkPackage
    }

    static MergingWorkPackage findOrSaveMergingWorkPackage(SeqTrack seqTrack, ReferenceGenome referenceGenome = null, Workflow workflow = null) {
        if (referenceGenome == null || workflow == null) {
            MergingWorkPackage workPackage = MergingWorkPackage.findWhere(
                    sample: seqTrack.sample,
                    seqType: seqTrack.seqType,
            )
            if (workPackage != null) {
                assert workPackage.seqPlatformGroup == seqTrack.seqPlatform.seqPlatformGroup
                return workPackage
            }
        }
        final MergingWorkPackage mergingWorkPackage = MergingWorkPackage.findOrSaveWhere(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatform.seqPlatformGroup,
                referenceGenome: referenceGenome ?: findOrSaveReferenceGenome(),
                workflow: workflow ?: findOrSaveWorkflow(),
        )
        return mergingWorkPackage
    }

    MergingSet createMergingSet(Map properties = [:]) {
        return new MergingSet([
            identifier: 0,
        ] + properties)
    }


    MergingPass createMergingPass(Map properties = [:]) {
        return new MergingPass([
            identifier: 0,
            description: "mergingPass",
        ] + properties)
    }

    static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:]) {
        return DomainFactory.createProcessedMergedBamFile(properties.mergingPass, [
            workPackage: properties.mergingPass.mergingWorkPackage,
            md5sum: "12345678901234567890123456789012",
            type: BamType.MDUP,
            coverage: 30.0,
            qualityAssessmentStatus:QaProcessingStatus.NOT_STARTED,
            status: AbstractBamFile.State.DECLARED,
            numberOfMergedLanes: 3,
            fileSize: 100000,
        ] + properties)
    }
}

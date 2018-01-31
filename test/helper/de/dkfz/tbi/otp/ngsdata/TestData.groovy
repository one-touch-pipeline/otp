package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.QaProcessingStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class TestData {

    final static ARBITRARY_LENGTH_FOR_REFERENCE_GENOME = 100

    @Deprecated
    String referenceGenomePath
    @Deprecated
    File directory
    @Deprecated
    File file
    @Deprecated
    Realm realm
    @Deprecated
    Project project
    @Deprecated
    Individual individual
    @Deprecated
    SampleType sampleType
    @Deprecated
    Sample sample
    @Deprecated
    SeqType seqType
    @Deprecated
    SeqType exomeSeqType
    @Deprecated
    SeqCenter seqCenter
    @Deprecated
    SeqPlatform seqPlatform
    @Deprecated
    Run run
    @Deprecated
    RunSegment runSegment
    @Deprecated
    SoftwareTool softwareTool
    @Deprecated
    SeqTrack seqTrack
    @Deprecated
    FileType fileType
    @Deprecated
    DataFile dataFile
    @Deprecated
    ReferenceGenome referenceGenome
    @Deprecated
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType

    /**
     * @deprecated Use the <code>build()</code> method from the test data plugin or the static methods in this class or
     * in {@link DomainFactory}.
     */
    @Deprecated
    void createObjects() {
        File testDir = new File(TestCase.createEmptyTestDirectory(), "referenceGenome")

        referenceGenomePath = testDir.absolutePath

        directory = new File(referenceGenomePath)
        if (!directory.exists()) {
            assertTrue(directory.mkdirs())
        }

        file = new File("${referenceGenomePath}prefixName.fa")
        if (!file.exists()) {
            file.createNewFile()
            file << "test"
        }

        realm = DomainFactory.createRealm([
            name: 'DKFZ',
        ])
        assertNotNull(realm.save(flush: true))

        project = createProject([
            name : "otp_test_project",
            dirName : "otp_test_project",
            realm : realm,
        ])
        assertNotNull(project.save(flush: true))

        individual = createIndividual()
        assertNotNull(individual.save(flush: true))

        sampleType = createSampleType()
        assertNotNull(sampleType.save(flush: true))

        sample = createSample()
        assertNotNull(sample.save(flush: true))

        seqType = DomainFactory.createWholeGenomeSeqType()
        exomeSeqType = DomainFactory.createExomeSeqType()

        seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        assertNotNull(seqCenter.save(flush: true))

        SeqPlatformGroup seqPlatformGroup = new SeqPlatformGroup()
        assert seqPlatformGroup.save(flush: true)

        seqPlatform = new SeqPlatform(name: 'seqPlatform')
        seqPlatform.addToSeqPlatformGroups(seqPlatformGroup)
        assert seqPlatform.save(flush: true)

        run = createRun("testname1")
        assertNotNull(run.save(flush: true))

        runSegment = DomainFactory.createRunSegment()

        softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        assertNotNull(softwareTool.save(flush: true))

        seqTrack = createSeqTrack()
        assertNotNull(seqTrack.save(flush: true))

        fileType = createFileType(FileType.Type.SEQUENCE)

        dataFile = createDataFile(seqTrack, runSegment)
        assertNotNull(dataFile.save(flush: true))

        referenceGenome = createReferenceGenome()
        assertNotNull(referenceGenome.save(flush: true))

        referenceGenomeProjectSeqType = createReferenceGenomeProjectSeqType()
        assertNotNull(referenceGenomeProjectSeqType.save(flush: true))
    }

    static Project createProject(Map properties = [:]) {
        return new Project([
            name: "project",
            dirName: "dirName",
            alignmentDeciderBeanName: 'dummyNonExistentAlignmentDecider',
        ] + properties)
    }

    @Deprecated
    Individual createIndividual(Map properties = [:]) {
        return new Individual([
            pid: "654321",
            mockPid: "PID",
            mockPid: "PID",
            type: Individual.Type.REAL,
            project: project,
        ] + properties)
    }

    static SampleType createSampleType(Map properties = [:]) {
        return new SampleType([
            name: "TUMOR",
            specificReferenceGenome: SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT,
        ] + properties)
    }

    @Deprecated
    Sample createSample(Map properties = [:]) {
        return new Sample([
            individual: individual,
            sampleType: sampleType,
        ] + properties)
    }

    @Deprecated
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

    @Deprecated
    FastqcProcessedFile createFastqcProcessedFile(Map properties = [:]) {
        return new FastqcProcessedFile([
            fileExists: true,
            contentUploaded: true,
            dataFile: dataFile,
        ] + properties)
    }

    @Deprecated
    DataFile createDataFile(SeqTrack seqTrack, RunSegment runSegment, FileType fileType = this.fileType) {
        return createDataFile(
                seqTrack: seqTrack,
                runSegment: runSegment,
                fileType: fileType,
                )
    }

    @Deprecated
    DataFile createDataFile(Map properties = [:]) {
        return DomainFactory.createDataFile([
            fileName: "datafile",
            initialDirectory: TestCase.getUniqueNonExistentPath().path,
            fileExists: true,
            fileSize: 1,
            fileType: fileType,
            seqTrack: seqTrack,
            runSegment: runSegment,
            run: run,
            fileWithdrawn: false,
        ] + properties)
    }

    public static ReferenceGenome createReferenceGenome(Map properties = [:]) {
        return DomainFactory.createReferenceGenome([
                name :"hg19_1_24",
                path: "referenceGenome",
                fileNamePrefix: "prefixName",
                length: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthWithoutN: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthRefChromosomes: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                lengthRefChromosomesWithoutN: ARBITRARY_LENGTH_FOR_REFERENCE_GENOME,
                chromosomeSuffix: "",
                chromosomePrefix: "",
        ] + properties, false)
    }


    @Deprecated
    ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType(Map properties = [:]) {
        return new ReferenceGenomeProjectSeqType([
            project: project,
            seqType: seqType,
            referenceGenome: referenceGenome,
        ] + properties)
    }

    @Deprecated
    Run createRun(String name) {
        return createRun(name: name)
    }

    @Deprecated
    Run createRun(Map properties = [:]) {
        return new Run([
            name: "TestRun",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform,
        ] + properties)
    }

    @Deprecated
    ExomeSeqTrack createExomeSeqTrack(Run run) {
        ExomeSeqTrack exomeSeqTrack = new ExomeSeqTrack(
                laneId: "laneId",
                run: run,
                sample: sample,
                seqType: exomeSeqType,
                seqPlatform: seqPlatform,
                pipelineVersion: softwareTool,
                kitInfoReliability: InformationReliability.KNOWN,
                libraryPreparationKit: LibraryPreparationKit.buildLazy(),
                )
        assertNotNull(exomeSeqTrack.save())
        return exomeSeqTrack
    }


    static LibraryPreparationKit createLibraryPreparationKit(String name) {
        LibraryPreparationKit libraryPreparationKit = new LibraryPreparationKit(
                name: name,
                shortDisplayName: name,
                )
        assertNotNull(libraryPreparationKit.save())
        return libraryPreparationKit
    }


    static BedFile createBedFile(ReferenceGenome referenceGenome, LibraryPreparationKit libraryPreparationKit) {
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


    static void addKitToExomeSeqTrack(ExomeSeqTrack exomeSeqTrack, LibraryPreparationKit sameLibraryPreparationKit) {
        exomeSeqTrack.libraryPreparationKit = sameLibraryPreparationKit
        exomeSeqTrack.kitInfoReliability = InformationReliability.KNOWN
        assertNotNull(exomeSeqTrack.save(flush: true))
    }

    @Deprecated
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

    static MergingWorkPackage createMergingWorkPackage(Map properties = [:]) {
        final MergingWorkPackage mergingWorkPackage = new MergingWorkPackage([
                seqPlatformGroup: properties.get('seqPlatformGroup') ?: DomainFactory.createSeqPlatformGroup(),
                referenceGenome: properties.get('referenceGenome') ?: DomainFactory.createReferenceGenome(),
                libraryPreparationKit: properties.get('libraryPreparationKit'),
                pipeline: properties.get('pipeline') ?: DomainFactory.createDefaultOtpPipeline(),
        ] + properties)
        return mergingWorkPackage
    }

    static MergingSet createMergingSet(Map properties = [:]) {
        return new MergingSet([
            identifier: 0,
        ] + properties)
    }


    static MergingPass createMergingPass(Map properties = [:]) {
        return new MergingPass([
            identifier: 0,
            description: "mergingPass",
        ] + properties)
    }

    static ProcessedMergedBamFile createProcessedMergedBamFile(Map properties = [:]) {
        return DomainFactory.createProcessedMergedBamFileWithoutProcessedBamFile(properties.mergingPass, [
            workPackage: properties.mergingPass.mergingWorkPackage,
            fileOperationStatus: FileOperationStatus.PROCESSED,
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

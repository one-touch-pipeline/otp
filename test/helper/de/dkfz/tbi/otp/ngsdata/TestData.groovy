package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.RunSegment.DataFormat
import de.dkfz.tbi.otp.ngsdata.RunSegment.FilesStatus

class TestData {

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
            processingRootPath: 'tmp',
        ])
        assertNotNull(realm.save(flush: true))

        project = new Project()
        project.name = "otp_test_project"
        project.dirName = "/tmp/alignmentPassService/"
        project.realmName = realm.name
        assertNotNull(project.save(flush: true))

        individual = createIndividual()
        assertNotNull(individual.save(flush: true))

        sampleType = new SampleType()
        sampleType.name = "TUMOR"
        assertNotNull(sampleType.save(flush: true))

        sample = new Sample()
        sample.individual = individual
        sample.sampleType = sampleType
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

        seqPlatform = new SeqPlatform()
        seqPlatform.name = "solid"
        seqPlatform.model = "4"
        assertNotNull(seqPlatform.save(flush: true))

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

    Individual createIndividual(Map properties = [:]) {
        return new Individual([
            pid: "654321",
            mockPid: "PID",
            mockPid: "PID",
            type: Individual.Type.REAL,
            project: project,
        ] + properties)
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


    public ReferenceGenome createReferenceGenome(Map properties = [:]) {
        return new ReferenceGenome([
            name :"hg19_1_24",
            path: "referenceGenome",
            fileNamePrefix: "prefixName",
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
            dataPath: "dataPath",
            mdPath: "mdPath",
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
                        exomeEnrichmentKit: null
                        )
        assertNotNull(exomeSeqTrack.save())
        return exomeSeqTrack
    }


    ExomeEnrichmentKit createEnrichmentKit(String name) {
        ExomeEnrichmentKit exomeEnrichmentKit = new ExomeEnrichmentKit(
                        name: name
                        )
        assertNotNull(exomeEnrichmentKit.save())
        return exomeEnrichmentKit
    }


    BedFile createBedFile(ReferenceGenome referenceGenome, ExomeEnrichmentKit exomeEnrichmentKit) {
        BedFile bedFile = new BedFile (
                        fileName: "BedFile",
                        targetSize: 10000000,
                        referenceGenome: referenceGenome,
                        exomeEnrichmentKit: exomeEnrichmentKit,
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


    void addKitToExomeSeqTrack(ExomeSeqTrack exomeSeqTrack, ExomeEnrichmentKit sameExomeEnrichmentKit) {
        exomeSeqTrack.exomeEnrichmentKit = sameExomeEnrichmentKit
        exomeSeqTrack.kitInfoReliability = InformationReliability.KNOWN
        assertNotNull(exomeSeqTrack.save(flush: true))
    }

    AlignmentPass createAlignmentPass(Map properties = [:]) {
        return new AlignmentPass([
            identifier: 0,
            seqTrack: seqTrack,
            description: "test",
        ] + properties)
    }

    /**
     * No default alignment provided, therefore the alignment needs to be passed always or <code>null</code> is used.
     */
    ProcessedBamFile createProcessedBamFile(Map properties = [:]) {
        return new ProcessedBamFile([
            type: AbstractBamFile.BamType.SORTED,
            withdrawn: false,
            qualityAssessmentStatus: AbstractBamFile.QaProcessingStatus.NOT_STARTED,
            status: AbstractBamFile.State.DECLARED
        ] + properties)
    }

}

package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import de.dkfz.tbi.otp.InformationReliability
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

        fileType = new FileType()
        fileType.type = FileType.Type.SEQUENCE
        assertNotNull(fileType.save(flush: true))

        referenceGenome = new ReferenceGenome()
        referenceGenome.name = "hg19_1_24"
        referenceGenome.path = "referenceGenome"
        referenceGenome.fileNamePrefix = "prefixName"
        assertNotNull(referenceGenome.save(flush: true))

        referenceGenomeProjectSeqType = createReferenceGenomeProjectSeqType()
        assertNotNull(referenceGenomeProjectSeqType.save(flush: true))
    }

    Individual createIndividual() {
        Individual individual = new Individual()
        individual.pid = "654321"
        individual.mockPid = "PUBLIC_PID"
        individual.mockFullName = "PUBLIC_NAME"
        individual.type = Individual.Type.REAL
        individual.project = project
        return individual
    }

    SeqTrack createSeqTrack() {
        SeqTrack seqTrack = new SeqTrack()
        seqTrack.laneId = "123"
        seqTrack.seqType = seqType
        seqTrack.sample = sample
        seqTrack.run = run
        seqTrack.seqPlatform = seqPlatform
        seqTrack.pipelineVersion = softwareTool
        return seqTrack
    }

    DataFile createDataFile(SeqTrack seqTrack, RunSegment runSegment) {
        DataFile dataFile = new DataFile()
        dataFile.fileExists = true
        dataFile.fileSize = 1
        dataFile.fileType = fileType
        dataFile.seqTrack = seqTrack
        dataFile.runSegment = runSegment
        dataFile.run = run
        dataFile.fileWithdrawn = false
        return dataFile
    }


    DataFile createDataFile() {
        DataFile dataFile = new DataFile()
        dataFile.fileExists = true
        dataFile.fileSize = 1
        dataFile.fileType = fileType
        dataFile.seqTrack = seqTrack
        dataFile.runSegment = runSegment
        dataFile.run = run
        dataFile.fileWithdrawn = false
        return dataFile
    }

    ReferenceGenomeProjectSeqType createReferenceGenomeProjectSeqType() {
        ReferenceGenomeProjectSeqType referenceGenomeProjectSeqType = new ReferenceGenomeProjectSeqType()
        referenceGenomeProjectSeqType.project = project
        referenceGenomeProjectSeqType.seqType = seqType
        referenceGenomeProjectSeqType.referenceGenome = referenceGenome
        return referenceGenomeProjectSeqType
    }

    RunSegment createRunSegment(Run run) {
        RunSegment runSegment = new RunSegment()
        runSegment.filesStatus = FilesStatus.FILES_CORRECT
        runSegment.metaDataStatus = RunSegment.Status.COMPLETE
        runSegment.run = run
        runSegment.initialFormat = DataFormat.FILES_IN_DIRECTORY
        runSegment.currentFormat = DataFormat.FILES_IN_DIRECTORY
        runSegment.dataPath = "dataPath"
        runSegment.mdPath = "mdPath"
        return runSegment
    }

    Run createRun(String name) {
        Run run = new Run()
        run.name = name
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        return run
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


    void addKitToExomeSeqTrack(ExomeSeqTrack exomeSeqTrack, ExomeEnrichmentKit sameExomeEnrichmentKit) {
        exomeSeqTrack.exomeEnrichmentKit = sameExomeEnrichmentKit
        exomeSeqTrack.kitInfoReliability = InformationReliability.KNOWN
        assertNotNull(exomeSeqTrack.save(flush: true))
    }
}

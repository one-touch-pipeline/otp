package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import grails.util.Environment

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
    SeqCenter seqCenter
    SeqPlatform seqPlatform
    Run run
    SoftwareTool softwareTool
    SeqTrack seqTrack
    FileType fileType
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

        realm = new Realm()
        realm.name = "def"
        realm.env = Environment.getCurrent().getName()
        realm.operationType = Realm.OperationType.DATA_PROCESSING
        realm.cluster = Realm.Cluster.DKFZ
        realm.rootPath = ""
        realm.processingRootPath = "tmp"
        realm.programsRootPath = ""
        realm.webHost = ""
        realm.host = ""
        realm.port = 8080
        realm.unixUser = ""
        realm.timeout = 1000000
        realm.pbsOptions = ""
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
        seqType.libraryLayout = "SINGLE"
        seqType.dirName = "whole_genome_sequencing"
        assertNotNull(seqType.save(flush: true))

        seqCenter = new SeqCenter()
        seqCenter.name = "DKFZ"
        seqCenter.dirName = "core"
        assertNotNull(seqCenter.save(flush: true))

        seqPlatform = new SeqPlatform()
        seqPlatform.name = "solid"
        seqPlatform.model = "4"
        assertNotNull(seqPlatform.save(flush: true))

        run = new Run()
        run.name = "testname"
        run.seqCenter = seqCenter
        run.seqPlatform = seqPlatform
        run.storageRealm = Run.StorageRealm.DKFZ
        assertNotNull(run.save(flush: true))

        softwareTool = new SoftwareTool()
        softwareTool.programName = "SOLID"
        softwareTool.programVersion = "0.4.8"
        softwareTool.qualityCode = null
        softwareTool.type = SoftwareTool.Type.ALIGNMENT
        assertNotNull(softwareTool.save(flush: true))

        seqTrack = createSeqTrack()
        assertNotNull(seqTrack.save(flush: true))

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

    DataFile createDataFile() {
        DataFile dataFile = new DataFile()
        dataFile.fileExists = true
        dataFile.fileSize = 1
        dataFile.fileType = fileType
        dataFile.seqTrack = seqTrack
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

}

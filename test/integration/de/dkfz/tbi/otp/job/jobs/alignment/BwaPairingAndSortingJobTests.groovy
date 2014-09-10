package de.dkfz.tbi.otp.job.jobs.alignment

import static org.junit.Assert.*
import grails.util.Environment

import org.junit.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type;
import de.dkfz.tbi.otp.dataprocessing.*
import org.apache.commons.logging.Log

class BwaPairingAndSortingJobTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    BwaPairingAndSortingJob bwaPairingAndSortingJob
    AlignmentPassService alignmentPassService
    ProcessedBamFileService processedBamFileService

    private AlignmentPass pass
    private ProcessedBamFile bamFile

    void setUp() {
        bwaPairingAndSortingJob.log = this.log

        Project project = new Project(
            name: "projectName",
            dirName: "dirName",
            realmName: 'DKFZ',
            )
        assertNotNull(project.save([flush: true, failOnError: true]))

        Individual individual = new Individual(
            pid: "pid_1",
            mockPid: "mockPid_1",
            mockFullName: "mockFullName_1",
            type: Individual.Type.UNDEFINED,
            project: project
            )
        assertNotNull(individual.save([flush: true, failOnError: true]))

        SampleType sampleType = new SampleType(
            name: "sampleTypeName"
            )
        assertNotNull(sampleType.save([flush: true, failOnError: true]))

        Sample sample = new Sample(
            individual: individual,
            sampleType: sampleType
            )
        assertNotNull(sample.save([flush: true, failOnError: true]))

        SeqType seqType = new SeqType(
            name: "seqTypeName",
            libraryLayout: "library",
            dirName: "dirName"
            )
        assertNotNull(seqType.save([flush: true, failOnError: true]))

        SeqPlatform seqPlatform = new SeqPlatform(
            name: "Illumina",
            model: "model"
            )
        assertNotNull(seqPlatform.save([flush: true, failOnError: true]))

        SeqCenter seqCenter = new SeqCenter(
            name: "name",
            dirName: "dirName"
            )
        assertNotNull(seqCenter.save([flush: true, failOnError: true]))

        Run run = new Run(
            name: "runName",
            seqCenter: seqCenter,
            seqPlatform: seqPlatform
            )
        assertNotNull(run.save([flush: true, failOnError: true]))

        SoftwareTool softwareTool = new SoftwareTool(
            programName: "softwareToolName",
            programVersion: "version",
            qualityCode: "quality",
            type: SoftwareTool.Type.ALIGNMENT
            )
        assertNotNull(softwareTool.save([flush: true, failOnError: true]))

        SeqTrack seqTrack = new SeqTrack(
            laneId: "laneId",
            run: run,
            sample: sample,
            seqType: seqType,
            seqPlatform: seqPlatform,
            pipelineVersion: softwareTool
            )
        assertNotNull(seqTrack.save([flush: true, failOnError: true]))

        pass = new AlignmentPass(
            identifier: 0,
            seqTrack: seqTrack
            )
        assertNotNull(pass.save([flush: true, failOnError: true]))

        ProcessingOption option = new ProcessingOption(
            name: 'bwaCommand',
            type: null,
            project: null,
            value: 'bwa-0.6.2-tpx',
            comment: 'binary BWA command'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'samtoolsCommand',
            type: null,
            project: null,
            value: 'samtools-0.1.19',
            comment: 'samtools (Tools for alignments in the SAM format)'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'numberOfSampeThreads',
            type: null,
            project: null,
            value: '-t 8',
            comment: 'number of threads to be used in processing with binary bwa sampe'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'numberOfSamToolsSortThreads',
            type: null,
            project: null,
            value: '-@ 8',
            comment: 'number of threads to be used in processing with samtools sort'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'mbufferPairingSorting',
            type: null,
            project: null,
            value: '-m 2G',
            comment: 'size of buffer to be used for pairing with binary bwa sampe'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'insertSizeCutoff',
            type: seqType.libraryLayout,
            project: project,
            value: '-a 1000',
            comment: 'insertSizeCutoff'
        )
        assertNotNull(option.save(flush: true))

        option = new ProcessingOption(
            name: 'samtoolsSortBuffer',
            type: null,
            project: null,
            value: '-m 30000000000',
            comment: 'samtoolsSortBuffer'
        )
        assertNotNull(option.save(flush: true))

        ReferenceGenome refGenome = new ReferenceGenome(
            name: "hg19_1_24",
            path: "hg19_1_24",
            fileNamePrefix: "preffix_hg19_1_24",
            length: ARBITRARY_REFERENCE_GENOME_LENGTH,
            lengthWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
            lengthRefChromosomes: ARBITRARY_REFERENCE_GENOME_LENGTH,
            lengthRefChromosomesWithoutN: ARBITRARY_REFERENCE_GENOME_LENGTH,
        )
        assertNotNull(refGenome.save(flush: true))

        ReferenceGenomeProjectSeqType refGenomeProjectSeqType = new ReferenceGenomeProjectSeqType(
            project: project,
            seqType: seqType,
            referenceGenome: refGenome
        )
        assertNotNull(refGenomeProjectSeqType.save(flush: true))

        FileType fileType = new FileType(
            type: FileType.Type.SEQUENCE,
            vbpPath: "vbpPath"
        )
        assertNotNull(fileType.save(flush: true))

        DataFile dataFile1 = new DataFile(
            fileName: "dataFile1.fastq",
            pathName: "testPath",
            run: run,
            seqTrack: seqTrack,
            used: true,
            project: project,
            fileType: fileType,
            vbpFileName: "dataFile1.fastq"
        )
        dataFile1.validate()
        println dataFile1.errors
        assertNotNull(dataFile1.save(flush: true))

        DataFile dataFile2 = new DataFile(
            fileName: "dataFile1.fastq",
            pathName: "testPath",
            run: run,
            seqTrack: seqTrack,
            used: true,
            project: project,
            fileType: fileType,
            vbpFileName: "dataFile2.fastq"
        )
        assertNotNull(dataFile2.save(flush: true))

        ProcessedSaiFile saiFile1 = new ProcessedSaiFile(
            fileExists: true,
            alignmentPass: pass,
            dataFile: dataFile1
        )
        assertNotNull(saiFile1.save(flush: true))

        ProcessedSaiFile saiFile2 = new ProcessedSaiFile(
            fileExists: true,
            alignmentPass: pass,
            dataFile: dataFile2
        )
        assertNotNull(saiFile2.save(flush: true))

        bamFile = new ProcessedBamFile(
            alignmentPass: pass,
            type: AbstractBamFile.BamType.SORTED
        )
        bamFile.validate()
        bamFile.errors
        assertNotNull(bamFile.save(flush: true))

        Map paths = [
            rootPath: "rootPath",
            processingRootPath: "/tmp/BwaPairingAndSortingJobTests/",
            ]

        Realm realm = DomainFactory.createRealmDataManagementDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        realm = DomainFactory.createRealmDataProcessingDKFZ(paths)
        assertNotNull(realm.save(flush: true))

        File dirs = new File("/tmp/BwaPairingAndSortingJobTests/reference_genomes/hg19_1_24")
        dirs.mkdirs()
        File refGenPath = new File("/tmp/BwaPairingAndSortingJobTests/reference_genomes/hg19_1_24/preffix_hg19_1_24.fa")
        refGenPath.createNewFile()
    }

    void tearDown() {
        pass = null
        bamFile = null
    }

    void testCreateGroupHeader() {
        String result = bwaPairingAndSortingJob.createGroupHeader(pass.seqTrack)
        println header()
        println result
        assertEquals(this.header(), result)
    }

    void testCreateCommand() {
        String refGenPath = alignmentPassService.referenceGenomePath(pass)
        String files = bwaPairingAndSortingJob.createSequenceAndSaiFiles(pass)
        String outFilePathNoSuffix = processedBamFileService.getFilePathNoSuffix(bamFile)
        String outFilePath = processedBamFileService.getFilePath(bamFile)
        String bwaLogFilePath = processedBamFileService.bwaSampeErrorLogFilePath(bamFile)
        String bwaErrorCheckingCmd = BwaErrorHelper.failureCheckScript(outFilePath, bwaLogFilePath)
        String sampeCmd = "bwa-0.6.2-tpx sampe -P -T -t 8 -a 1000 -r \"${header()}\" ${refGenPath} ${files}"
        String viewCmd = "samtools-0.1.19 view -uSbh - "
        String sortCmd = "samtools-0.1.19 sort -@ 8 -m 30000000000 - ${outFilePathNoSuffix}"
        String chmodCmd = "chmod 440 ${outFilePath} ${bwaLogFilePath}"
        String mbufferPart = "mbuffer -q -m 2G -l /dev/null"
        String cmd = "${sampeCmd} 2> ${bwaLogFilePath} | ${mbufferPart} | ${viewCmd} | ${mbufferPart} | ${sortCmd} ; ${bwaErrorCheckingCmd} ;${chmodCmd}"
        String result = bwaPairingAndSortingJob.createCommand(pass)
        println sampeCmd
        println viewCmd
        println sortCmd
        println chmodCmd
        println mbufferPart
        println "exp: ${cmd}"
        println "res: ${result}"
        assertEquals(cmd, result)
    }

    private String header() {
        return "@RG\\tID:runName_laneId\\tSM:sample_sampleTypeName_pid_1\\tLB:sampleTypeName_pid_1\\tPL:ILLUMINA"
    }
}

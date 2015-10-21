package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.BAMFileWriter
import net.sf.samtools.BAMIndexer
import net.sf.samtools.SAMFileHeader
import net.sf.samtools.SAMFileReader
import net.sf.samtools.SAMRecord
import net.sf.samtools.SAMSequenceRecord

import groovy.json.JsonSlurper
import org.junit.*

class QaExomeTests extends GroovyTestCase {

    private final static String BASE_DIR = '/tmp/integ-test-qa'

    private Map params = [
        pathBamFile: "${BASE_DIR}/bamFile.bam",
        pathBamIndexFile: "${BASE_DIR}/bamFile.bam.bai",
        pathQaResulsFile: "${BASE_DIR}/qa-results.txt",
        pathCoverateResultsFile: "${BASE_DIR}/coverage.txt",
        pathInsertSizeHistogramFile: "${BASE_DIR}/histogram.txt",
        overrideOutput: true,
        allChromosomeName: 'ALL',
        minAlignedRecordLength: 36,
        minMeanBaseQuality: 0,
        mappingQuality: 0,
        coverageMappingQualityThreshold: 1,
        winSize: 1000,
        binSize: 10,
        testMode: false,
        bedFilePath: "${BASE_DIR}/bed.bed",
        refGenMetaInfoFilePath: "${BASE_DIR}/ref-gen-meta-info.txt"
    ]

    @Before
    void setUp() {
        File baseDir = new File(BASE_DIR)
        baseDir.mkdir()
    }

    @After
    void tearDown() {
        File baseDir = new File(BASE_DIR)
        baseDir.deleteDir()
    }

    @Test
    void testTrivialExomeCase() {
        createSimpleBamFile(params.pathBamFile)
        createBamIndexFile(params.pathBamFile, params.pathBamIndexFile)
        File bedFile = new File(params.bedFilePath)
        bedFile << 'chr17\t1000\t1250' // one read completely on target, another is not on target
        File refGenInfoFile = new File(params.refGenMetaInfoFilePath)
        refGenInfoFile << 'chr17\t1000\t10000'

        QualityAssessmentStatistics.run(params.values() as String[])
        File jsonFile = new File(params.pathQaResulsFile)
        def slurper = new JsonSlurper()
        def json = slurper.parseText(jsonFile.text)

        assertEquals(100, json.chr17.onTargetMappedBases)
        assertEquals(200, json.chr17.allBasesMapped)
        assertEquals(100, json."${params.allChromosomeName}".onTargetMappedBases)
        assertEquals(200, json."${params.allChromosomeName}".allBasesMapped)
    }

    /*
     * A comment to cigar strings:
     * A cigar example string: "3M1I3M1D5M"
     *
     * M = MATCH
     * I = INSERTION
     * D = DELETION
     *
     * 3 bases match
     * 1 base insertion
     * ...
     * Always from the query (read) perspective
     */
    private void createSimpleBamFile(String pathBamFile) {
        File bamFile = new File(pathBamFile)
        BAMFileWriter bamFileWriter = new BAMFileWriter(bamFile)
        SAMSequenceRecord samSequenceRecord = new SAMSequenceRecord('chr17', 78774742)
        samSequenceRecord.setSpecies('homo fantasticus')
        SAMFileHeader samFileHeader = new SAMFileHeader()
        samFileHeader.addSequence(samSequenceRecord)
        bamFileWriter.setHeader(samFileHeader)
        SAMRecord samRecord = new SAMRecord(samFileHeader)
        samRecord.with {
            setReadName('read1')
            setReferenceName('chr17')
            setReadString('GTTCCTGCATAGATAATTGCATGACAATTGCCTTGTCCCTCCTGAATGTGGTTCCTGCATAGATAATTGCATGACAATTGCCTTGTCCCTCCTGAATGTG')
            setBaseQualityString('2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222')
            setAlignmentStart(1100)
            setCigarString('100M')
            setReadPairedFlag(true)
            setProperPairFlag(true)
            setDuplicateReadFlag(false)
            setMappingQuality(10)
            setReadUnmappedFlag(false)
        }
        bamFileWriter.writeAlignment(samRecord)
        samRecord = new SAMRecord(samFileHeader)
        samRecord.with {
            setReadName('read2')
            setReferenceName('chr17')
            setReadString('GTTCCTGCATAGATAATTGCATGACAATTGCCTTGTCCCTCCTGAATGTGGTTCCTGCATAGATAATTGCATGACAATTGCCTTGTCCCTCCTGAATGTG')
            setBaseQualityString('2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222')
            setAlignmentStart(1300)
            setCigarString('100M')
            setReadPairedFlag(true)
            setProperPairFlag(true)
            setDuplicateReadFlag(false)
            setMappingQuality(10)
            setReadUnmappedFlag(false)
        }
        bamFileWriter.writeAlignment(samRecord)
        bamFileWriter.close()
    }

    /*
     * creates a bai index for the bam file
     */
    private void createBamIndexFile(String pathBamFile, String pathBamIndexFile) {
        File bamFile = new File(pathBamFile)
        File indexFile = new File(pathBamIndexFile)
        SAMFileReader samFileReader = new SAMFileReader(bamFile)
        SAMFileHeader header = samFileReader.getFileHeader()
        BAMIndexer bamIndexer = new BAMIndexer(indexFile, header)
        samFileReader.close()
    }
}

package scripts

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.FastqSet
import de.dkfz.tbi.otp.dataprocessing.MergingSet
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import grails.util.Environment
import groovy.sql.Sql
import groovy.util.logging.Log4j
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import javax.sql.DataSource

import static junit.framework.Assert.*

@Log4j
class ImportAnalysisBamFilesTests extends GroovyScriptAwareTestCase {
    DataSource dataSource
    CreateClusterScriptService createClusterScriptService
    final shouldFail = new GroovyTestCase().&shouldFail

    Sample sample
    SeqType seqType
    MergingSet mergingSet
    Project project
    Individual individual
    SampleType sampleType
    SoftwareTool softwareTool
    SeqPlatform seqPlatform
    Run run1
    File testDirectory
    File metaDataFile
    File importShellScriptFile

    static final String SCRIPT_NAME = "scripts/ImportAnalysisBamFiles.groovy"

    static final String projectName = "PROJECT_NAME"
    static final Long individualID = 131117  // The database object ID, not what we call "PID"
    static final String individualPid = individualID as String
    static final String mockPid = "PID"
    static final String refGenomeName = "asdfasdf"
    static final String sampleTypeName = "CONTROL"
    static final String bamType = "RMDUP"
    static final String fileName = "/tumor_PID_merged.bam.rmdup.bam"
    static final String md5sum = "12345678901234567890123456789012"
    static final long fileSize = 123456
    static final List<Long> lanes1 = [184245L, 186982L, 585992L]
    static final List<Long> lanes2 = [12345L, 23456L, 34567L]


    static final String correctHeader = """"File","Md5Sum","Errors","InExcludeList","Size","PID","IndOTP","RefGenome","SampleType","BamType","Lanes","LanesInOTP","MissingLanes","WithdrawnLanes"
"""
    static final String wrongHeader = """"asdf","Md5Sum","Errors","InExcludeList","Size","PID","IndOTP","RefGenome","SampleType","BamType","Lanes","LanesInOTP","MissingLanes","WithdrawnLanes"
"""
    static final String correctData = """"${fileName}","${md5sum}",","no","${fileSize}","${mockPid}",${individualPid},"${refGenomeName}","${sampleTypeName}","${bamType}","111129_SN952_0063_AC0A53ACXX_L005","${lanes1.join(" ")}","",""
"""
    static final String skipData = """"${fileName}","asdfasdf","${md5sum}","YES","${fileSize}","${mockPid}",${individualPid},"${refGenomeName}","${sampleTypeName}","${bamType}","111129_SN952_0063_AC0A53ACXX_L005","${lanes2.join(" ")}","",""
"""
    static final String wrongData = """"${fileName}","${md5sum}","","no","${fileSize}","INVALID_PID","null","${refGenomeName}","${sampleTypeName}","${bamType}","111129_SN952_0063_AC0A53ACXX_L005","${lanes2.join(" ")}","",""
"""
    final Map<String, String> correctDataMap = [
            absoluteFilePath: fileName,
            md5sum          : md5sum,
            errors          : "",
            inExcludeList   : "no",
            fileSize        : fileSize.toString(),
            mockPid         : mockPid,
            pid             : individualPid,
            refGenome       : refGenomeName,
            sampleType      : sampleTypeName,
            bamType         : bamType,
            lanes           : "-----",
            seqTrackIds     : lanes1.join(" "),
            missingLanes    : "", withdrawnLanes: ""
    ]


    @Before
    void setUp() {
        ReferenceGenome refGenome = ReferenceGenome.build(
                name: "${refGenomeName}",
                length: 1000,
                lengthWithoutN: 10000000,
                lengthRefChromosomes: 32,
                lengthRefChromosomesWithoutN: 7,
        )

        Realm copyScriptRealm = Realm.build(
                name: 'BioQuant',
                cluster: Realm.Cluster.BIOQUANT,
                env: Environment.current.name,
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        Realm realm = Realm.build(
                name: 'DKFZ',
                cluster: Realm.Cluster.DKFZ,
                env: 'test',
                operationType: Realm.OperationType.DATA_MANAGEMENT,
        )

        project = Project.build(
                name: "${projectName}",
                realmName: 'DKFZ',
        )

        individual = Individual.build(
                pid: "${individualPid}",
                mockPid: "${mockPid}",
                project: project,
        )
        // Enforce the ID in the database and update the reference
        assert individual.executeUpdate("update Individual set id = ${individualID}") == 1
        individual = Individual.get(individualID)

        sampleType = SampleType.build(
                name: "${sampleTypeName}"
        )

        sample = Sample.build(
                individual: individual,
                sampleType: sampleType,
        )

        seqType = SeqType.build()
        softwareTool = SoftwareTool.build()
        seqPlatform = SeqPlatform.build()

        run1 = Run.build(
                storageRealm: Run.StorageRealm.DKFZ
        )

        testDirectory = TestCase.createEmptyTestDirectory()
        metaDataFile = new File(testDirectory, "bam_metadata_test.csv")
        importShellScriptFile = new File(testDirectory, "${projectName}-import.sh")
    }

    @After
    public void tearDown() {
        TestCase.removeMetaClass(CreateClusterScriptService, createClusterScriptService)
        metaDataFile.parentFile.deleteDir()
    }


    private void createData() {
        lanes1.each { Long lane ->
            createSeqTrack(lane.toString(), lane)
        }
        lanes2.each { Long lane ->
            createSeqTrack(lane.toString(), lane)
        }
    }

    private void checkArgumentsForCreateTransferScript() {
        createClusterScriptService.metaClass.createTransferScript = { List<File> sourceLocations, List<File> targetLocations, List<File> linkLocations, List<String> md5sums, boolean move = false ->
            assert sourceLocations == linkLocations
            assert sourceLocations.size() == md5sums.size()
            assert sourceLocations[0].equals(new File(fileName))
            assert targetLocations == [new ExternallyProcessedMergedBamFile(
                    fastqSet: new FastqSet(seqTracks: [SeqTrack.get(lanes1[0])]),
                    fileName: new File(fileName).name,
                    source: "analysisImport",
                    referenceGenome: ReferenceGenome.findByName(refGenomeName)
            ).getFilePath().getAbsoluteDataManagementPath()]
            assert move
            return ""
        }
    }

    private void checkSuccess() {
        List<FastqSet> fss = FastqSet.withCriteria {
            seqTracks {
                eq("laneId", lanes1.first().toString())
            }
        }
        assert(!fss.empty)

        List<FastqSet> fss2 = FastqSet.withCriteria {
            seqTracks {
                eq("laneId", lanes2.first().toString())
            }
        }
        assert(fss2.empty)

        FastqSet fastqSet = fss.first()
        assertNotNull(fastqSet)
        assert fastqSet.seqTracks*.id.containsAll(lanes1)

        ExternallyProcessedMergedBamFile bamFile = ExternallyProcessedMergedBamFile.findByFastqSet(fastqSet)
        assertNotNull(bamFile)
        assert bamFile.type == AbstractBamFile.BamType.RMDUP
        assert bamFile.md5sum == md5sum
        assert bamFile.fileName == new File(fileName).name
        assert bamFile.fileSize == fileSize
        assert bamFile.source == "analysisImport"
        assert bamFile.referenceGenome.name == refGenomeName
        assertTrue(importShellScriptFile.exists())
    }


    @Test
    void testSuccess() {
        createData()
        writeMetadataFile(correctHeader + correctData)
        checkArgumentsForCreateTransferScript()

        runScript(SCRIPT_NAME, ["project": "${projectName}", "metadata": "${metaDataFile}"])

       checkSuccess()
    }

    @Test
    void testSuccessAfterSkippedNotIncluded() {
        createData()
        writeMetadataFile(correctHeader + skipData + correctData)
        checkArgumentsForCreateTransferScript()

        runScript(SCRIPT_NAME, ["project": "${projectName}", "metadata": "${metaDataFile}"])

        checkSuccess()
    }

    @Test
    void testSuccessAfterSkippedError() {
        createData()
        writeMetadataFile(correctHeader + wrongData + correctData)
        checkArgumentsForCreateTransferScript()

        runScript(SCRIPT_NAME, ["project": "${projectName}", "metadata": "${metaDataFile}"])

        checkSuccess()
    }



    @Test
    void testReadFile() {
        writeMetadataFile(correctHeader + correctData)
        LinkedHashMap<String, String> header = [absoluteFilePath: "File", md5sum: "Md5Sum", errors: "Errors", inExcludeList: "InExcludeList", fileSize: "Size", mockPid: "PID", pid: "IndOTP",
         refGenome: "RefGenome", sampleType: "SampleType", bamType: "BamType", lanes: "Lanes",
         seqTrackIds: "LanesInOTP", missingLanes: "MissingLanes", withdrawnLanes: "WithdrawnLanes",
         ]
        List<Map<String, String>> ret = invokeMethod(new File(SCRIPT_NAME), "readFile",
                [metaDataFile, header], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        assert ret == [[absoluteFilePath: "/tumor_PID_merged.bam.rmdup.bam", md5sum:"12345678901234567890123456789012", errors:"", inExcludeList:"no", fileSize:"123456", mockPid:"PID",
                        pid:PID, refGenome:"asdfasdf", sampleType:"CONTROL", bamType:"RMDUP", lanes:"111129_SN952_0063_AC0A53ACXX_L005",
                        seqTrackIds:"184245 186982 585992", missingLanes:"", withdrawnLanes:""]]
    }

    @Test
    void testWrongHeader() {
        writeMetadataFile(wrongHeader + correctData)

        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Error: Wrong headers/
    }

    @Test
    void testWrongHeadersSize() {
        String wrongHeader = """"asdf","Errors","InExcludeList","Size","PID","IndOTP","RefGenome","SampleType","BamType","Lanes","LanesInOTP","MissingLanes","WithdrawnLanes"
"""
        writeMetadataFile(wrongHeader)
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Error: File must have/
    }



    @Test
    void testInvalidMd5() {
        correctDataMap.md5sum = "-----"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*, because of invalid MD5: .*\./
    }

    @Test
    void testIndividualNotFound() {
        correctDataMap.pid = "-1"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: Individual not found\./
    }

    @Test
    void testWrongProject() {
        Project project2 = Project.build(
                name: "asdfasdf",
                realmName: 'DKFZ',
        )
        individual.project = project2
        individual.save(flush: true)
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: Wrong project:/
    }

    @Test
    void testSampleTypeNotFound() {
        correctDataMap.sampleType = "-----"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: SampleType .* not found\./
    }

    @Test
    void testIndividualDoesntContainSampleType() {
        individual.samples.first().sampleType = SampleType.build(
                name: "ASDF"
        )
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: Individual does not contain sampleType .*\./
    }

    @Test
    void testRefGenNotFound() {
        correctDataMap.refGenome= "-----"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: RefGenome .* not found\./
    }

    @Test
    void testSeqTrackNotFound() {
        createData()
        correctDataMap.seqTrackIds = correctDataMap.seqTrackIds + " 900000"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: SeqTrack .* not found\./
    }

    @Test
    void testIndividualInvalid() {
        createData()
        Individual individual2 = Individual.build(
                project: project,
        )
        Sample sample1 = Sample.build(
                individual: individual2,
                sampleType: sampleType,
        )
        SeqTrack track = SeqTrack.get(lanes1.first())
        track.sample = sample1
        assert track.save(flush: true)

        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: Individual invalid\./
    }

    @Test
    void testNoSeqTracks() {
        correctDataMap.seqTrackIds = ""
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*: No SeqTracks\./
    }

    @Test
    void testInvalidBamType() {
        createData()
        correctDataMap.bamType = "---"
        assert shouldFail (IllegalArgumentException.class, { invokeMethod(new File(SCRIPT_NAME), "validate",
                [correctDataMap, project], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^No enum const class .*/
    }



    @Test
    void testNoMDFile() {
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "${projectName}", metadata: ""])
        }) =~ /Error: Metadata file not given\./
    }
    @Test
    void testMDFileNotReadable() {
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "${projectName}", "metadata": "asdfasdf"])
        }) =~ /Error: Metadata file .* doesn't exist or is not readable\./
    }
    @Test
    void testNoProject() {
        writeMetadataFile('')
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                [project: "",  "metadata": "${metaDataFile}"])
        }) =~ /Error: Project name not given\./
    }
    @Test
    void testProjectNotFound() {
        writeMetadataFile('')
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "asdf", "metadata": "${metaDataFile}"])
        }) =~ /Error: Project .* not found\./
    }
    @Test
    void testOutputFileExists() {
        writeMetadataFile('')
        importShellScriptFile.createNewFile()
        assert shouldFail (AssertionError.class, { runScript(SCRIPT_NAME,
                ["project": "${projectName}", metadata: "${metaDataFile}"])
        }) =~ /Error: Output file .* exists, please move it\./
    }



    @Test
    void testExcluded() {

        correctDataMap.inExcludeList = "YES"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "includes",
                [correctDataMap], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*, set to be excluded\./
    }

    @Test
    void testErrors() {
        correctDataMap.errors = "Fatal Error"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "includes",
                [correctDataMap], ["project": "${projectName}", "metadata": "${metaDataFile}"])
        }) =~ /^Ignored .*, because of errors: .*\./
    }

    @Test
    void testMissingLanes() {
        correctDataMap.missingLanes = "123456"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "includes",
                [correctDataMap], ["project": "${projectName}", "metadata": "${metaDataFile}"])

        }) =~ /^Ignored .*, because of missing lanes: .*\./
    }

    @Test
    @Ignore('For most projects, we will import withdrawn lanes. The check is also disabled in the script.')
    void testWithdrawnLanes() {
        correctDataMap.withdrawnLanes = "456789"
        assert shouldFail (AssertionError.class, { invokeMethod(new File(SCRIPT_NAME), "includes",
                [correctDataMap], ["project": "${projectName}", "metadata": "${metaDataFile}"])

        }) =~ /^Ignored .*, because of withdrawn lanes: .*\./
    }



    private void writeMetadataFile(String content) {
        metaDataFile.withWriter { Writer writer ->
            writer.append(content)
        }
    }

    private SeqTrack createSeqTrack(String laneId = "laneId", long id) {
        // it's not possible to set the id when using domain objects
        Sql sql = new Sql(dataSource.connection)
        sql.executeInsert("insert into seq_track (id, version, lane_id, run_id, sample_id, " +
                "seq_type_id, seq_platform_id, pipeline_version_id, " +
                "fastqc_state, has_final_bam, has_original_bam, insert_size, n_base_pairs, " +
                "n_reads, using_original_bam, quality_encoding, class, linked_externally, kit_info_reliability)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                [id, 0, laneId, run1.id, sample.id, seqType.id, seqPlatform.id, softwareTool.id,
                 "UNKNOWN", false, false, -1, 0, 0, false, "UNKNOWN",
                 "de.dkfz.tbi.otp.ngsdata.SeqTrack", false, InformationReliability.UNKNOWN_UNVERIFIED.rawValue]
        )
        return SeqTrack.get(id)
    }
}

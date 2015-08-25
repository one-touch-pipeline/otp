package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.testing.GroovyScriptAwareTestCase
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static de.dkfz.tbi.otp.ngsdata.Realm.OperationType.DATA_MANAGEMENT
import static de.dkfz.tbi.otp.ngsdata.Realm.OperationType.DATA_PROCESSING

class SampleSwapServiceTests extends GroovyScriptAwareTestCase {
    SampleSwapService sampleSwapService
    LsdfFilesService lsdfFilesService


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        createUserAndRoles()
        temporaryFolder.create()
    }


    @Test
    void test_moveSample() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = Individual.build(project: bamFile.project)
        Realm.build(name: bamFile.project.realmName, operationType: DATA_MANAGEMENT, rootPath: temporaryFolder.newFolder("mgmt"))
        Realm.build(name: bamFile.project.realmName, operationType: DATA_PROCESSING, rootPath: temporaryFolder.newFolder("proc"))
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String fileName = "FILE_NAME"
        bamFile.getFinalExecutionStoreDirectory().mkdirs()
        File execDir = new File(bamFile.getFinalExecutionStoreDirectory(), fileName)
        assert execDir.createNewFile()
        bamFile.getFinalMergedQADirectory().mkdirs()
        File mergedQaDir = new File(bamFile.getFinalMergedQADirectory(), fileName)
        assert mergedQaDir.createNewFile()
        bamFile.getFinalRoddySingleLaneQADirectories().values().iterator().next().mkdirs()
        File singleLaneQaFile = new File(bamFile.getFinalRoddySingleLaneQADirectories().values().iterator().next(), fileName)
        assert singleLaneQaFile.createNewFile()
        String destinationDirectory = AbstractMergedBamFileService.destinationDirectory(bamFile)

        String dataFileName1 = 'DataFileFileName_R1.gz'
        String dataFileName2 = 'DataFileFileName_R2.gz'

        File scriptFolder = temporaryFolder.newFolder("files")

        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.moveSample(
                    bamFile.project.name,
                    bamFile.project.name,
                    bamFile.individual.pid,
                    individual.pid,
                    bamFile.sampleType.name,
                    bamFile.sampleType.name,
                    [(dataFileName1): '', (dataFileName2): ''],
                    script,
                    new StringBuilder(),
                    false,
                    scriptFolder.absolutePath,
            )
        }


        assert scriptFolder.listFiles().length != 0

        File alignmentScript = new File(scriptFolder, "restartAli_${script}.groovy")
        assert alignmentScript.exists()
        assert alignmentScript.text.contains("ctx.seqTrackService.decideAndPrepareForAlignment(SeqTrack.get(${bamFile.seqTracks.iterator().next().id}))")

        File copyScriptOtherUser = new File(scriptFolder, "${script}-OtherUnixUser.sh")
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        assert copyScriptOtherUserContent.contains("#rm -rf ${execDir.absolutePath}")
        assert copyScriptOtherUserContent.contains("#rm -rf ${mergedQaDir.absolutePath}")
        assert copyScriptOtherUserContent.contains("#rm -rf ${singleLaneQaFile.absolutePath}")

        File copyScript = new File(scriptFolder, "${script}.sh")
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' '${lsdfFilesService.getFileViewByPidPath(it)}'")
        }

        assert bamFile.individual == individual
    }


    @Test
    void test_moveIndividual() {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        Project newProject = Project.build(realmName: bamFile.project.realmName)
        String script = "TEST-MOVE-INDIVIDUAL"
        Realm.build(name: bamFile.project.realmName, operationType: DATA_MANAGEMENT, rootPath: temporaryFolder.newFolder("mgmt"))
        Realm.build(name: bamFile.project.realmName, operationType: DATA_PROCESSING, rootPath: temporaryFolder.newFolder("proc"))
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        List<String> dataFilePaths = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
            dataFilePaths.add(lsdfFilesService.getFileFinalPath(it))
        }

        String fileName = "FILE_NAME"
        bamFile.getFinalExecutionStoreDirectory().mkdirs()
        File execDir = new File(bamFile.getFinalExecutionStoreDirectory(), fileName)
        assert execDir.createNewFile()
        bamFile.getFinalMergedQADirectory().mkdirs()
        File mergedQaDir = new File(bamFile.getFinalMergedQADirectory(), fileName)
        assert mergedQaDir.createNewFile()
        bamFile.getFinalRoddySingleLaneQADirectories().values().iterator().next().mkdirs()
        File singleLaneQaFile = new File(bamFile.getFinalRoddySingleLaneQADirectories().values().iterator().next(), fileName)
        assert singleLaneQaFile.createNewFile()
        String destinationDirectory = AbstractMergedBamFileService.destinationDirectory(bamFile)

        File scriptFolder = temporaryFolder.newFolder("files")

        SpringSecurityUtils.doWithAuth("admin") {
            sampleSwapService.moveIndividual(
                    bamFile.project.name,
                    newProject.name,
                    bamFile.individual.pid,
                    bamFile.individual.pid,
                    [(bamFile.sampleType.name): ""],
                    ['DataFileFileName_R1.gz': '', 'DataFileFileName_R2.gz': ''],
                    script,
                    new StringBuilder(),
                    false,
                    scriptFolder.absolutePath,
            )
        }

        assert scriptFolder.listFiles().length != 0

        File alignmentScript = new File(scriptFolder, "restartAli_${script}.groovy")
        assert alignmentScript.exists()

        File copyScriptOtherUser = new File(scriptFolder, "${script}-OtherUnixUser.sh")
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        assert copyScriptOtherUserContent.contains("#rm -rf ${execDir.absolutePath}")
        assert copyScriptOtherUserContent.contains("#rm -rf ${mergedQaDir.absolutePath}")
        assert copyScriptOtherUserContent.contains("#rm -rf ${singleLaneQaFile.absolutePath}")

        File copyScript = new File(scriptFolder, "${script}.sh")
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileFinalPath(it)).getParent()}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}' '${lsdfFilesService.getFileFinalPath(it)}'")
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' '${lsdfFilesService.getFileViewByPidPath(it)}'")
        }

        assert bamFile.project == newProject
    }



    @Test
    void test_changeMetadataEntry() {
        Sample sample = Sample.build()
        SeqTrack seqTrack = SeqTrack.build(sample: sample)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        MetaDataKey metaDataKey = MetaDataKey.build()
        String newValue = "NEW"
        MetaDataEntry metaDataEntry = MetaDataEntry.build(key: metaDataKey, dataFile: dataFile)

        sampleSwapService.changeMetadataEntry(sample, metaDataKey.name, metaDataEntry.value, newValue)

        assert metaDataEntry.value == newValue
    }

    @Test
    void test_changeSeqType_withClassChange() {
        SeqType wgs = DomainFactory.createSeqType(name: SeqTypeNames.WHOLE_GENOME.seqTypeName)
        SeqType exome = DomainFactory.createSeqType(name: SeqTypeNames.EXOME.seqTypeName)
        SeqTrack seqTrack = DomainFactory.createSeqTrack(seqType: wgs)
        assert seqTrack.class == SeqTrack
        long seqTrackId = seqTrack.id

        SeqTrack returnedSeqTrack = sampleSwapService.changeSeqType(seqTrack, exome)

        assert returnedSeqTrack.id == seqTrackId
        assert returnedSeqTrack.seqType.id == exome.id
        assert returnedSeqTrack.class == ExomeSeqTrack
    }

    @Test
    void test_renameSampleIdentifiers() {

        Sample sample = Sample.build()
        SampleIdentifier sampleIdentifier = SampleIdentifier.build(sample: sample)
        String sampleIdentifierName = sampleIdentifier.name
        SeqTrack seqTrack = SeqTrack.build(sample: sample)
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        MetaDataKey metaDataKey = MetaDataKey.build(name: "SAMPLE_ID")
        MetaDataEntry metaDataEntry = MetaDataEntry.build(key: metaDataKey, dataFile: dataFile)

        sampleSwapService.renameSampleIdentifiers(sample, new StringBuilder())

        assert sampleIdentifierName != sampleIdentifier.name
    }


    @Test
    void test_getSingleSampleForIndividualAndSampleType_singleSample() {
        Individual individual = Individual.build()
        SampleType sampleType = SampleType.build()
        Sample sample = Sample.build(individual: individual, sampleType: sampleType)

        assert sample == sampleSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_noSample() {
        Individual individual = Individual.build()
        SampleType sampleType = SampleType.build()

        shouldFail IllegalArgumentException, {
            sampleSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
        }
    }


    @Test
    void test_getAndShowSeqTracksForSample() {
        Sample sample = Sample.build()
        SeqTrack seqTrack = SeqTrack.build(sample: sample)

        assert [seqTrack] == sampleSwapService.getAndShowSeqTracksForSample(sample, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks_noDataFile_shouldFail() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        Map<String, String> dataFileMap = [:]

        shouldFail IllegalArgumentException, {
            sampleSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
        }
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [dataFile] == sampleSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowAlignmentDataFilesForSeqTracks() {
        SeqTrack seqTrack = SeqTrack.build()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DataFile.build(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [] == sampleSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())

        AlignmentLog alignmentLog = AlignmentLog.build(seqTrack: seqTrack)
        DataFile dataFile2 = DataFile.build(alignmentLog: alignmentLog)
        assert [dataFile2] == sampleSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_collectFileNamesOfDataFiles() {
        DataFile dataFile = DataFile.build()

        assert [(dataFile): [directFileName: lsdfFilesService.getFileFinalPath(dataFile), vbpFileName: lsdfFilesService.getFileViewByPidPath(dataFile)]] ==
                sampleSwapService.collectFileNamesOfDataFiles([dataFile])
    }
}

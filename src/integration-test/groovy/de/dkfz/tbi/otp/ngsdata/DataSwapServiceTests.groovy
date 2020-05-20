/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.*
import org.junit.rules.TemporaryFolder

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.DataProcessingFilesService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper

import java.nio.file.Path

@Rollback
@Integration
class DataSwapServiceTests implements UserAndRoles {

    DataSwapService dataSwapService
    LsdfFilesService lsdfFilesService
    DataProcessingFilesService dataProcessingFilesService
    TestConfigService configService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    Path outputFolder

    void setupData() {
        createUserAndRoles()
        outputFolder = temporaryFolder.newFolder("outputFolder").toPath()
        configService = new TestConfigService([
                (OtpProperty.PATH_PROJECT_ROOT)   : outputFolder.toString(),
                (OtpProperty.PATH_PROCESSING_ROOT): outputFolder.toString(),
        ])
    }

    @After
    void tearDown() {
        configService.clean()
    }

    @Test
    void test_moveSample() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        String script = "TEST-MOVE_SAMPLE"
        Individual individual = DomainFactory.createIndividual(project: bamFile.project)

        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
        }

        String dataFileName1 = 'DataFileFileName_R1.gz'
        String dataFileName2 = 'DataFileFileName_R2.gz'

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            dataSwapService.moveSample(
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
                    scriptFolder,
                    false,
            )
        }

        assert scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${script}.groovy").toFile()
        assert alignmentScript.exists()
        assert alignmentScript.text.contains("${bamFile.seqTracks.iterator().next().id},")

        File copyScriptOtherUser = scriptFolder.resolve("${script}-otherUser.sh").toFile()
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = scriptFolder.resolve("${script}.sh").toFile()
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.getComment().comment == "Attention: Datafile swapped!"
        }
    }

    @Test
    void test_moveIndividual() {
        setupData()
        DomainFactory.createAllAlignableSeqTypes()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                roddyExecutionDirectoryNames: [DomainFactory.DEFAULT_RODDY_EXECUTION_STORE_DIRECTORY],
        ])
        Project newProject = DomainFactory.createProject(realm: bamFile.project.realm)
        String scriptName = "TEST-MOVE-INDIVIDUAL"
        SeqTrack seqTrack = bamFile.seqTracks.iterator().next()
        List<String> dataFileLinks = []
        List<String> dataFilePaths = []
        DataFile.findAllBySeqTrack(seqTrack).each {
            new File(lsdfFilesService.getFileFinalPath(it)).parentFile.mkdirs()
            assert new File(lsdfFilesService.getFileFinalPath(it)).createNewFile()
            dataFileLinks.add(lsdfFilesService.getFileViewByPidPath(it))
            dataFilePaths.add(lsdfFilesService.getFileFinalPath(it))
        }
        File missedFile = bamFile.finalMd5sumFile
        File unexpectedFile = new File(bamFile.baseDirectory, 'notExpectedFile.txt')

        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(bamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(bamFile)
        assert missedFile.delete()
        assert unexpectedFile.createNewFile()

        List<File> roddyFilesToDelete = createRoddyFileListToDelete(bamFile)
        File destinationDirectory = bamFile.baseDirectory

        Path scriptFolder = temporaryFolder.newFolder("files").toPath()

        StringBuilder outputLog = new StringBuilder()

        SpringSecurityUtils.doWithAuth(ADMIN) {
            dataSwapService.moveIndividual(
                    bamFile.project.name,
                    newProject.name,
                    bamFile.individual.pid,
                    bamFile.individual.pid,
                    [(bamFile.sampleType.name): ""],
                    ['DataFileFileName_R1.gz': '', 'DataFileFileName_R2.gz': ''],
                    scriptName,
                    outputLog,
                    false,
                    scriptFolder,
            )
        }
        String output = outputLog
        assert output.contains("${DataSwapService.MISSING_FILES_TEXT}\n    ${missedFile}")
        assert output.contains("${DataSwapService.EXCESS_FILES_TEXT}\n    ${unexpectedFile}")

        assert scriptFolder.toFile().listFiles().length != 0

        File alignmentScript = scriptFolder.resolve("restartAli_${scriptName}.groovy").toFile()
        assert alignmentScript.exists()

        File copyScriptOtherUser = scriptFolder.resolve("${scriptName}-otherUser.sh").toFile()
        assert copyScriptOtherUser.exists()
        String copyScriptOtherUserContent = copyScriptOtherUser.text
        roddyFilesToDelete.each {
            assert copyScriptOtherUserContent.contains("#rm -rf ${it}")
        }

        File copyScript = scriptFolder.resolve("${scriptName}.sh").toFile()
        assert copyScript.exists()
        String copyScriptContent = copyScript.text
        assert copyScriptContent.contains("#rm -rf ${destinationDirectory}")
        DataFile.findAllBySeqTrack(seqTrack).eachWithIndex { DataFile it, int i ->
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileFinalPath(it)).getParent()}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}' \\\n   '${lsdfFilesService.getFileFinalPath(it)}'")
            assert copyScriptContent.contains("mv '${dataFilePaths[i]}.md5sum' \\\n     '${lsdfFilesService.getFileFinalPath(it)}.md5sum'")
            assert copyScriptContent.contains("rm -f '${dataFileLinks[i]}'")
            assert copyScriptContent.contains("mkdir -p -m 2750 '${new File(lsdfFilesService.getFileViewByPidPath(it)).getParent()}'")
            assert copyScriptContent.contains("ln -s '${lsdfFilesService.getFileFinalPath(it)}' \\\n      '${lsdfFilesService.getFileViewByPidPath(it)}'")
            assert it.getComment().comment == "Attention: Datafile swapped!"
        }
    }

    @Test
    void test_changeMetadataEntry() {
        setupData()
        Sample sample = DomainFactory.createSample()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        MetaDataKey metaDataKey = DomainFactory.createMetaDataKey()
        String newValue = "NEW"
        MetaDataEntry metaDataEntry = DomainFactory.createMetaDataEntry(key: metaDataKey, dataFile: dataFile)

        dataSwapService.changeMetadataEntry(sample, metaDataKey.name, metaDataEntry.value, newValue)

        assert metaDataEntry.value == newValue
    }

    @Test
    void test_renameSampleIdentifiers() {
        setupData()

        Sample sample = DomainFactory.createSample()
        SampleIdentifier sampleIdentifier = DomainFactory.createSampleIdentifier(sample: sample)
        String sampleIdentifierName = sampleIdentifier.name
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        MetaDataKey metaDataKey = DomainFactory.createMetaDataKey(name: "SAMPLE_ID")
        DomainFactory.createMetaDataEntry(key: metaDataKey, dataFile: dataFile)

        dataSwapService.renameSampleIdentifiers(sample, new StringBuilder())

        assert sampleIdentifierName != sampleIdentifier.name
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_singleSample() {
        setupData()
        Individual individual = DomainFactory.createIndividual()
        SampleType sampleType = DomainFactory.createSampleType()
        Sample sample = DomainFactory.createSample(individual: individual, sampleType: sampleType)

        assert sample == dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
    }

    @Test
    void test_getSingleSampleForIndividualAndSampleType_noSample() {
        setupData()
        Individual individual = DomainFactory.createIndividual()
        SampleType sampleType = DomainFactory.createSampleType()

        TestCase.shouldFail(IllegalArgumentException) {
            dataSwapService.getSingleSampleForIndividualAndSampleType(individual, sampleType, new StringBuilder())
        }
    }

    @Test
    void test_getAndShowSeqTracksForSample() {
        setupData()
        Sample sample = DomainFactory.createSample()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(sample: sample)

        assert [seqTrack] == dataSwapService.getAndShowSeqTracksForSample(sample, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks_noDataFile_shouldFail() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        Map<String, String> dataFileMap = [:]

        TestCase.shouldFail(IllegalArgumentException) {
            dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
        }
    }

    @Test
    void test_getAndValidateAndShowDataFilesForSeqTracks() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [dataFile] == dataSwapService.getAndValidateAndShowDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void test_getAndValidateAndShowAlignmentDataFilesForSeqTracks() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        List<SeqTrack> seqTracks = [seqTrack]
        DataFile dataFile = DomainFactory.createDataFile(seqTrack: seqTrack)
        Map<String, String> dataFileMap = [(dataFile.fileName): ""]

        assert [] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())

        AlignmentLog alignmentLog = DomainFactory.createAlignmentLog(seqTrack: seqTrack)
        DataFile dataFile2 = DomainFactory.createDataFile(alignmentLog: alignmentLog)
        dataFileMap = [(dataFile2.fileName): ""]
        assert [dataFile2] == dataSwapService.getAndValidateAndShowAlignmentDataFilesForSeqTracks(seqTracks, dataFileMap, new StringBuilder())
    }

    @Test
    void testThrowExceptionInCaseOfExternalMergedBamFileIsAttached() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createExternallyProcessedMergedBamFile(
                workPackage: DomainFactory.createExternalMergingWorkPackage(
                        sample: seqTrack.sample,
                        seqType: seqTrack.seqType,
                )
        ).save(flush: true)

        final shouldFail = new GroovyTestCase().&shouldFail
        shouldFail AssertionError, {
            dataSwapService.throwExceptionInCaseOfExternalMergedBamFileIsAttached([seqTrack])
        }
    }

    @Test
    void testThrowExceptionInCaseOfSeqTracksAreOnlyLinked() {
        setupData()
        SeqTrack seqTrack = DomainFactory.createSeqTrack(linkedExternally: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "seqTracks only linked") {
            dataSwapService.throwExceptionInCaseOfSeqTracksAreOnlyLinked([seqTrack])
        }
    }

    private List<File> createRoddyFileListToDelete(RoddyBamFile roddyBamFile) {
        [
                roddyBamFile.workExecutionDirectories,
                roddyBamFile.workMergedQADirectory,
                roddyBamFile.workSingleLaneQADirectories.values(),
        ].flatten()*.absolutePath
    }
}

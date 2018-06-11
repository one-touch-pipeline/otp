package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.job.jobs.utils.JobParameterKeys
import de.dkfz.tbi.otp.job.processing.ClusterJobSchedulerService
import de.dkfz.tbi.otp.ngsdata.ChecksumFileService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.ProcessHelperService
import org.apache.commons.logging.impl.NoOpLog
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.beans.factory.annotation.Autowired

class CalculateFileChecksumMD5JobTests {


    ProcessedMergedBamFile processedMergedBamFile
    QualityAssessmentMergedPass qualityAssessmentMergedPass

    @Autowired
    CalculateFileChecksumMD5Job calculateFileChecksumMD5Job

    @Autowired
    ProcessedMergedBamFileService processedMergedBamFileService

    @Autowired
    ProcessedMergedBamFileQaFileService processedMergedBamFileQaFileService

    @Autowired
    TestConfigService configService

    @Autowired
    ChecksumFileService checksumFileService

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder()

    @Before
    void setUp() {

        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage(pipeline: DomainFactory.createDefaultOtpPipeline())
        processedMergedBamFile = DomainFactory.createProcessedMergedBamFile(mergingWorkPackage, [fileOperationStatus: FileOperationStatus.INPROGRESS])
        qualityAssessmentMergedPass = DomainFactory.createQualityAssessmentMergedPass(abstractMergedBamFile: processedMergedBamFile)

        calculateFileChecksumMD5Job.metaClass.getProcessParameterValue = { -> "${processedMergedBamFile.id}" }

        calculateFileChecksumMD5Job.metaClass.addOutputParameter = { String parameterName, String ids ->
            assert (parameterName == JobParameterKeys.JOB_ID_LIST || parameterName == JobParameterKeys.REALM)
        }

        calculateFileChecksumMD5Job.clusterJobSchedulerService.metaClass.executeJob = { Realm realm, String command ->
            ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        configService = new TestConfigService([
                        'otp.root.path': tmpDir.root.path,
                        'otp.processing.root.path': tmpDir.root.path
        ])
        calculateFileChecksumMD5Job.log = new NoOpLog()
        calculateFileChecksumMD5Job.configService = configService
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(ClusterJobSchedulerService, calculateFileChecksumMD5Job.clusterJobSchedulerService)
        TestCase.removeMetaClass(CalculateFileChecksumMD5Job, calculateFileChecksumMD5Job)
        configService.clean()
    }


    @Test
    void testExecute_FileOperationStatusNotInProgress_ShouldFail() {
        processedMergedBamFile.fileOperationStatus = FileOperationStatus.DECLARED
        assert processedMergedBamFile.save(flush: true)

        assert TestCase.shouldFail(AssertionError) {
            calculateFileChecksumMD5Job.execute()
        }.contains("FileOperationStatus.INPROGRESS")
    }


    @Test
    void testExecute_contentNotChanged() {
        Map<String, File> files = commonSetUp()
        files.picardmd5File << "randomContent"

        assert files.picardmd5File.exists()
        assert !files.bamMd5SumFile.exists()
        assert !files.picardmd5File.text.contains(processedMergedBamFile.bamFileName)

        calculateFileChecksumMD5Job.execute()

        finalCheck(files)
    }


    @Test
    void testExecute_contentAlreadyChanged() {
        Map<String, File> files = commonSetUp()
        files.picardmd5File << "randomContent  ${processedMergedBamFile.bamFileName}"

        assert files.picardmd5File.exists()
        assert !files.bamMd5SumFile.exists()
        assert files.picardmd5File.text == "randomContent  ${processedMergedBamFile.bamFileName}"

        calculateFileChecksumMD5Job.execute()

        finalCheck(files)

    }


    @Test
    void testExecute_md5SumFileAlreadyMoved() {
        Map<String, File> files = commonSetUp()
        files.bamMd5SumFile << "randomContent  ${processedMergedBamFile.bamFileName}"

        assert !files.picardmd5File.exists()
        assert files.bamMd5SumFile.exists()
        assert files.bamMd5SumFile.text == "randomContent  ${processedMergedBamFile.bamFileName}"

        calculateFileChecksumMD5Job.execute()

        finalCheck(files)

    }

    @Test
    void testExecute_baiMd5sumFileAlreadyCreated() {
        Map<String, File> files = commonSetUp()
        files.bamMd5SumFile << "randomContent  ${processedMergedBamFile.bamFileName}"
        files.baiMd5SumFile << "some md5sum string ${files.baiMd5SumFile.name}"

        assert files.baiMd5SumFile.text == "some md5sum string ${files.baiMd5SumFile.name}"

        calculateFileChecksumMD5Job.execute()

        assert files.baiMd5SumFile.text == "some md5sum string ${files.baiMd5SumFile.name}"
        finalCheck(files)
    }

    @Test
    void testExecute_baiMd5sumFileAlreadyCreatedButEmpty() {
        Map<String, File> files = commonSetUp()
        files.bamMd5SumFile << "randomContent  ${processedMergedBamFile.bamFileName}"
        files.baiMd5SumFile << ""

        assert files.baiMd5SumFile.text.isEmpty()

        calculateFileChecksumMD5Job.execute()

        assert files.baiMd5SumFile.text.contains(files.baiFile.name)
        finalCheck(files)
    }


    private Map<String, File> commonSetUp() {
        File tempDir = new File(processedMergedBamFileService.directory(processedMergedBamFile))
        assert tempDir.mkdirs()

        File qaResultDirectory = new File(processedMergedBamFileQaFileService.directoryPath(qualityAssessmentMergedPass))
        assert qaResultDirectory.mkdirs()

        File bamMd5SumFile = new File(tempDir, checksumFileService.md5FileName(processedMergedBamFile.bamFileName))

        String picardMd5FileName = checksumFileService.picardMd5FileName(processedMergedBamFile.getBamFileName())
        File picardmd5File = new File(tempDir, picardMd5FileName)

        String baiFile = processedMergedBamFile.baiFileName
        new File(tempDir, baiFile) << "comeContent"
        File baiMd5SumFile = new File(tempDir, checksumFileService.md5FileName(baiFile))

        new File(qaResultDirectory, "ResultsFile.txt") << "Some Result Content"
        File qaResultMd5sumFile = new File(processedMergedBamFileQaFileService.qaResultsMd5sumFile(processedMergedBamFile))

        return [
           "bamMd5SumFile": bamMd5SumFile,
           "picardmd5File": picardmd5File,
           "baiMd5SumFile": baiMd5SumFile,
           "qaResultMd5sumFile": qaResultMd5sumFile,
           "baiFile": new File(baiFile),
        ]
    }


    private void finalCheck(Map<String, File> files) {
        assert !files.picardmd5File.exists()
        assert files.bamMd5SumFile.exists()
        assert files.bamMd5SumFile.text == "randomContent  ${processedMergedBamFile.bamFileName}"
        assert files.baiMd5SumFile.exists()
        assert files.qaResultMd5sumFile.exists()
    }
}

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Before
import grails.buildtestdata.mixin.Build

/**
 */
@Build([RoddyBamFile, SeqPlatform])
class RoddyBamFileUnitTests {

    SampleType sampleType
    Individual individual
    RoddyBamFile roddyBamFile
    final String TEST_DIR = TestCase.getUniqueNonExistentPath()

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        sampleType = roddyBamFile.sampleType
        individual = roddyBamFile.individual

        AbstractMergedBamFileService.metaClass.static.destinationDirectory = { AbstractMergedBamFile bamFile -> return TEST_DIR }
    }

    @After
    void tearDown() {
        sampleType = null
        individual = null
        roddyBamFile = null
        AbstractMergedBamFileService.metaClass = null
    }

    void testGetRoddyBamFileName() {
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam" == roddyBamFile.getBamFileName()
    }

    void testGetRoddyBaiFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.bai" == roddyBamFile.getBaiFileName()
    }

    void testGetRoddyMd5sumFileName(){
        assert "${sampleType.dirName}_${individual.pid}_merged.mdup.bam.md5sum" == roddyBamFile.getMd5sumFileName()
    }

    void testGetTmpRoddyAlignmentDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}" == roddyBamFile.tmpRoddyAlignmentDirectory.path
    }


    void testGetTmpRoddyQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.getTmpRoddyQADirectory().path
    }


    void testGetFinalQADirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.QUALITY_CONTROL_DIR}" ==
                roddyBamFile.finalQADirectory.path
    }


    void testGetTmpRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.tmpRoddyExecutionStoreDirectory.path
    }


    void testGetFinalRoddyExecutionStoreDirectory_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}" ==
                roddyBamFile.finalExecutionStoreDirectory.path
    }


    void testGetTmpRoddyBamFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.tmpRoddyBamFile.path
    }


    void testGetTmpRoddyBaiFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.tmpRoddyBaiFile.path
    }


    void testGetTmpRoddyMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.tmpRoddyMd5sumFile.path
    }


    void testGetFinalBamFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.bamFileName}" ==
                roddyBamFile.finalBamFile.path
    }


    void testGetFinalBaiFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.baiFileName}" ==
                roddyBamFile.finalBaiFile.path
    }


    void testGetFinalMd5sumFile_AllFine() {
        assert "${TEST_DIR}/${roddyBamFile.md5sumFileName}" ==
                roddyBamFile.finalMd5sumFile.path
    }

    void testGetPathToJobStateLogFile_AllFine() {
        assert "${TEST_DIR}/${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}/roddyExecutionStore/" == roddyBamFile.getPathToJobStateLogFiles()
    }
}

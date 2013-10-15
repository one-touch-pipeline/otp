package de.dkfz.tbi.ngstools.qualityAssessment

import org.junit.*

class QualityAssessmentStatisticsUnitTests {

    QualityAssessmentStatistics qualityAssessmentStatistics
    File testFile
    String pathForFileToOverride

    @Before
    public void setUp() throws Exception {
        qualityAssessmentStatistics = new QualityAssessmentStatistics()
        new File("/tmp/testDirectory/").mkdir()
        testFile = new File("/tmp/testDirectory/QualityAssessmentStatisticTest.txt")
        testFile.createNewFile()
    }

    @After
    public void tearDown() throws Exception {
        qualityAssessmentStatistics = null
        testFile.getParentFile().setWritable(true)
        testFile.getParentFile().deleteDir()
    }

    /**
     * overrideOutput = false
     */
    @Test(expected = RuntimeException.class)
    public void manageOutputFileNoOverride(){
        boolean overrideOutput = false
        pathForFileToOverride = testFile.path
        qualityAssessmentStatistics.manageOutputFile(pathForFileToOverride, overrideOutput)
    }

    /**
     * overrideOutput = false
     * file.exists = false
     */
    @Test
    public void manageOutputFileNoOverrideFileDoesNotExist(){
        boolean overrideOutput = false
        pathForFileToOverride = testFile.path + "_DoesNotExist"
        qualityAssessmentStatistics.manageOutputFile(pathForFileToOverride, overrideOutput)
    }

    /**
     * overrideOutput = true
     * can not be deleted
     */
    @Test(expected = RuntimeException.class)
    public void manageOutputFileOverrideNoDeletable(){
        boolean overrideOutput = true
        pathForFileToOverride = testFile.path
        testFile.setWritable(false)
        testFile.getParentFile().setWritable(false)
        qualityAssessmentStatistics.manageOutputFile(pathForFileToOverride, overrideOutput)
    }

    /**
     * overrideOutput = true
     * file.exists = false
     */
    @Test
    public void manageOutputFileOverrideFileDoesNotExist(){
        boolean overrideOutput = true
        pathForFileToOverride = testFile.path + "_DoesNotExist"
        qualityAssessmentStatistics.manageOutputFile(pathForFileToOverride, overrideOutput)
    }

    /**
     * overrideOutput = true
     * can be deleted
     */
    @Test
    public void manageOutputFileOverrideDeletable(){
        boolean overrideOutput = true
        pathForFileToOverride = testFile.path
        qualityAssessmentStatistics.manageOutputFile(pathForFileToOverride, overrideOutput)
    }
}

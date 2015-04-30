package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.TestConstants
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import org.junit.After
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 */

class AbstractMergedBamFileServiceTests {

    AbstractMergedBamFileService abstractMergedBamFileService

    final String MERGED_BAM_FILES_PATH = "merged-alignment"

    @After
    void setUp() {
        abstractMergedBamFileService.configService = [
                getProjectRootPath: { Project project -> return TestConstants.BASE_TEST_DIRECTORY },
        ] as ConfigService

        abstractMergedBamFileService.mergedAlignmentDataFileService = [
                buildRelativePath: { SeqType type, Sample sample -> return MERGED_BAM_FILES_PATH },
        ] as MergedAlignmentDataFileService
    }

    @Test
    void testDestinationDirectory_ProcessedMergedBamFile() {
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.build()

        String destinationExp = "${TestConstants.BASE_TEST_DIRECTORY}/${MERGED_BAM_FILES_PATH}"
        String destinationAct = abstractMergedBamFileService.destinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }

    @Test
    void testDestinationDirectory_RoddyBamFile() {
        RoddyBamFile mergedBamFile = DomainFactory.createRoddyBamFile()

        String destinationExp = "${TestConstants.BASE_TEST_DIRECTORY}/${MERGED_BAM_FILES_PATH}"
        String destinationAct = abstractMergedBamFileService.destinationDirectory(mergedBamFile)
        assertEquals(destinationExp, destinationAct)
    }
}

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.Test

/**
 */

class AbstractMergedBamFileServiceTests {


    final String MERGED_BAM_FILES_PATH = "merged-alignment"

    @Test
    void testDestinationDirectory_ProcessedMergedBamFile() {
        ProcessedMergedBamFile mergedBamFile = ProcessedMergedBamFile.build()
        Realm realm = Realm.build(name: mergedBamFile.project.realmName)

        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        assert destinationExp == destinationAct
    }

    @Test
    void testDestinationDirectory_RoddyBamFile() {
        RoddyBamFile mergedBamFile = DomainFactory.createRoddyBamFile()
        Realm realm = Realm.build(name: mergedBamFile.project.realmName)

        String destinationExp = expectedMergedAlignmentPath(mergedBamFile, realm)
        String destinationAct = AbstractMergedBamFileService.destinationDirectory(mergedBamFile)

        assert destinationExp == destinationAct
    }

    private String expectedMergedAlignmentPath(AbstractMergedBamFile mergedBamFile, Realm realm) {
        String pidPath = "${realm.rootPath}/${mergedBamFile.project.dirName}/sequencing/${mergedBamFile.seqType.dirName}/view-by-pid/${mergedBamFile.individual.pid}"
        return "${pidPath}/${mergedBamFile.sampleType.dirName}/${mergedBamFile.seqType.libraryLayoutDirName}/${MERGED_BAM_FILES_PATH}/"
    }
}

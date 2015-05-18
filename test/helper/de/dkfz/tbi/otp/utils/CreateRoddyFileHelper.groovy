package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 */
class CreateRoddyFileHelper {


    static void createRoddyAlignmentTempResultFiles(Realm realm, RoddyBamFile roddyBamFile) {
        File mergingBaseDir = new File("${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment")
        if (mergingBaseDir.exists()) {
            assert mergingBaseDir.deleteDir()
        }
        assert mergingBaseDir.mkdirs()
        String bamFileName = "${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_merged.mdup.bam"

        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()
        File tmpQADir = new File(baseTempDir, RoddyBamFile.QUALITY_CONTROL_DIR)
        assert tmpQADir.mkdir()
        File qaFile = new File(tmpQADir, "qa.txt")
        assert qaFile.createNewFile()
        File tmpRoddyExecutionStoreDir  = new File(baseTempDir, RoddyBamFile.RODDY_EXECUTION_STORE_DIR)
        assert tmpRoddyExecutionStoreDir.mkdir()
        File execution1Dir = new File(tmpRoddyExecutionStoreDir, "execution1")
        assert execution1Dir.mkdir()
        File tmpRoddyBamFile  = new File(baseTempDir, bamFileName)
        assert tmpRoddyBamFile.createNewFile()
        tmpRoddyBamFile << "content"
        File tmpRoddyBaiFile  = new File(baseTempDir, "${bamFileName}.bai")
        assert tmpRoddyBaiFile.createNewFile()
        tmpRoddyBaiFile << "content"
        File tmpRoddyMd5sumFile = new File(baseTempDir, "${bamFileName}.md5sum")
        assert tmpRoddyMd5sumFile.createNewFile()
        tmpRoddyMd5sumFile << "a841c64c5825e986c4709ac7298e9366"
    }
}

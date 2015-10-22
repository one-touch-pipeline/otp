package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 */
class CreateRoddyFileHelper {


    //TODO: OTP-1734 delete this test
    @Deprecated
    static void createRoddyAlignmentWorkResultFilesOld(Realm realm, RoddyBamFile roddyBamFile) {
        File mergingBaseDir = new File("${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment")
        if (mergingBaseDir.exists()) {
            assert mergingBaseDir.deleteDir()
        }
        assert mergingBaseDir.mkdirs()
        String bamFileName = "${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_merged.mdup.bam"

        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()
        File tmpQADir = new File(baseTempDir, "${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}")
        assert tmpQADir.mkdirs()
        File qaFile = new File(tmpQADir, "qa.txt")
        assert qaFile.createNewFile()
        String readgroupname = RoddyBamFile.getReadGroupName(roddyBamFile.seqTracks.iterator()[0])
        File tempSingleQADir = new File(baseTempDir, "${RoddyBamFile.QUALITY_CONTROL_DIR}/${readgroupname}")
        assert tempSingleQADir.mkdirs()
        File singleQaFile = new File(tempSingleQADir, "qa.txt")
        assert singleQaFile.createNewFile()
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
        File tmpRoddyMd5sumFile = new File(baseTempDir, "${bamFileName}.md5")
        assert tmpRoddyMd5sumFile.createNewFile()
        tmpRoddyMd5sumFile << "a841c64c5825e986c4709ac7298e9366"
    }

    static private createRoddyAlignmentWorkOrFinalResultFiles(Realm realm, RoddyBamFile roddyBamFile, String workOrFinal) {
        assert CreateFileHelper.createFile(roddyBamFile."get${workOrFinal}MergedQAJsonFile"())

        roddyBamFile."get${workOrFinal}SingleLaneQAJsonFiles"().values().each {
            assert CreateFileHelper.createFile(it)
        }

        assert roddyBamFile."get${workOrFinal}ExecutionStoreDirectory"().mkdirs()
        roddyBamFile."get${workOrFinal}ExecutionDirectories"().each {
            assert it.mkdirs()
            assert new File(it, 'file').createNewFile()
        }
        roddyBamFile."get${workOrFinal}BamFile"() << "content"
        roddyBamFile."get${workOrFinal}BaiFile"() << "content"
        roddyBamFile."get${workOrFinal}Md5sumFile"() << DomainFactory.DEFAULT_MD5_SUM
    }

    static void createRoddyAlignmentWorkResultFiles(Realm realm, RoddyBamFile roddyBamFile) {
        assert roddyBamFile.workDirectory.mkdirs()
        createRoddyAlignmentWorkOrFinalResultFiles(realm, roddyBamFile, "Work")
    }

    static void createRoddyAlignmentFinalResultFiles(Realm realm, RoddyBamFile roddyBamFile) {
        assert roddyBamFile.baseDirectory.mkdirs()
        createRoddyAlignmentWorkOrFinalResultFiles(realm, roddyBamFile, "Final")
    }

}

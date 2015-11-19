package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

/**
 */
class CreateRoddyFileHelper {

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

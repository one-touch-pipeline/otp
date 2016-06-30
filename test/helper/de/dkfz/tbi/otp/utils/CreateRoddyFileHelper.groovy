package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.DomainFactory


class CreateRoddyFileHelper {

    static private createRoddyAlignmentWorkOrFinalResultFiles(RoddyBamFile roddyBamFile, String workOrFinal) {
        assert CreateFileHelper.createFile(roddyBamFile."get${workOrFinal}MergedQAJsonFile"())

        roddyBamFile."get${workOrFinal}SingleLaneQAJsonFiles"().values().each {
            assert CreateFileHelper.createFile(it)
        }

        if (roddyBamFile.seqType.isWgbs()) {
            assert roddyBamFile."get${workOrFinal}MergedMethylationDirectory"().mkdirs()
            roddyBamFile."get${workOrFinal}LibraryQADirectories"().values().each {
                assert it.mkdirs()
            }
            roddyBamFile."get${workOrFinal}LibraryMethylationDirectories"().values().each {
                assert it.mkdirs()
            }
            assert roddyBamFile."get${workOrFinal}MetadataTableFile"().createNewFile()
        }

        assert roddyBamFile."get${workOrFinal}ExecutionStoreDirectory"().mkdirs()
        roddyBamFile."get${workOrFinal}ExecutionDirectories"().each {
            assert it.mkdirs()
            assert new File(it, 'file').createNewFile()
        }
        roddyBamFile."get${workOrFinal}BamFile"() << "content"
        roddyBamFile."get${workOrFinal}BaiFile"() << "content"
        roddyBamFile."get${workOrFinal}Md5sumFile"() << DomainFactory.DEFAULT_MD5_SUM

        if (roddyBamFile.seqType.isWgbs()) {
            File methylationDir = roddyBamFile."get${workOrFinal}MethylationDirectory"()
            File methylationMergedDir = new File(methylationDir, "merged")
            assert new File(methylationMergedDir, "results").mkdirs()
            roddyBamFile.seqTracks.each {
                String libraryName = it.libraryDirectoryName
                File methylationLibraryDir = new File(methylationDir, libraryName)
                assert new File(methylationLibraryDir, "results").mkdirs()
            }
        }
    }

    static void createRoddyAlignmentWorkResultFiles(RoddyBamFile roddyBamFile) {
        assert roddyBamFile.workDirectory.mkdirs()
        createRoddyAlignmentWorkOrFinalResultFiles(roddyBamFile, "Work")
    }

    static void createRoddyAlignmentFinalResultFiles(RoddyBamFile roddyBamFile) {
        assert roddyBamFile.baseDirectory.mkdirs()
        createRoddyAlignmentWorkOrFinalResultFiles(roddyBamFile, "Final")
    }

}

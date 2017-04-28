package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

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
        if (roddyBamFile.seqType.isRna()) {
            new File(roddyBamFile.workDirectory, "additionalArbitraryFile") << "content"
            (roddyBamFile as RnaRoddyBamFile).correspondingWorkChimericBamFile << "content"
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

    static void createRoddySnvResultFiles(RoddySnvCallingInstance roddySnvCallingInstance) {
        CreateFileHelper.createFile(new File(roddySnvCallingInstance.workExecutionStoreDirectory, 'someFile'))

        roddySnvCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(roddySnvCallingInstance.getAllSNVdiagnosticsPlots().absoluteDataManagementPath)

        [SnvCallingStep.CALLING, SnvCallingStep.SNV_DEEPANNOTATION].each {
            CreateFileHelper.createFile(new OtpPath(roddySnvCallingInstance.instancePath, it.getResultFileName(roddySnvCallingInstance.individual)).absoluteDataManagementPath)
        }
    }

    static void createIndelResultFiles(IndelCallingInstance indelCallingInstance) {
        CreateFileHelper.createFile(new File(indelCallingInstance.workExecutionStoreDirectory, 'someFile'))

        indelCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(indelCallingInstance.getCombinedPlotPath())

        indelCallingInstance.getResultFilePathsToValidate().each {
            CreateFileHelper.createFile(it)
        }
    }

    static createSophiaResultFiles(SophiaInstance sophiaInstance) {
        CreateFileHelper.createFile(new File(sophiaInstance.workExecutionStoreDirectory, 'someFile'))
        sophiaInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(sophiaInstance.finalAceseqInputFile)
    }

    static void createInsertSizeFiles(SophiaInstance sophiaInstance) {
        RoddyBamFile tumorBam = sophiaInstance.sampleType1BamFile
        RoddyBamFile controlBam = sophiaInstance.sampleType2BamFile

        CreateFileHelper.createFile(tumorBam.getFinalInsertSizeFile())
        CreateFileHelper.createFile(controlBam.getFinalInsertSizeFile())
    }
}

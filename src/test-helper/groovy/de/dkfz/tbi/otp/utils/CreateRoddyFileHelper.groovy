/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
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

    static void createRoddySnvResultFiles(RoddySnvCallingInstance roddySnvCallingInstance, int minConfidenceScore = 8) {
        CreateFileHelper.createFile(new File(roddySnvCallingInstance.workExecutionStoreDirectory, 'someFile'))

        roddySnvCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(roddySnvCallingInstance.getCombinedPlotPath())

        [
                roddySnvCallingInstance.getSnvCallingResult(),
                roddySnvCallingInstance.getSnvDeepAnnotationResult(),
                getSnvResultRequiredForRunYapsa(roddySnvCallingInstance, minConfidenceScore),
        ].each {
            CreateFileHelper.createFile(it)
        }
    }

    static File getSnvResultRequiredForRunYapsa(RoddySnvCallingInstance instance, int minConfidenceScore) {
        return new File(instance.workDirectory, "snvs_${instance.individual.pid}_somatic_snvs_conf_${minConfidenceScore}_to_10.vcf")
    }

    static void createIndelResultFiles(IndelCallingInstance indelCallingInstance) {
        CreateFileHelper.createFile(new File(indelCallingInstance.workExecutionStoreDirectory, 'someFile'))

        indelCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(indelCallingInstance.getCombinedPlotPath())
        CreateFileHelper.createFile(indelCallingInstance.getIndelQcJsonFile())
        CreateFileHelper.createFile(indelCallingInstance.getSampleSwapJsonFile())

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

    static void createAceseqResultFiles(AceseqInstance aceseqInstance) {
        CreateFileHelper.createFile(new File(aceseqInstance.workExecutionStoreDirectory, 'someFile'))
        aceseqInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }
        aceseqInstance.getAllFiles().each { File plot ->
            CreateFileHelper.createFile(plot)
        }
    }
}

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
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.IndividualService

import java.nio.file.Files
import java.nio.file.Path

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

    static void createRoddySnvResultFiles(RoddySnvCallingInstance roddySnvCallingInstance, IndividualService individualService, int minConfidenceScore = 8) {
        SnvCallingService service = new SnvCallingService(individualService: individualService)
        CreateFileHelper.createFile(new File(roddySnvCallingInstance.workExecutionStoreDirectory, 'someFile'))

        roddySnvCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(service.getCombinedPlotPath(roddySnvCallingInstance))

        [
                service.getSnvCallingResult(roddySnvCallingInstance),
                service.getSnvDeepAnnotationResult(roddySnvCallingInstance),
                getSnvResultRequiredForRunYapsa(roddySnvCallingInstance, minConfidenceScore, individualService),
        ].each {
            CreateFileHelper.createFile(it)
        }
    }

    static Path getSnvResultRequiredForRunYapsa(RoddySnvCallingInstance instance, int minConfidenceScore, IndividualService individualService) {
        SnvCallingService service = new SnvCallingService(individualService: individualService)
        return service.getWorkDirectory(instance).resolve("snvs_${instance.individual.pid}_somatic_snvs_conf_${minConfidenceScore}_to_10.vcf")
    }

    static void createIndelResultFiles(IndelCallingInstance indelCallingInstance, IndividualService individualService) {
        IndelCallingService service = new IndelCallingService(individualService: individualService)
        CreateFileHelper.createFile(new File(indelCallingInstance.workExecutionStoreDirectory, 'someFile'))

        indelCallingInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(service.getCombinedPlotPath(indelCallingInstance))
        CreateFileHelper.createFile(service.getIndelQcJsonFile(indelCallingInstance))
        CreateFileHelper.createFile(service.getSampleSwapJsonFile(indelCallingInstance))

        service.getResultFilePathsToValidate(indelCallingInstance).each {
            CreateFileHelper.createFile(it)
        }
    }

    static createSophiaResultFiles(SophiaInstance sophiaInstance, IndividualService individualService) {
        SophiaService service = new SophiaService(individualService: individualService)
        CreateFileHelper.createFile(new File(sophiaInstance.workExecutionStoreDirectory, 'someFile'))
        sophiaInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(service.getFinalAceseqInputFile(sophiaInstance))
    }

    static void createInsertSizeFiles(SophiaInstance sophiaInstance) {
        RoddyBamFile tumorBam = sophiaInstance.sampleType1BamFile
        RoddyBamFile controlBam = sophiaInstance.sampleType2BamFile

        CreateFileHelper.createFile(tumorBam.getFinalInsertSizeFile())
        CreateFileHelper.createFile(controlBam.getFinalInsertSizeFile())
    }

    static void createAceseqResultFiles(AceseqInstance aceseqInstance, IndividualService individualService) {
        AceseqService service = new AceseqService(individualService: individualService)
        CreateFileHelper.createFile(new File(aceseqInstance.workExecutionStoreDirectory, 'someFile'))
        aceseqInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }
        Files.createDirectories(service.getWorkDirectory(aceseqInstance).resolve("plots"))
        service.getAllFiles(aceseqInstance).each { Path plot ->
            CreateFileHelper.createFile(plot)
        }
    }
}

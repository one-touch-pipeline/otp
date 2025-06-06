/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqService
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingService
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaService
import de.dkfz.tbi.otp.infrastructure.alignment.*
import de.dkfz.tbi.otp.ngsdata.IndividualService

import java.nio.file.Files
import java.nio.file.Path

class CreateRoddyFileHelper {

    static void createRoddyAlignmentResultFiles(AbstractPanCancerFileService service, RoddyBamFile roddyBamFile) {
        Files.createDirectories(service.getDirectoryPath(roddyBamFile))
        assert CreateFileHelper.createFile(service.getMergedQAJsonFile(roddyBamFile))

        service.getSingleLaneQAJsonFiles(roddyBamFile).values().each {
            assert CreateFileHelper.createFile(it)
        }

        if (roddyBamFile.seqType.isWgbs()) {
            Files.createDirectories(service.getMergedMethylationDirectory(roddyBamFile))
            service.getLibraryQADirectories(roddyBamFile).values().each {
                Files.createDirectories(it)
            }
            service.getLibraryMethylationDirectories(roddyBamFile).values().each {
                Files.createDirectories(it)
            }
            Files.createFile(service.getMetadataTableFile(roddyBamFile))
        }

        Files.createDirectories(service.getExecutionStoreDirectory(roddyBamFile))
        service.getExecutionDirectories(roddyBamFile).each {
            Files.createDirectories(it)
            Files.createFile(it.resolve('file'))
        }
        service.getBamFile(roddyBamFile) << "content"
        service.getBaiFile(roddyBamFile) << "content"
        service.getMd5sumFile(roddyBamFile) << HelperUtils.randomMd5sum

        if (roddyBamFile.seqType.isWgbs()) {
            Path methylationDir = service.getMethylationDirectory(roddyBamFile)
            Path methylationMergedDir = methylationDir.resolve("merged")
            Files.createDirectories(methylationMergedDir.resolve("results"))
            roddyBamFile.seqTracks.each {
                String libraryName = it.libraryDirectoryName
                Path methylationLibraryDir = methylationDir.resolve(libraryName)
                Files.createDirectories(methylationLibraryDir.resolve("results"))
            }
        }
        if (roddyBamFile.seqType.isRna()) {
            service.getDirectoryPath(roddyBamFile).resolve("additionalArbitraryFile") << "content"
            (service as RnaAlignmentWorkFileService).getCorrespondingChimericBamFile(roddyBamFile as RnaRoddyBamFile) << "content"
        }
    }

    @Deprecated
    static void createRoddyAlignmentWorkResultFiles(RoddyBamFile roddyBamFile) {
        AbstractAlignmentWorkFileService service = roddyBamFile.seqType.isRna() ? new RnaAlignmentWorkFileService() : new PanCancerWorkFileService()
        service.abstractBamFileService = [
                getBaseDirectory: { RoddyBamFile ignored -> roddyBamFile.baseDirectory.toPath() }
        ] as AbstractBamFileService
        createRoddyAlignmentResultFiles(service, roddyBamFile)
    }

    @Deprecated
    static void createRoddyAlignmentFinalResultFiles(RoddyBamFile roddyBamFile) {
        AbstractAlignmentLinkFileService service = roddyBamFile.seqType.isRna() ? new RnaAlignmentLinkFileService() : new PanCancerLinkFileService()
        service.abstractBamFileService = [
                getBaseDirectory: { RoddyBamFile ignored -> roddyBamFile.baseDirectory.toPath() }
        ] as AbstractBamFileService
        createRoddyAlignmentResultFiles(service, roddyBamFile)
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

    static void createSophiaResultFiles(SophiaInstance sophiaInstance, IndividualService individualService) {
        SophiaService service = new SophiaService(individualService: individualService)
        CreateFileHelper.createFile(new File(sophiaInstance.workExecutionStoreDirectory, 'someFile'))
        sophiaInstance.workExecutionDirectories.each {
            CreateFileHelper.createFile(new File(it, 'someFile'))
        }

        CreateFileHelper.createFile(service.getFinalAceseqInputFile(sophiaInstance))
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

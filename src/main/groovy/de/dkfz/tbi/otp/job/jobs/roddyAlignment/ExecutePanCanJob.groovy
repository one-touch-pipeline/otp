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
package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.jobs.AutoRestartableJob
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RoddyConfigValueService
import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.LsdfFilesService.ensureFileIsReadableAndNotEmpty

@CompileDynamic
@Component
@Scope("prototype")
@Slf4j
class ExecutePanCanJob extends AbstractRoddyAlignmentJob implements AutoRestartableJob {

    @Autowired
    FileSystemService fileSystemService

    @Override
    protected List<String> prepareAndReturnWorkflowSpecificCValues(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "roddyBamFile must not be null"

        List<String> filesToMerge = getFilesToMerge(roddyBamFile)

        RoddyBamFile baseBamFile = roddyBamFile.baseBamFile
        ensureCorrectBaseBamFileIsOnFileSystem(baseBamFile)

        List<String> cValues = prepareAndReturnAlignmentCValues(roddyBamFile)

        if (roddyBamFile.seqType.name == SeqTypeNames.EXOME.seqTypeName) {
            BedFile bedFile = roddyBamFile.bedFile
            File bedFilePath = bedFileService.filePath(bedFile) as File
            cValues.add("TARGET_REGIONS_FILE:${bedFilePath}")
            cValues.add("TARGETSIZE:${bedFile.targetSize}")
        }
        cValues.add("fastq_list:${filesToMerge.join(";")}")
        if (baseBamFile) {
            cValues.add("bam:${baseBamFile.pathForFurtherProcessing}")
        }

        return cValues
    }

    @Override
    protected String prepareAndReturnWorkflowSpecificParameter(RoddyBamFile roddyBamFile) {
        return ""
    }

    /**
     * @deprecated use {@link RoddyConfigValueService#getFilesToMerge()}
     */
    @Deprecated
    protected List<File> getFilesToMerge(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "roddyBamFile must not be null"
        List<File> vbpDataFiles = []

        roddyBamFile.seqTracks.each { SeqTrack seqTrack ->
            List<DataFile> dataFiles = DataFile.findAllBySeqTrackAndIndexFile(seqTrack, false)
            assert seqTrack.seqType.libraryLayout.mateCount == dataFiles.size()
            dataFiles.sort { it.mateNumber }.each { DataFile dataFile ->
                String pathName = lsdfFilesService.getFileViewByPidPath(dataFile)
                FileSystem fs = dataFile.fileLinked ?
                        fileSystemService.filesystemForFastqImport :
                        fileSystemService.filesystemForProcessingForRealm
                Path path = fs.getPath(pathName)
                FileService.ensureFileIsReadableAndNotEmpty(path)
                assert dataFile.fileSize == Files.size(path)
                vbpDataFiles.add(new File(pathName))
            }
        }

        if (roddyBamFile.seqType.libraryLayout.mateCount == 2) {
            vbpDataFiles.collate(2).each {
                MetaDataService.ensurePairedSequenceFileNameConsistency(it.first().path, it.last().path)
            }
            MetaDataService.ensurePairedSequenceFileNameOrder(vbpDataFiles)
        }

        return vbpDataFiles
    }

    @Override
    protected void workflowSpecificValidation(RoddyBamFile roddyBamFile) {
        if (roddyBamFile.seqType.seqTypeName == SeqTypeNames.EXOME) {
            ensureFileIsReadableAndNotEmpty(roddyBamFile.workMergedQATargetExtractJsonFile)
        }
        if (roddyBamFile.seqType.isRna()) {
            RnaRoddyBamFile rnaRoddyBamFile = roddyBamFile as RnaRoddyBamFile
            ensureFileIsReadableAndNotEmpty(rnaRoddyBamFile.correspondingWorkChimericBamFile)
        }
    }
}

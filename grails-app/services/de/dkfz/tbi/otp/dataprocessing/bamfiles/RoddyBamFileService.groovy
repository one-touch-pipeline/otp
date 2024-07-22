/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.filestore.WorkFolder
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

@Transactional
class RoddyBamFileService extends AbstractAbstractBamFileService<RoddyBamFile> implements RoddyResultServiceTrait<RoddyBamFile> {

    AbstractBamFileService abstractBamFileService
    FilestoreService filestoreService

    static final String WORK_DIR_PREFIX = ".merging"

    static final String QUALITY_CONTROL_DIR = "qualitycontrol"
    static final String METHYLATION_DIR = "methylation"

    static final String QUALITY_CONTROL_JSON_FILE_NAME = "qualitycontrol.json"
    static final String QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME = "qualitycontrol_targetExtract.json"

    static final String MERGED_DIR = "merged"

    static final String METADATATABLE_FILE = "metadataTable.tsv"

    static final String INSERT_SIZE_FILE_SUFFIX = 'insertsize_plot.png_qcValues.txt'
    static final String INSERT_SIZE_FILE_DIRECTORY = 'insertsize_distribution'

    // Example: blood_somePid_merged.mdup.bam.md5
    String getMd5sumFileName(RoddyBamFile bamFile) {
        return "${bamFile.bamFileName}.md5"
    }

    Path getBaseDirectory(RoddyBamFile bamFile) {
        return abstractBamFileService.getBaseDirectory(bamFile)
    }

    @Override
    Path getDirectoryPath(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile)
    }

    Path getWorkDirectory(RoddyBamFile bamFile) {
        if (bamFile.workflowArtefact?.producedBy?.workFolder) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        return getBaseDirectory(bamFile).resolve(bamFile.workDirectoryName)
    }

    WorkFolder getWorkFolder(RoddyBamFile bamFile) {
        return bamFile.workflowArtefact?.producedBy?.workFolder
    }

    Path getFinalQADirectory(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(QUALITY_CONTROL_DIR)
    }

    Path getWorkQADirectory(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(QUALITY_CONTROL_DIR)
    }

    Path getFinalMethylationDirectory(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(METHYLATION_DIR)
    }

    Path getWorkMethylationDirectory(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(METHYLATION_DIR)
    }

    Path getFinalMergedQADirectory(RoddyBamFile bamFile) {
        return getFinalQADirectory(bamFile).resolve(MERGED_DIR)
    }

    Path getWorkMergedQADirectory(RoddyBamFile bamFile) {
        return getWorkQADirectory(bamFile).resolve(MERGED_DIR)
    }

    Map<String, Path> getFinalLibraryQADirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getFinalQADirectory(bamFile))
    }

    Map<String, Path> getWorkLibraryQADirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getWorkQADirectory(bamFile))
    }

    Path getFinalMergedMethylationDirectory(RoddyBamFile bamFile) {
        return getFinalMethylationDirectory(bamFile).resolve(MERGED_DIR)
    }

    Path getWorkMergedMethylationDirectory(RoddyBamFile bamFile) {
        return getWorkMethylationDirectory(bamFile).resolve(MERGED_DIR)
    }

    Map<String, Path> getFinalLibraryMethylationDirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getFinalMethylationDirectory(bamFile))
    }

    Map<String, Path> getWorkLibraryMethylationDirectories(RoddyBamFile bamFile) {
        return getLibraryDirectories(bamFile, getWorkMethylationDirectory(bamFile))
    }

    Path getFinalMergedQAJsonFile(RoddyBamFile bamFile) {
        return getFinalMergedQADirectory(bamFile).resolve(QUALITY_CONTROL_JSON_FILE_NAME)
    }

    Path getWorkMergedQAJsonFile(RoddyBamFile bamFile) {
        return getWorkMergedQADirectory(bamFile).resolve(QUALITY_CONTROL_JSON_FILE_NAME)
    }

    Path getWorkMergedQATargetExtractJsonFile(RoddyBamFile bamFile) {
        return getWorkMergedQADirectory(bamFile).resolve(QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME)
    }

    Map<SeqTrack, Path> getFinalSingleLaneQADirectories(RoddyBamFile bamFile) {
        return getSingleLaneQADirectoriesHelper(bamFile, getFinalQADirectory(bamFile))
    }

    Map<SeqTrack, Path> getWorkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return getSingleLaneQADirectoriesHelper(bamFile, getWorkQADirectory(bamFile))
    }

    Map<SeqTrack, Path> getFinalSingleLaneQAJsonFiles(RoddyBamFile bamFile) {
        return getSingleLaneQAJsonFiles(bamFile, 'Final')
    }

    Map<SeqTrack, Path> getWorkSingleLaneQAJsonFiles(RoddyBamFile bamFile) {
        return getSingleLaneQAJsonFiles(bamFile, 'Work')
    }

    private Map<String, Path> getLibraryDirectories(RoddyBamFile bamFile, Path baseDirectory) {
        return bamFile.seqTracks.collectEntries {
            [(it.libraryDirectoryName): baseDirectory.resolve(it.libraryDirectoryName)]
        }
    }

    @CompileDynamic
    private Map<SeqTrack, Path> getSingleLaneQAJsonFiles(RoddyBamFile bamFile, String workOrFinal) {
        return "get${workOrFinal}SingleLaneQADirectories"(bamFile).collectEntries { SeqTrack seqTrack, Path directory ->
            [(seqTrack): directory.resolve(QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    Map<String, Path> getFinalLibraryQAJsonFiles(RoddyBamFile bamFile) {
        return getLibraryQAJsonFiles(bamFile, 'Final')
    }

    Map<String, Path> getWorkLibraryQAJsonFiles(RoddyBamFile bamFile) {
        return getLibraryQAJsonFiles(bamFile, 'Work')
    }

    @CompileDynamic
    private Map<String, Path> getLibraryQAJsonFiles(RoddyBamFile bamFile, String workOrFinal) {
        return "get${workOrFinal}LibraryQADirectories"(bamFile).collectEntries { String lib, Path directory ->
            [(lib): directory.resolve(QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    // Example: run140801_SN751_0197_AC4HUVACXX_D2059_AGTCAA_L001
    private Map<SeqTrack, Path> getSingleLaneQADirectoriesHelper(RoddyBamFile bamFile, Path baseDirectory) {
        Map<SeqTrack, Path> directoriesPerSeqTrack = [:]
        bamFile.seqTracks.each { SeqTrack seqTrack ->
            String readGroupName = seqTrack.readGroupName
            directoriesPerSeqTrack.put(seqTrack, baseDirectory.resolve(readGroupName))
        }
        return directoriesPerSeqTrack
    }

    Path getFinalExecutionStoreDirectory(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(RODDY_EXECUTION_STORE_DIR)
    }

    List<Path> getFinalExecutionDirectories(RoddyBamFile bamFile) {
        return bamFile.roddyExecutionDirectoryNames.collect {
            getFinalExecutionStoreDirectory(bamFile).resolve(it)
        }
    }

    Path getFinalBamFile(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(bamFile.bamFileName)
    }

    Path getWorkBamFile(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(bamFile.bamFileName)
    }

    Path getFinalBaiFile(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(bamFile.baiFileName)
    }

    Path getWorkBaiFile(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(bamFile.baiFileName)
    }

    Path getFinalMd5sumFile(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(getMd5sumFileName(bamFile))
    }

    Path getWorkMd5sumFile(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(getMd5sumFileName(bamFile))
    }

    Path getFinalMetadataTableFile(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(METADATATABLE_FILE)
    }

    Path getWorkMetadataTableFile(RoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(METADATATABLE_FILE)
    }

    Path getFinalInsertSizeDirectory(RoddyBamFile bamFile) {
        return getFinalMergedQADirectory(bamFile).resolve(INSERT_SIZE_FILE_DIRECTORY)
    }

    @Override
    Path getFinalInsertSizeFile(RoddyBamFile bamFile, PathOption... options) {
        return getFinalInsertSizeDirectory(bamFile).resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}_${INSERT_SIZE_FILE_SUFFIX}")
    }

    /**
     * returns whether the old or the new file structure is used.
     * <ul>
     *     <li>old file structure: in the old structure the processing was done in a temporary work directory. After
     *     finishing the processing the files to keep were moved to there final place. At the end the temporary directory
     *     was deleted.</li>
     *     <li>new structure: The new structure is designed to not move files. Therefore a permanent work directory is
     *     used to create files. After creation the files and directories are linked to the final place.</li>
     * </ul>
     *
     * @return true if the old structure is used.
     */
    boolean isOldStructureUsed(RoddyBamFile bamFile) {
        return !bamFile.workDirectoryName
    }

    /**
     * return for old structure the final bam file and for the new structure the work bam file
     */
    @Override
    protected Path getPathForFurtherProcessingNoCheck(RoddyBamFile bamFile) {
        return isOldStructureUsed(bamFile) ? getFinalBamFile(bamFile) : getWorkBamFile(bamFile)
    }
}

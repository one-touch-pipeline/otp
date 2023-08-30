/*
 * Copyright 2011-2023 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

@CompileDynamic
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

    // Example: ${OtpProperty#PATH_PROJECT_ROOT}/${project}/sequencing/whole_genome_sequencing/view-by-pid/somePid/control/paired/merged-alignment/.merging_3
    @Override
    Path getWorkDirectory(RoddyBamFile bamFile) {
        return getBaseDirectory(bamFile).resolve(bamFile.workDirectoryName)
    }

    Path getWorkDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(bamFile.workDirectoryName)
    }

    @Override
    Path getBaseDirectory(RoddyBamFile bamFile) {
        return abstractBamFileService.getBaseDirectory(bamFile)
    }

    Path getBaseDirectory(RoddyBamFile bamFile, PathOption... options) {
        if (options.contains(PathOption.REAL_PATH) && bamFile.workflowArtefact.producedBy.workFolder) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        return getBaseDirectory(bamFile)
    }

    Path getFinalQADirectory(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(QUALITY_CONTROL_DIR)
    }

    Path getWorkQADirectory(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(QUALITY_CONTROL_DIR)
    }

    Path getWorkMethylationDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(METHYLATION_DIR)
    }

    Path getFinalMethylationDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(METHYLATION_DIR)
    }

    Path getFinalMergedQADirectory(RoddyBamFile bamFile, PathOption... options) {
        return getFinalQADirectory(bamFile, options).resolve(MERGED_DIR)
    }

    Path getWorkMergedQADirectory(RoddyBamFile bamFile, PathOption... options) {
        return getWorkQADirectory(bamFile, options).resolve(MERGED_DIR)
    }

    Map<String, Path> getFinalLibraryQADirectories(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryDirectories(bamFile, getFinalQADirectory(bamFile, options))
    }

    Map<String, Path> getWorkLibraryQADirectories(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryDirectories(bamFile, getWorkQADirectory(bamFile, options))
    }

    Path getFinalMergedMethylationDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getFinalMethylationDirectory(bamFile, options).resolve(MERGED_DIR)
    }

    Path getWorkMergedMethylationDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getWorkMethylationDirectory(bamFile, options).resolve(MERGED_DIR)
    }

    Map<String, Path> getFinalLibraryMethylationDirectories(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryDirectories(bamFile, getFinalMethylationDirectory(bamFile, options))
    }

    Map<String, Path> getWorkLibraryMethylationDirectories(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryDirectories(bamFile, getWorkMethylationDirectory(bamFile, options))
    }

    Path getFinalMergedQAJsonFile(RoddyBamFile bamFile, PathOption... options) {
        return getFinalMergedQADirectory(bamFile, options).resolve(QUALITY_CONTROL_JSON_FILE_NAME)
    }

    Path getWorkMergedQAJsonFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkMergedQADirectory(bamFile, options).resolve(QUALITY_CONTROL_JSON_FILE_NAME)
    }

    Path getWorkMergedQATargetExtractJsonFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkMergedQADirectory(bamFile, options).resolve(QUALITY_CONTROL_TARGET_EXTRACT_JSON_FILE_NAME)
    }

    Map<SeqTrack, Path> getFinalSingleLaneQADirectories(RoddyBamFile bamFile, PathOption... options) {
        return getSingleLaneQADirectoriesHelper(bamFile, getFinalQADirectory(bamFile, options))
    }

    Map<SeqTrack, Path> getWorkSingleLaneQADirectories(RoddyBamFile bamFile, PathOption... options) {
        return getSingleLaneQADirectoriesHelper(bamFile, getWorkQADirectory(bamFile, options))
    }

    Map<SeqTrack, Path> getFinalSingleLaneQAJsonFiles(RoddyBamFile bamFile, PathOption... options) {
        return getSingleLaneQAJsonFiles(bamFile, 'Final', options)
    }

    Map<SeqTrack, Path> getWorkSingleLaneQAJsonFiles(RoddyBamFile bamFile, PathOption... options) {
        return getSingleLaneQAJsonFiles(bamFile, 'Work', options)
    }

    private Map<String, Path> getLibraryDirectories(RoddyBamFile bamFile, Path baseDirectory) {
        return bamFile.seqTracks.collectEntries {
            [(it.libraryDirectoryName): baseDirectory.resolve(it.libraryDirectoryName)]
        }
    }

    private Map<SeqTrack, Path> getSingleLaneQAJsonFiles(RoddyBamFile bamFile, String workOrFinal, PathOption... options) {
        return "get${workOrFinal}SingleLaneQADirectories"(bamFile, options).collectEntries { SeqTrack seqTrack, Path directory ->
            [(seqTrack): directory.resolve(QUALITY_CONTROL_JSON_FILE_NAME)]
        }
    }

    Map<String, Path> getFinalLibraryQAJsonFiles(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryQAJsonFiles(bamFile, 'Final', options)
    }

    Map<String, Path> getWorkLibraryQAJsonFiles(RoddyBamFile bamFile, PathOption... options) {
        return getLibraryQAJsonFiles(bamFile, 'Work', options)
    }

    private Map<String, Path> getLibraryQAJsonFiles(RoddyBamFile bamFile, String workOrFinal, PathOption... options) {
        return "get${workOrFinal}LibraryQADirectories"(bamFile, options).collectEntries { String lib, Path directory ->
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

    Path getFinalExecutionStoreDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(RODDY_EXECUTION_STORE_DIR)
    }

    List<Path> getFinalExecutionDirectories(RoddyBamFile bamFile, PathOption... options) {
        return bamFile.roddyExecutionDirectoryNames.collect {
            getFinalExecutionStoreDirectory(bamFile, options).resolve(it)
        }
    }

    Path getFinalBamFile(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(bamFile.bamFileName)
    }

    Path getWorkBamFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(bamFile.bamFileName)
    }

    Path getFinalBaiFile(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(bamFile.baiFileName)
    }

    Path getWorkBaiFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(bamFile.baiFileName)
    }

    Path getFinalMd5sumFile(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(getMd5sumFileName(bamFile))
    }

    Path getWorkMd5sumFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(getMd5sumFileName(bamFile))
    }

    Path getFinalMetadataTableFile(RoddyBamFile bamFile, PathOption... options) {
        return getBaseDirectory(bamFile, options).resolve(METADATATABLE_FILE)
    }

    Path getWorkMetadataTableFile(RoddyBamFile bamFile, PathOption... options) {
        return getWorkDirectory(bamFile, options).resolve(METADATATABLE_FILE)
    }

    Path getFinalInsertSizeDirectory(RoddyBamFile bamFile, PathOption... options) {
        return getFinalMergedQADirectory(bamFile, options).resolve(INSERT_SIZE_FILE_DIRECTORY)
    }

    @Override
    Path getFinalInsertSizeFile(RoddyBamFile bamFile) {
        return getFinalInsertSizeDirectory(bamFile).resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}_${INSERT_SIZE_FILE_SUFFIX}")
    }

    Path getFinalInsertSizeFile(RoddyBamFile bamFile, PathOption... options) {
        return getFinalInsertSizeDirectory(bamFile, options).resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}_${INSERT_SIZE_FILE_SUFFIX}")
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

    protected Path getPathForFurtherProcessingNoCheck(RoddyBamFile bamFile, PathOption... options) {
        return isOldStructureUsed(bamFile) ? getFinalBamFile(bamFile, options) : getWorkBamFile(bamFile, options)
    }
}

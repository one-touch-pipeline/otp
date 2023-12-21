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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.nio.file.*
import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.waitUntilDoesNotExist

@Transactional
class LsdfFilesService {

    private static final String SINGLE_CELL_ALL_WELL = '0_all'

    @Autowired
    RemoteShellHelper remoteShellHelper
    CreateClusterScriptService createClusterScriptService

    IndividualService individualService
    ProjectService projectService

    FileSystemService fileSystemService
    FilestoreService filestoreService

    /**
     * This function return path to the initial location
     * of the given sequenceFile
     */
    Path getFileInitialPathAsPath(RawSequenceFile rawSequenceFile) {
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        return fileSystem.getPath(getFileInitialPath(rawSequenceFile))
    }

    Path getSeqTypeDirectory(RawSequenceFile rawSequenceFile) {
        Path basePath = projectService.getSequencingDirectory(rawSequenceFile.project)
        String seqTypeDirName = rawSequenceFile.seqTrack?.seqType?.dirName
        if (!seqTypeDirName) {
            return null
        }
        return basePath.resolve(seqTypeDirName)
    }

    Path getSeqCenterRunDirectory(RawSequenceFile rawSequenceFile) {
        if (!checkFinalPathDefined(rawSequenceFile)) {
            return null
        }
        Path basePath = getSeqTypeDirectory(rawSequenceFile)
        String centerDir = rawSequenceFile.run.seqCenter.dirName
        return basePath?.resolve(centerDir)?.resolve(rawSequenceFile.run.dirName)
    }

    // method will be cleaned up in issue otp-2375
    @SuppressWarnings("UnusedMethodParameter")
    Path getFinalDirPath(RawSequenceFile rawSequenceFile, PathOption... options) {
        // FinalDir is already the work directory, therefore the option `PathOption.REAL_PATH` is unnecessary
        if (rawSequenceFile.seqTrack.workflowArtefact?.producedBy?.workFolder) {
            return filestoreService.getWorkFolderPath(rawSequenceFile.seqTrack.workflowArtefact.producedBy)
        }
        return getSeqCenterRunDirectory(rawSequenceFile)?.resolve(rawSequenceFile.pathName)
    }

    /**
     * Important function.
     * This function knows all naming conventions and data organization
     *
     * @return String with path or null if path can not be established
     */
    Path getFileFinalPathAsPath(RawSequenceFile rawSequenceFile, PathOption... options) {
        return getFinalDirPath(rawSequenceFile, options)?.resolve(rawSequenceFile?.fileName)
    }

    Path getFileMd5sumFinalPathAsPath(RawSequenceFile rawSequenceFile) {
        return getFileFinalPathAsPath(rawSequenceFile)?.resolveSibling(rawSequenceFile.fileName.concat(".md5sum"))
    }

    private boolean checkFinalPathDefined(RawSequenceFile rawSequenceFile) {
        if (!rawSequenceFile) {
            return false
        }
        return rawSequenceFile.used
    }

    /**
     * Attention: In most cases the method {@link #getSingleCellWellDirectory(RawSequenceFile)} is to use instead of this one to include the well label level.
     */
    Path getSampleTypeDirectory(RawSequenceFile rawSequenceFile) {
        Path basePath = individualService.getViewByPidPath(rawSequenceFile.individual, rawSequenceFile.seqType)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        String antiBodyTarget = seqTrack.seqType.hasAntibodyTarget ? "-${seqTrack.antibodyTarget.name}" : ""
        return basePath.resolve("${seqTrack.sample.sampleType.dirName}${antiBodyTarget}")
    }

    Path getSingleCellWellDirectory(RawSequenceFile rawSequenceFile, WellDirectory wellDirectory = null) {
        Path basePath = getSampleTypeDirectory(rawSequenceFile)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        if (seqTrack.singleCellWellLabel && seqTrack.seqType.singleCell) {
            return basePath.resolve(wellDirectory == WellDirectory.ALL_WELL ? SINGLE_CELL_ALL_WELL : seqTrack.singleCellWellLabel)
        }
        return basePath
    }

    Path getRunDirectory(RawSequenceFile rawSequenceFile, WellDirectory wellDirectory = null) {
        Path basePath = getSingleCellWellDirectory(rawSequenceFile, wellDirectory)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        return basePath.resolve(seqTrack.seqType.libraryLayoutDirName).resolve(seqTrack.run.dirName)
    }

    Path getFileViewByPidDirectory(RawSequenceFile rawSequenceFile, WellDirectory wellDirectory = null, PathOption... options) {
        if (options.contains(PathOption.REAL_PATH) && rawSequenceFile.seqTrack.workflowArtefact.producedBy.workFolder) {
            return getFinalDirPath(rawSequenceFile, options)
        }
        Path basePath = getRunDirectory(rawSequenceFile, wellDirectory)
        // For historic reasons, vbpPath starts and ends with a slash.
        // Remove the slashes here, otherwise it would be interpreted as an absolute path by resolve():
        String vbpPath = Paths.get(rawSequenceFile.fileType.vbpPath).getName(0)
        return basePath.resolve(vbpPath)
    }

    Path getFileViewByPidPathAsPath(RawSequenceFile rawSequenceFile, WellDirectory wellDirectory = null, PathOption... options) {
        Path basePath = getFileViewByPidDirectory(rawSequenceFile, wellDirectory, options)
        return basePath.resolve(rawSequenceFile.vbpFileName)
    }

    /**
     * Similar to {@link java.nio.file.Paths#get(String, String ...)} from Java 7.
     * @deprecated use Path
     */
    @Deprecated
    static File getPath(final String first, final String... more) {
        validatePathSegment(first, "first")
        File file = new File(first)
        for (int i = 0; i < more.length; i++) {
            validatePathSegment(more[i], "more[${i}]")
            file = new File(file, more[i])
        }
        return file
    }

    @Deprecated
    private static void validatePathSegment(final String segment, final String segmentPosition) {
        if (!segment) {
            throw new IllegalArgumentException("${segmentPosition} is blank")
        }
        if (segment =~ /(?:^|${Pattern.quote(File.separator)})\.{1,2}(?:${Pattern.quote(File.separator)}|$)/) {
            throw new IllegalArgumentException("${segmentPosition} contains '.' or '..': ${segment}")
        }
        if (!(segment ==~ OtpPathValidator.PATH_CHARACTERS_REGEX)) {
            throw new IllegalArgumentException("${segmentPosition} contains at least one illegal character: ${segment}")
        }
    }

    /**
     * This function return path to the initial location
     * of the given sequenceFile
     * @deprecated use {@link #getFileInitialPathAsPath}
     */
    @Deprecated
    static String getFileInitialPath(RawSequenceFile dataFile) {
        return "${dataFile.initialDirectory}/${dataFile.fileName}"
    }

    /**
     * @deprecated use {@link #getFileFinalPathAsPath}
     */
    @Deprecated
    String getFileFinalPath(RawSequenceFile dataFile) {
        return getFileFinalPathAsPath(dataFile)?.toString()
    }

    /**
     * @deprecated use {@link #getFileViewByPidPathAsPath}
     */
    @Deprecated
    String getFileViewByPidPath(long fileId) {
        RawSequenceFile file = RawSequenceFile.get(fileId)
        if (!file) {
            return null
        }
        return getFileViewByPidPath(file)
    }

    /**
     * @deprecated use {@link #getFileViewByPidPathAsPath}
     */
    @Deprecated
    String getFileViewByPidPath(RawSequenceFile file) {
        return getFileViewByPidPathAsPath(file)
    }
    /**
     * for single cell data with well identifier, the path in the all directory is returned.
     * For all other data the same as {@link #getFileViewByPidPath} is returned
     * @deprecated use {@link #getFileViewByPidPathAsPath(RawSequenceFile, WellDirectory#ALL_WELL)}
     */
    @Deprecated
    String getWellAllFileViewByPidPath(RawSequenceFile file) {
        return getFileViewByPidPathAsPath(file, WellDirectory.ALL_WELL)
    }

    /**
     * @deprecated use {@link FileService#ensureFileIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureFileIsReadableAndNotEmpty(final File file) {
        FileService.ensureFileIsReadableAndNotEmptyStatic(file.toPath())
    }

    /**
     * @deprecated use {@link FileService#ensureDirIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureDirIsReadableAndNotEmpty(final File dir) {
        FileService.ensureDirIsReadableAndNotEmptyStatic(dir.toPath())
    }

    /**
     * @deprecated use {@link FileService#ensureDirIsReadable}
     */
    @Deprecated
    static void ensureDirIsReadable(final File dir) {
        FileService.ensureDirIsReadableStatic(dir.toPath())
    }

    /**
     * @deprecated use {@link FileService#deleteDirectoryRecursively(Path)}
     */
    @Deprecated
    void deleteFilesRecursive(Collection<File> filesOrDirectories) {
        assert filesOrDirectories != null: 'filesOrDirectories may not be null'
        if (filesOrDirectories.empty) {
            return // nothing to do
        }
        String cmd = createClusterScriptService.removeDirs(filesOrDirectories, CreateClusterScriptService.RemoveOption.RECURSIVE_FORCE)
        assert remoteShellHelper.executeCommand(cmd) ==~ /^0\s*$/
        filesOrDirectories.each {
            waitUntilDoesNotExist(it)
        }
    }
}

enum WellDirectory {
    ALL_WELL,
}

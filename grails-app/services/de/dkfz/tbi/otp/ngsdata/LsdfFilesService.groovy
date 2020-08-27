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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.waitUntilDoesNotExist
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog

@Transactional
class LsdfFilesService {

    static final String SINGLE_CELL_ALL_WELL = '0_all'

    @Autowired
    RemoteShellHelper remoteShellHelper
    CreateClusterScriptService createClusterScriptService

    FileService fileService

    FileSystemService fileSystemService

    /**
     * Similar to {@link java.nio.file.Paths#get(String, String ...)} from Java 7.
     */
    static File getPath(final String first, final String... more) {
        validatePathSegment(first, "first")
        File file = new File(first)
        for (int i = 0; i < more.length; i++) {
            validatePathSegment(more[i], "more[${i}]")
            file = new File(file, more[i])
        }
        return file
    }

    private static void validatePathSegment(final String segment, final String segmentPosition) {
        if (!segment) {
            throw new IllegalArgumentException("${segmentPosition} is blank")
        }
        if (segment =~ /(?:^|${Pattern.quote(File.separator)})\.{1,2}(?:${Pattern.quote(File.separator)}|$)/) {
            throw new IllegalArgumentException("${segmentPosition} contains '.' or '..': ${segment}")
        }
        if (!(segment ==~ OtpPath.PATH_CHARACTERS_REGEX)) {
            throw new IllegalArgumentException("${segmentPosition} contains at least one illegal character: ${segment}")
        }
    }

    /**
     * This function return path to the initial location
     * of the given dataFile
     */
    static String getFileInitialPath(DataFile dataFile) {
        return "${dataFile.initialDirectory}/${dataFile.fileName}"
    }

    Path getFileInitialPathAsPath(DataFile dataFile, FileSystem fileSystem) {
        return fileSystem.getPath(getFileInitialPath(dataFile))
    }


    /**
     * Important function.
     * This function knows all naming conventions and data organization
     *
     * @param file
     * @return String with path or null if path can not be established
     */
    String getFileFinalPath(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String seqTypeDir = seqTypeDirectory(file)
        if (seqTypeDir == null) {
            return null
        }
        String centerDir = file.run.seqCenter.dirName
        String basePath = file.project.projectSequencingDirectory
        String path = "${basePath}/${seqTypeDir}/${centerDir}/${file.run.dirName}/${file.pathName}/${file?.fileName}"
        return path
    }

    Path getFileFinalPathAsPath(DataFile file, FileSystem fileSystem) {
        return fileSystem.getPath(getFileFinalPath(file))
    }

    boolean checkFinalPathDefined(DataFile dataFile) {
        if (!dataFile) {
            return false
        }
        return dataFile.used
    }

    String seqTypeDirectory(DataFile file) {
        if (file.seqTrack) {
            return file.seqTrack.seqType?.dirName
        }
        if (file.alignmentLog) {
            return file.alignmentLog.seqTrack.seqType?.dirName
        }
        return null
    }

    private String combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(DataFile dataFile, boolean useAllWellDirectory = false) {
        return combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(dataFile.seqTrack ?: dataFile.alignmentLog.seqTrack, useAllWellDirectory)
    }

    private String combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(SeqTrack seqTrack, boolean useAllWellDirectory = false) {
        StringBuilder sb = new StringBuilder()
        sb << seqTrack.sample.sampleType.dirName
        if (seqTrack.seqType.hasAntibodyTarget) {
            AntibodyTarget antibodyTarget = seqTrack.antibodyTarget
            String antibodyDirNamePart = antibodyTarget.name
            sb << "-${antibodyDirNamePart}"
        }
        if (seqTrack.singleCellWellLabel && seqTrack.seqType.singleCell) {
            sb << "/"
            sb << (useAllWellDirectory ? SINGLE_CELL_ALL_WELL : seqTrack.singleCellWellLabel)
        }
        return sb.toString()
    }

    String getFileViewByPidPath(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return getFileViewByPidPath(file)
    }

    String getFileViewByPidPath(DataFile file) {
        return createFinalPathHelper(file, false)
    }

    Path getFileViewByPidPathAsPath(DataFile file, FileSystem fileSystem) {
        return fileSystem.getPath(getFileViewByPidPath(file))
    }

    /**
     * for single cell data with well identifier, the path in the all directory is returned.
     * For all other data the same as {@link #getFileViewByPidPath} is returned
     */
    String getWellAllFileViewByPidPath(DataFile file) {
        return createFinalPathHelper(file, true)
    }

    Path getWellAllFileViewByPidPathAsPath(DataFile file, FileSystem fileSystem) {
        return fileSystem.getPath(getWellAllFileViewByPidPath(file))
    }

    private OtpPath createViewByPidPath(DataFile dataFile) {
        return dataFile.individual.getViewByPidPath(dataFile.seqType)
    }

    String createSingleCellAllWellDirectoryPath(DataFile file) {
        OtpPath vbpPath = createViewByPidPath(file)
        String sampleTypeDir = combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(file, true)
        return new OtpPath(vbpPath, sampleTypeDir).absoluteDataManagementPath.path
    }

    private String createFinalPathHelper(DataFile file, boolean useAllWellDirectory = false) {
        OtpPath vbpPath = createViewByPidPath(file)
        return new OtpPath(
                vbpPath,
                getFilePathInViewByPid(file, useAllWellDirectory)
        ).absoluteDataManagementPath.path
    }

    String getFilePathInViewByPid(DataFile file, boolean useAllWellDirectory = false) {
        String sampleTypeDir = combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(file, useAllWellDirectory)

        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        return Paths.get(
                sampleTypeDir,
                seqTrack.seqType.libraryLayoutDirName,
                "run${seqTrack.run.name}",
                file.fileType.vbpPath,
                file.vbpFileName
        )
    }

    File getFileViewByPidDirectory(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<File> paths = files.collect { DataFile file ->
            new File(getFileViewByPidPath(file)).parentFile
        }
        return CollectionUtils.exactlyOneElement(paths.unique()).parentFile
    }

    @Deprecated
    static void ensureFileIsReadableAndNotEmpty(final File file) {
        FileService.ensureFileIsReadableAndNotEmpty(file.toPath())
    }

    @Deprecated
    static void ensureDirIsReadableAndNotEmpty(final File dir) {
        FileService.ensureDirIsReadableAndNotEmpty(dir.toPath())
    }

    @Deprecated
    static void ensureDirIsReadable(final File dir) {
        FileService.ensureDirIsReadable(dir.toPath())
    }

    /**
     * Deletes the specified file. Throws an exception if the path is not absolute, if the file does not exist, if the
     * path points to something different than a file, or if the deletion fails.
     */
    void deleteFile(final Realm realm, final File file) {
        assert file.isAbsolute() && file.exists() && file.isFile()
        try {
            assert remoteShellHelper.executeCommand(realm, "rm '${file}'; echo \$?") ==~ /^0\s*$/
            waitUntilDoesNotExist(file)
        } catch (final Throwable e) {
            throw new RuntimeException("Could not delete file ${file}.", e)
        }
        threadLog.info "Deleted file ${file}"
    }

    /**
     * Deletes the specified empty directory. Throws an exception if the path is not absolute, if the directory does not
     * exist, if the path points to something different than a directory, or if the deletion fails.
     */
    void deleteDirectory(final Realm realm, final File directory) {
        assert directory.isAbsolute() && directory.exists() && directory.isDirectory()
        try {
            assert remoteShellHelper.executeCommand(realm, "rmdir '${directory}'; echo \$?") ==~ /^0\s*$/
            waitUntilDoesNotExist(directory)
        } catch (final Throwable e) {
            throw new RuntimeException("Could not delete directory ${directory}.", e)
        }
        threadLog.info "Deleted directory ${directory}"
    }

    String[] getAllPathsForRun(Run run, boolean fullPath = false) {
        assert run: "No run given"

        Set<String> paths = [] as Set
        DataFile.findAllByRun(run).each { DataFile file ->
            String path = getPathToRun(file, fullPath)
            if (path) {
                paths << path
            } else {
                paths << file.initialDirectory
            }
        }
        return (String[]) paths.toArray()
    }


    private String getPathToRun(DataFile file, boolean fullPath = false) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = file.project.projectSequencingDirectory
        String seqTypeDir = seqTypeDirectory(file)
        String centerDir = file.run.seqCenter.dirName
        String path = "${basePath}/${seqTypeDir}/${centerDir}/"
        if (fullPath) {
            String runName = file.run.name
            String pathWithRunName = "${path}run${runName}"
            return pathWithRunName
        }
        return path
    }

    void deleteFilesRecursive(Realm realm, Collection<File> filesOrDirectories) {
        assert realm: 'realm may not be null'
        assert filesOrDirectories != null: 'filesOrDirectories may not be null'
        if (filesOrDirectories.empty) {
            return //nothing to do
        }
        String cmd = createClusterScriptService.removeDirs(filesOrDirectories, CreateClusterScriptService.RemoveOption.RECURSIVE_FORCE)
        assert remoteShellHelper.executeCommand(realm, cmd) ==~ /^0\s*$/
        filesOrDirectories.each {
            waitUntilDoesNotExist(it)
        }
    }
}

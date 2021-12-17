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

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator

import java.nio.file.*
import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.waitUntilDoesNotExist

@Transactional
class LsdfFilesService {

    static final String SINGLE_CELL_ALL_WELL = '0_all'

    @Autowired
    RemoteShellHelper remoteShellHelper
    CreateClusterScriptService createClusterScriptService

    FileService fileService

    FileSystemService fileSystemService

    IndividualService individualService
    ProjectService projectService

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
     * of the given dataFile
     * @deprecated use {@link #getFileInitialPathAsPath}
     */
    @Deprecated
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
     * @return String with path or null if path can not be established
     */
    Path getFileFinalPathAsPath(DataFile dataFile) {
        if (!checkFinalPathDefined(dataFile)) {
            return null
        }
        String seqTypeDir = seqTypeDirectory(dataFile)
        if (seqTypeDir == null) {
            return null
        }
        String centerDir = dataFile.run.seqCenter.dirName
        Path basePath = projectService.getSequencingDirectory(dataFile.project)
        return basePath.resolve(seqTypeDir).resolve(centerDir).resolve(dataFile.run.dirName).resolve(dataFile.pathName).resolve(dataFile?.fileName)
    }

    /**
     * @deprecated use {@link #getFileFinalPathAsPath}
     */
    @Deprecated
    String getFileFinalPath(DataFile dataFile) {
        return getFileFinalPathAsPath(dataFile)?.toString()
    }

    boolean checkFinalPathDefined(DataFile dataFile) {
        if (!dataFile) {
            return false
        }
        return dataFile.used
    }

    @Deprecated
    String getFileMd5sumFinalPath(DataFile dataFile) {
        return getFileMd5sumFinalPathAsPath(dataFile)?.toString()
    }

    Path getFileMd5sumFinalPathAsPath(DataFile dataFile) {
        return getFileFinalPathAsPath(dataFile)?.resolveSibling(dataFile.fileName.concat(".md5sum"))
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

    /**
     * @deprecated use {@link #getFileViewByPidPathAsPath}
     */
    @Deprecated
    String getFileViewByPidPath(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return getFileViewByPidPath(file)
    }

    /**
     * @deprecated use {@link #getFileViewByPidPathAsPath}
     */
    @Deprecated
    String getFileViewByPidPath(DataFile file) {
        return getFileViewByPidPathAsPath(file)
    }

    Path getFileViewByPidPathAsPath(DataFile file) {
        return createFinalPathHelper(file, false)
    }

    /**
     * for single cell data with well identifier, the path in the all directory is returned.
     * For all other data the same as {@link #getFileViewByPidPath} is returned
     * @deprecated use {@link #getWellAllFileViewByPidPathAsPath}
     */
    @Deprecated
    String getWellAllFileViewByPidPath(DataFile file) {
        return getWellAllFileViewByPidPathAsPath(file)
    }

    Path getWellAllFileViewByPidPathAsPath(DataFile file) {
        return createFinalPathHelper(file, true)
    }

    private Path createViewByPidPath(DataFile dataFile) {
        return individualService.getViewByPidPath(dataFile.individual, dataFile.seqType)
    }

    Path createSingleCellAllWellDirectoryPath(DataFile file) {
        Path vbpPath = createViewByPidPath(file)
        String sampleTypeDir = combinedDirectoryNameForSampleTypePlusAntibodyPlusSingleCellWell(file, true)
        return vbpPath.resolve(sampleTypeDir)
    }

    private Path createFinalPathHelper(DataFile file, boolean useAllWellDirectory = false) {
        Path vbpPath = createViewByPidPath(file)
        return vbpPath.resolve(getFilePathInViewByPid(file, useAllWellDirectory))
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

    Path getFileViewByPidDirectory(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<Path> paths = files.collect { DataFile file ->
            getFileViewByPidPathAsPath(file).parent
        }
        return CollectionUtils.exactlyOneElement(paths.unique()).parent
    }

    /**
     * @deprecated use {@link FileService#ensureFileIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureFileIsReadableAndNotEmpty(final File file) {
        FileService.ensureFileIsReadableAndNotEmpty(file.toPath())
    }

    /**
     * @deprecated use {@link FileService#ensureDirIsReadableAndNotEmpty}
     */
    @Deprecated
    static void ensureDirIsReadableAndNotEmpty(final File dir) {
        FileService.ensureDirIsReadableAndNotEmpty(dir.toPath())
    }

    /**
     * @deprecated use {@link FileService#ensureDirIsReadable}
     */
    @Deprecated
    static void ensureDirIsReadable(final File dir) {
        FileService.ensureDirIsReadable(dir.toPath())
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

    private Path getPathToRun(DataFile file, boolean fullPath = false) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        Path basePath = projectService.getSequencingDirectory(file.project)
        String seqTypeDir = seqTypeDirectory(file)
        String centerDir = file.run.seqCenter.dirName
        Path path = basePath.resolve(seqTypeDir).resolve(centerDir)
        if (fullPath) {
            String runName = file.run.name
            Path pathWithRunName = path.resolve("run${runName}")
            return pathWithRunName
        }
        return path
    }

    /**
     * @deprecated use {@link FileService#deleteDirectoryRecursively(Path)}
     */
    @Deprecated
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

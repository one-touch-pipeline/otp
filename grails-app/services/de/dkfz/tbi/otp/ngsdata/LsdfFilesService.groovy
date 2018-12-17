package de.dkfz.tbi.otp.ngsdata

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.RemoteShellHelper
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.util.regex.Pattern

import static de.dkfz.tbi.otp.utils.WaitingFileUtils.waitUntilDoesNotExist
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog

class LsdfFilesService {
    @Autowired
    RemoteShellHelper remoteShellHelper
    CreateClusterScriptService createClusterScriptService


    /**
     * Similar to {@link java.nio.file.Paths#get(String, String...)} from Java 7.
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

    boolean checkFinalPathDefined(DataFile dataFile) {
        if (!dataFile) {
            return false
        }
        if (!dataFile.used) {
            return false
        }
        return true
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

    String getFileViewByPidPath(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return getFileViewByPidPath(file)
    }

    /**
     * Important function.
     * This function knows view-by-pid data organization schema
     * If the view by bid path do not apply, the function returns null
     *
     * @param DataFile
     * @return path to view by pid file, or null if vbp does not apply
     */
    String getFileViewByPidPath(DataFile file, Sequence sequence = null) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = file.project.projectSequencingDirectory.path
        String relativePath = getFileViewByPidRelativePath(file, sequence)
        return "${basePath}/${relativePath}"
    }

    File getFileViewByPidDirectory(SeqTrack seqTrack) {
        List<DataFile> files = DataFile.findAllBySeqTrack(seqTrack)
        List<File> paths = files.collect() { DataFile file ->
            new File(getFileViewByPidPath(file)).parentFile
        }
        return CollectionUtils.exactlyOneElement(paths.unique()).parentFile
    }

    String getFileViewByPidRelativePath(DataFile file, Sequence sequence = null) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String directory = sequence ? getFileViewByPidRelativeDirectory(file, sequence) : getFileViewByPidRelativeDirectory(file)
        return "${directory}/${file.vbpFileName}"
    }


    private String getFileViewByPidRelativeDirectory(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        String seqTypeDir = seqTrack.seqType.dirName
        String pid = seqTrack.sample.individual.pid
        String library = seqTrack.seqType.libraryLayoutDirName
        String sampleTypeDir = seqTrack.sample.sampleType.dirName
        if (seqTrack instanceof ChipSeqSeqTrack) {
            ChipSeqSeqTrack chipSeqSeqTrack = seqTrack as ChipSeqSeqTrack
            AntibodyTarget antibodyTarget = chipSeqSeqTrack.antibodyTarget
            String antibodyDirNamePart = antibodyTarget.name
            sampleTypeDir += "-${antibodyDirNamePart}"
        }
        return getFileViewByPidRelativeDirectory(seqTypeDir, pid, sampleTypeDir, library, file.run.name, file.fileType.vbpPath)
    }

    private String getFileViewByPidRelativeDirectory(DataFile file, Sequence sequence) {
        return getFileViewByPidRelativeDirectory(sequence.dirName,
        sequence.pid,
        sequence.sampleTypeName.toLowerCase(),
        sequence.libraryLayoutDirName,
        sequence.name,
        file.fileType.vbpPath)
    }

    private String getFileViewByPidRelativeDirectory(String seqTypeDir, String pid, String sampleType, String library, String runName, String vbpPath) {
        return "${seqTypeDir}/view-by-pid/${pid}/${sampleType}/${library}/run${runName}/${vbpPath}"
    }

    @Deprecated
    static boolean isFileReadableAndNotEmpty(final File file) {
        FileService.isFileReadableAndNotEmpty(file.toPath())
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
            } else  {
                paths << file.initialDirectory
            }
        }
        return (String[])paths.toArray()
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

    void createDirectory(File dir, Realm realm) {
        String cmd = createClusterScriptService.makeDirs([dir], "2770")
        assert remoteShellHelper.executeCommand(realm, cmd) ==~ /^0\s*$/
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

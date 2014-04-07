package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ExecutionService
import org.codehaus.groovy.grails.commons.GrailsApplication

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getLog

class LsdfFilesService {

    GrailsApplication grailsApplication
    ConfigService configService
    ExecutionService executionService

    /**
     * Similar to {@link Paths.get(String, String...)} from Java 7.
     */
    public static File getPath(final String first, final String... more) {
        File file = new File(first)
        for (final String part : more) {
            file = new File(file, part)
        }
        return file
    }

    /**
     * This function return path to the initial location
     * of the given dataFile
     */
    public String getFileInitialPath(DataFile dataFile) {
        Run run = dataFile.run
        //String prefix = (run.legacyRun) ? "run" : ""
        String initialPath = dataFile.runSegment.dataPath
        return "${initialPath}/${run.name}/${dataFile.pathName}/${dataFile.fileName}"
    }

    /**
     * This function returns an array of strings to final locations of this runs.
     * Only data files belonging to a given project are considered, because
     * projects are often processed separately.
     *
     * @param run to be processed
     * @param projectName only data files belonging to a given project are used
     * @return
     */
    String[] getListOfRunDirecotries(Run run, String projectName) {
        Set<String> paths = new HashSet<String>()
        DataFile.findAllByRun(run).each {DataFile dataFile ->
            if (dataFile.project == null) {
                // data files without projects eg. metadata file
                return
            }
            if (dataFile.project.name.contains(projectName)) {
                String path = getPathToRun(dataFile)
                if (path != null) {
                    String fullPath = path + "/run" + run.name
                    paths << fullPath
                }
            }
        }
        return (String[])paths.toArray()
    }

    String getFileFinalPath(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return getFileFinalPath(file)
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
        String basePath = configService.getProjectSequencePath(file.project)
        String path =
            "${basePath}/${seqTypeDir}/${centerDir}/run${file.run.name}/${file.pathName}/${file?.fileName}"
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

    private String seqTypeDirectory(DataFile file) {
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
        String basePath = configService.getProjectSequencePath(file.project)
        String relativePath = getFileViewByPidRelativePath(file, sequence)
        return "${basePath}/${relativePath}"
    }

    String getFileViewByPidDirectory(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = configService.getProjectSequencePath(file.project)
        String relativePath = getFileViewByPidRelativeDirectory(file)
        return "${basePath}/${relativePath}"
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
        String library = seqTrack.seqType.libraryLayout.toLowerCase()
        String sampleTypeDir = seqTrack.sample.sampleType.name.toLowerCase()
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
            sequence.libraryLayout.toLowerCase(),
            sequence.name,
            file.fileType.vbpPath)
    }

    private String getFileViewByPidRelativeDirectory(String seqTypeDir, String pid, String sampleType, String library, String runName, String vbpPath) {
        return "${seqTypeDir}/view-by-pid/${pid}/${sampleType}/${library}/run${runName}/${vbpPath}"
    }

    boolean fileExists(String path) {
        File file = new File(path)
        return file.canRead()
    }

    long fileSize(String path) {
        File file = new File(path)
        if (file.isDirectory()) {
            return 0L
        }
        return file.length()
    }

    /**
     *
     * @param path
     * @return
     */
    Date fileCreationDate(String path) {
        if (!path) {
            return null
        }
        File file = new File(path)
        long timestamp = file.lastModified()
        if (timestamp == 0L) {
            return null
        }
        return new Date(timestamp)
    }

    /**
     * Deletes the specified file. Throws an exception if the path is not absolute, if the file does not exist, if the
     * path points to something different than a file, or if the deletion fails.
     */
    void deleteFile(final Realm realm, final File file) {
        assert file.isAbsolute() && file.exists() && file.isFile()
        try {
            assert executionService.executeCommand(realm, "rm '${file}'; echo \$?") == "0"
        } catch (final Throwable e) {
            throw new RuntimeException("Could not delete file ${file}.", e)
        }
        assert !file.exists()
        log.info "Deleted file ${file}"
    }

    /**
     * Deletes the specified empty directory. Throws an exception if the path is not absolute, if the directory does not
     * exist, if the path points to something different than a directory, or if the deletion fails.
     */
    void deleteDirectory(final Realm realm, final File directory) {
        assert directory.isAbsolute() && directory.exists() && directory.isDirectory()
        try {
            assert executionService.executeCommand(realm, "rmdir '${directory}'; echo \$?") == "0"
        } catch (final Throwable e) {
            throw new RuntimeException("Could not delete directory ${directory}.", e)
        }
        assert !directory.exists()
        log.info "Deleted directory ${directory}"
    }

    String[] getAllPathsForRun(long runId) {
        Run run = Run.getAt(runId)
        if (!run) {
            return null
        }
        return getAllPathsForRun(run)
    }

    String[] getAllPathsForRun(Run run) {
        if (!run) {
            //exception
            return null
        }
        Set<String> paths = new HashSet<String>()
        DataFile.findAllByRun(run).each { DataFile file ->
            String path = getPathToRun(file)
            if (path) {
                paths << path
            } else  {
                paths << file.runSegment.dataPath
            }
        }
        return (String[])paths.toArray()
    }

    private String getPathToRun(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = configService.getProjectSequencePath(file.project)
        String seqTypeDir = seqTypeDirectory(file)
        String centerDir = file.run.seqCenter.dirName
        String path = "${basePath}/${seqTypeDir}/${centerDir}/"
        return path
    }
}

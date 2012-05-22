package de.dkfz.tbi.otp.ngsdata

class LsdfFilesService {

    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    def configService

    /**
     * This function return path to the initial location
     * of the given dataFile
     */
    public String getFileInitialPath(DataFile dataFile) {
        Run run = dataFile.run
        String prefix = (run.legacyRun) ? "run" : ""
        String initialPath = dataFile.runInitialPath.dataPath
        return "${initialPath}/${prefix}${run.name}/${dataFile.pathName}/${dataFile.fileName}"
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

    /**
     *
     * @param fileId
     * @return
     */
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
        String seqTypeDir = seqTypeDirectory(file);
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
            return false;
        }
        if (!dataFile.used) {
            return false; 
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

    /**
     *
     * @param fileId
     * @return
     */
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
    String getFileViewByPidPath(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = configService.getProjectSequencePath(file.project)
        String relativePath = getFileViewByPidRelativePath(file)
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

    String getFileViewByPidRelativePath(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String directory = getFileViewByPidRelativeDirectory(file)
        return "${directory}/${file.vbpFileName}"
    }


    private String getFileViewByPidRelativeDirectory(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        String seqTypeDir = seqTrack.seqType.dirName
        String pid = seqTrack.sample.individual.pid
        String sampleType = seqTrack.sample.type.toString().toLowerCase()
        String library = seqTrack.seqType.libraryLayout.toLowerCase()
        String path =
            "${seqTypeDir}/view-by-pid/${pid}/${sampleType}/${library}/run${file.run.name}/${file.fileType.vbpPath}"
        return path
    }

    /**
     *
     *
     * @param path
     * @return
     */
    boolean fileExists(String path) {
        File file = new File(path)
        return file.canRead()
    }

    /**
     *
     * @param path
     * @return
     */
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
     *
     *
     */
    String[] getAllPathsForRun(long runId) {
        Run run = Run.getAt(runId)
        if (!run) {
            return null
        }
        return getAllPathsForRun(run)
    }

    /**
     *
     *
     */
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
                paths << file.runInitialPath.dataPath
            }
        }
        return (String[])paths.toArray()
    }

    /**
     *
     *
     */
    private String getPathToRun(DataFile file) {
        if (!checkFinalPathDefined(file)) {
            return null
        }
        String basePath = configService.getProjectSequencePath(file.project)
        String seqTypeDir = seqTypeDirectory(file);
        String centerDir = file.run.seqCenter.dirName
        String path = "${basePath}/${seqTypeDir}/${centerDir}/"
        return path
    }
}

package de.dkfz.tbi.otp.ngsdata

//import java.io.*


class LsdfFilesService {
    // will go somewhere
    // TODO: these constants should not be here!
    //private static final String basePath = "$ROOT_PATH/project/"
    //private static final String metaDataPath = "~/ngs-isgc/"

    /**
     * This function return path to the initial location
     * of the given dataFile
     */
    public String getFileInitialPath(DataFile dataFile) {
        Run run = dataFile.run
        return run.dataPath() + "/" + run.name() + "/" +
            dataFile.pathName() + "/" + dataFile.fileName
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
    String[] getListOfRunDirecotries(run, projectName) {

        DataFile dataFiles = run.dataFiles
        Set<String> paths = new HashSet<String>()
        dataFiles.each {DataFile dataFile ->
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
        DataFile file = DataFile.getAt(fileId)
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
        if (!file) {
            log.debug("null file object")
            return null
        }
        if (!file.used) {
            log.debug("File not used in seqTrack, location undefined [${file}]")
            return null
        }
        if (file.fileType.type == FileType.Type.METADATA) {
            String path = otp.datPath['metadata'] + "/data-tracking-orig/" +
                    file.run.seqCenter?.dirName +
                    "/run" + file.run.name + file.fileName
            return path
        }
        String seqTypeDir = "";
        if (file.seqTrack) {
            seqTypeDir = file.seqTrack.seqType?.dirName
        } else if (file.alignmentLog) {
            seqTypeDir = file.alignmentLog.seqTrack.seqType?.dirName
        }
        String basePath = otp.dataPath[host]
        if (!basePath) {
            return null
        }
        String path =
                basePath + file?.project?.dirName + "/sequencing/" +
                seqTypeDir + "/" + file.run.seqCenter?.dirName +
                "/run" + file.run?.name + "/" + file?.prvFilePath + "/" +
                file?.fileName
        return path
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
        if (!file) {
            return null
        }
        if (file.fileType.type == FileType.Type.METADATA) {
            return null
        }
        if (!file.used) {
            log.debug("File not used ${file.fileName}")
            return null
        }
        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        if (!seqTrack) {
            log.debug("File used but no SeqTrack ${file.fileName}")
            return null
        }
        String basePath = otp.dataPath[host]
        if (!basePath) {
             return null
        }
        String pid = seqTrack.sample.individual.pid
        String path =
                basePath + file.project?.dirName + "/sequencing/" +
                seqTrack.seqType.dirName +  "/view-by-pid/" + pid +
                "/" + seqTrack.sample.type.toString().toLowerCase() + "/" +
                seqTrack.seqType.libraryLayout.toLowerCase() +
                "/run" + file.run.name + "/" + file.fileType.vbpPath + "/" +
                file.vbpFileName
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
     * @param file
     * @return
     */
    /*
    boolean fileExists(DataFile file) {
        String path = getFilePath(file)
        if (!path) {
            return false
        }
        return fileExists(path)
    }
    */
    /**
     *
     * @param fileId
     * @return
     */
    /*
    boolean fileExists(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return false
        }
        return fileExists(file)
    }
    */
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
     * @param file
     * @return
     */
    /*
    long fileSize(DataFile file) {
        String path = getFilePath(file)
        if (!path) {
            return 0
        }
        return fileSize(path)
    }
    */
    /**
     *
     * @param fileId
     * @return
     */
    /*
    long fileSize(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return 0
        }
        return fileSize(file)
    }
    */
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
     * @param file
     * @return
     */
    /*
    Date fileCreationDate(DataFile file) {
        if (!file) {
            return null
        }
        String path = getFilePath(file)
        if (!path) {
            return null
        }
        return fileCreationDate(path)
    }
    */
    /**
     *
     * @param fileId
     * @return
     */
    /*
    Date fileCreationDate(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return fileCreationDate(file)
    }
    */
    /**
     *
     * @param runId
     * @return
     */
    /*
    boolean runFolderInFinalLocation(long runId) {
        Run run = Run.get(runId)
        if (!run) {
            return false
        }
        return runInFinalLocation(run)
    }
    */
    /**
     *
     * @param run
     * @return
     */
    /*
    boolean runFolderInFinalLocation(Run run) {
        if (!run) {
            // will be exception
            return false
        }
        String [] paths = getAllPathsForRun(run)
        run.finalLocation = false
        run.save(flush: true)
        boolean locationMissing = false
        paths.each { String path ->
            log.debug(path)
            File file = new File(path + "/run" + run.name)
            if (!file.isDirectory() || !file.canRead()) {
                locationMissing = true
            }
        }
        if (locationMissing) {
            return false
        }
        run.finalLocation = true
        run.save(flush: true)
        return true
    }
    */
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
            if (file.fileType.type == FileType.Type.METADATA) {
                return
            }
            String path = getPathToRun(file)
            if (!path) {
                paths << run.dataPath
            }
            paths << path
        }
        return (String[])paths.toArray()
    }

    /**
     *
     *
     */
    private String getPathToRun(DataFile file) {
        if (!file) {
            return null
        }
        if (file.fileType.type == FileType.Type.METADATA) {
            return null
        }
        if (!file.used) {
            return null
        }
        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        if (!seqTrack) {
            return null
        }
        String basePath = otp.dataPath[file.project.host]
        String path =
                basePath + file?.project?.dirName + "/sequencing/" +
                seqTrack.seqType.dirName + "/" + file.run.seqCenter?.dirName + "/"
        return path
    }
}

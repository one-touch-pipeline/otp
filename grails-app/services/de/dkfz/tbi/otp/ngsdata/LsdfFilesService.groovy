package de.dkfz.tbi.otp.ngsdata

//import java.io.*


class LsdfFilesService {
    // will go somewhere
    // TODO: these constants should not be here!
    private static final String basePath = "$ROOT_PATH/project/"
    private static final String metaDataPath = "~/ngs-isgc/"

    /**
     * This function checks if all dataFiles belonging to this 
     * run are in the final location and properly linked.
     *
     * If run does not exists the RunTimeException with be risen.
     * @param runId
     */
    void checkAllFiles(long runId) {
        Run run = Run.get(runId)
        if (!run) {
            log.debug("Run ${runID} not found")
            //return
        }
        run.dataFiles.each { DataFile dataFile ->
            boolean exists = fileExists(dataFile)
            if (!exists) {
                log.debug("file ${dataFile.fileName} does not exist")
                // continue
                return
            }
            dataFile.fileExists = true
            dataFile.fileSize = fileSize(dataFile)
            dataFile.dateFileSystem = fileCreationDate(dataFile)
            String vbpPath = getViewByPidPath(dataFile)
            if (vbpPath) {
                dataFile.fileLinked = fileExists(vbpPath)
            }
        }
        run.save(flush: true)
    }

    /**
     *
     * @param fileId
     * @return
     */
    String getFilePath(long fileId) {
        DataFile file = DateFile.getAt(fileId)
        if (!file) {
            return null
        }
        return getFilePath(file)
    }

    /**
     *
     * @param file
     * @return
     */
    String getFilePath(DataFile file) {
        if (!file) {
            log.debug("null file object")
            return null
        }
        if (!file.used) {
            log.debug("File not used in seqTrack, location undefined [${file}]")
            return null
        }
        if (file.fileType.type == FileType.Type.METADATA) {
            String path = metaDataPath + "/data-tracking-orig/" +
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
    String getViewByPidPath(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return getViewByPidPath(file)
    }

    /**
     *
     * @param DataFile
     * @return
     */
    String getViewByPidPath(DataFile file) {
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
    boolean fileExists(DataFile file) {
        String path = getFilePath(file)
        if (!path) {
            return false
        }
        return fileExists(path)
    }

    /**
     *
     * @param fileId
     * @return
     */
    boolean fileExists(long fileId) {
        DataFile file = DataFiele.get(fileId)
        if (!file) {
            return false
        }
        return fileExists(file)
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
     * @param file
     * @return
     */
    long fileSize(DataFile file) {
        String path = getFilePath(file)
        if (!path) {
            return 0
        }
        return fileSize(path)
    }

    /**
     *
     * @param fileId
     * @return
     */
    long fileSize(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return 0
        }
        return fileSize(file)
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
     * @param file
     * @return
     */
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

    /**
     *
     * @param fileId
     * @return
     */
    Date fileCreationDate(long fileId) {
        DataFile file = DataFile.get(fileId)
        if (!file) {
            return null
        }
        return fileCreationDate(file)
    }

    /**
     *
     * @param runId
     * @return
     */
    boolean runInFinalLocation(long runId) {
        Run run = Run.get(runId)
        if (!run) {
            return false
        }
        return runInFinalLocation(run)
    }

    /**
     *
     * @param run
     * @return
     */
    boolean runInFinalLocation(Run run) {
        if (!run) {
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
            return null
        }
        Set<String> paths = new HashSet<String>()
        run.dataFiles.each { DataFile file ->
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
        String path =
                basePath + file?.project?.dirName + "/sequencing/" +
                seqTrack.seqType.dirName + "/" + file.run.seqCenter?.dirName + "/"
        return path
    }

    boolean checkAllRuns(String projectName) {
        Project project = Project.findByName(projectName)
        String dir = basePath + "/" + project.dirName + "/sequencing/"
        File baseDir = new File(dir)
        File[] seqDirs = baseDir.listFiles()
        int nMissing = 0
        for (int i=0; i<seqDirs.size(); i++) {
            File seqTypeDir = seqDirs[i]
            if (!seqTypeDir.isDirectory()) {
                continue
            }
            SeqType seqType = SeqType.findByDirName(seqTypeDir.getName())
            if (!seqType) {
                continue
            }
            File[] seqCenters = seqTypeDir.listFiles()
            for (int j=0; j<seqCenters.size(); j++) {
                SeqCenter center = SeqCenter.findByDirName(seqCenters[j].getName())
                if (!center) {
                    continue
                }
                log.debug("\nChecking ${seqTypeDir} ${seqCenters[j]}")
                File[] runs = seqCenters[j].listFiles()
                for (int iRun=0; iRun<runs.size(); iRun++) {
                    if (!runs[iRun].getName().contains("run")) {
                        continue
                    }
                    log.debug("\t ${runs[iRun].getName()}")
                    String runName = runs[iRun].getName().substring(3)
                    Run run = Run.findByName(runName)
                    if (!run) {
                        nMissing++
                        log.debug("!!! Run ${runName} does not exist !!!")
                    }
                }
            }
        }
        if (nMissing > 0) {
            return false
        }
        return true
    }
}

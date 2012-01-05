package de.dkfz.tbi.otp.ngsdata

class LsdfFilesService {

    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    // will go somewhere
    // TODO: these constants should not be here!src/groovy/de/dkfz/tbi/otp/job/jobs/metaData/MetaDataStartJob.groovy
    //private static final String basePath = "$ROOT_PATH/project/"
    //private static final String metaDataPath = "~/ngs-isgc/"

    /**
     * This function return path to the initial location
     * of the given dataFile
     */
    public String getFileInitialPath(DataFile dataFile) {
        Run run = dataFile.run
        return run.dataPath + "/run" + run.name + "/" +
            dataFile.pathName + "/" + dataFile.fileName
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
        if (!file) {
            log.debug("null file object")
            return null
        }
        if (!file.used) {
            log.debug("File not used in seqTrack, location undefined [${file}]")
            return null
        }
        if (file.fileType.type == FileType.Type.METADATA) {
            String basePath = grailsApplication.config.otp.dataPath['metadata']
            String path = basePath + "/data-tracking-orig/" +
                    file.run.seqCenter?.dirName +
                    "/run" + file.run.name + "/" + file.fileName
            return path
        }
        String seqTypeDir = "";
        if (file.seqTrack) {
            seqTypeDir = file.seqTrack.seqType?.dirName
        } else if (file.alignmentLog) {
            seqTypeDir = file.alignmentLog.seqTrack.seqType?.dirName
        }
        String basePath = grailsApplication.config.otp.dataPath[file.project.host.toLowerCase()]
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
        if (!file.seqTrack) {
            log.debug("File used but no SeqTrack ${file.fileName}")
            return null
        }
        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        String basePath = grailsApplication.config.otp.dataPath[file.project.host.toLowerCase()]
        if (!basePath) {
             return null
        }
        if(!seqTrack.sample.individual.pid) {
            log.debug("No individual pid set.")
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
        String basePath = grailsApplication.config.otp.dataPath[file.project.host.toLowerCase()]
        String path =
                basePath + file?.project?.dirName + "/sequencing/" +
                seqTrack.seqType.dirName + "/" + file.run.seqCenter?.dirName + "/"
        return path
    }
}

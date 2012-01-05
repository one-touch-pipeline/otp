package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

class FilesCompletenessService {

    def lsdfFilesService
    /**
     * Dependency injection of Grails Application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     *
     * This function loops over files from a run, registered in meta-data
     * and check if they exists in the initial location
     *
     * @param run
     */
    public boolean checkInitialSequenceFiles(Run run) {
        boolean allExists = true
        List<DataFile> dataFiles = DataFile.findAllByRun(run)
        if(dataFiles.empty) {
            throw new ProcessingException("No data file provided for the given run.")
        }
        dataFiles.each {DataFile dataFile ->
            if (dataFile.fileType.type == FileType.Type.SEQUENCE ||
            dataFile.fileType.type == FileType.Type.ALIGNMENT) {
                String path = lsdfFilesService.getFileInitialPath(dataFile)
                if (!lsdfFilesService.fileExists(path)) {
                    allExists = false
                    println "missing file ${path}"
                }
            }
        }
        return allExists
    }

    /**
     * This function checks if all files with defined final location 
     * are in the right final location. This function fills statistics
     * fields of DataFile (size and date from the file system)
     * 
     * If the file have final location is decided by the lsdfFilesService
     * 
     * @param run run to be analyzed
     * @return true if all files are in the final location
     */
    boolean checkFinalLocation(Run run) {
        run.finalLocation = false
        run.save(flush: true)
        boolean allExists = true
        List<DataFile> dataFiles = DataFile.findAllByRun(run)
        if(dataFiles.empty) {
            throw new ProcessingException("No data file provided for the given run.")
        }
        dataFiles.each {DataFile dataFile ->
            String path = lsdfFilesService.getFileFinalPath(dataFile.id)
            if (path == null) {
                return // continue
            }
            boolean exists = lsdfFilesService.fileExists(path)
            if (exists) {
                dataFile.fileExists = true
                dataFile.fileSize = lsdfFilesService.fileSize(path)
                dataFile.dateFileSystem = lsdfFilesService.fileCreationDate(path)
            } else {
                dataFile.fileExists = false
                allExists = false
            }
            dataFile.save(flush: true)
        }
        if (allExists) {
            run.finalLocation = true
            run.save(flush: true)
        }
        return allExists
    }

    /**
     * This function checks if all files are linked in the view by pid
     * structure. Only files with determined view by pid path are checked.
     * Location of the view by pid link is decided by lsdfFilesService
     * 
     * @param run
     * @return
     */
    boolean checkViewByPid(Run run) {
        boolean allLinked = true
        List<DataFile> dataFiles = DataFile.findAllByRun(run)
        if(dataFiles.empty) {
            throw new ProcessingException("No data file provided for the given run.")
        }
        dataFiles.each {DataFile dataFile ->
            String path = lsdfFilesService.getFileViewByPidPath(dataFile)
            if (path == null) {
                allLinked = false
                return // continue
            }
            boolean exists = lsdfFilesService.fileExists(path)
            if (exists) {
                dataFile.fileLinked = true
            } else {
                dataFile.fileLinked = false
                allLinked = false
            }
            dataFile.save(flush: true)
        }
        return allLinked
    }

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
            throw new ProcessingException("The handed over runId has no associated Run.")
        }
        List<DataFile> dataFiles = DataFile.findAllByRun(run)
        if(dataFiles.empty) {
            throw new ProcessingException("No data file provided for the given run.")
        }
        dataFiles.each { DataFile dataFile ->
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
    }

    /**
     *
     * @param projectName
     * @param host
     * @return
     */
    boolean checkAllRuns(String projectName, String host) {
        if (!projectName) {
            throw new ProcessingException("No projectName specified.")
        }
        Project project = Project.findByName(projectName)
        if (!project) {
            throw new ProcessingException("No Project could be found for the specified projectName.")
        }
        if (!host) {
            throw new ProcessingException("No host specified.")
        }
        String basePath = grailsApplication.config.otp.dataPath[host]
        if (basePath == "{}") {
            throw new ProcessingException("No base path could be found.")
        }
        String dir = basePath + "/" + project.dirName + "/sequencing/"
        File baseDir = new File(dir)
        File[] seqDirs = baseDir.listFiles()
        int nMissing = 0
        if (!seqDirs) {
            throw new ProcessingException("No sequencing directories could be found.")
        }
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

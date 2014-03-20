package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.job.processing.ProcessingException

class FilesCompletenessService {

    def lsdfFilesService
    def fileTypeService
    def runProcessingService
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
        List<DataFile> dataFiles = runProcessingService.dataFilesForProcessing(run)
        if (!dataFiles) {
            log.debug "No files in processing for run ${run}"
            return false
        }
        for (DataFile file in dataFiles) {
            String path = lsdfFilesService.getFileInitialPath(file)
            if (!lsdfFilesService.fileExists(path)) {
                log.debug "missing file ${path}"
                return false
            }
        }
        return true
    }

    /**
     * This function checks if all files with defined final location
     * are in the right final location. This function fills statistics
     * fields of DataFile (size and date from the file system)
     *
     * If the file have final location is decided by the lsdfFilesService
     *
     * @param run run to be analyzed
     * @return A list of problems that this method finds. The list will be empty if all files are in
     * the final location.
     */
    List<String> validateAllFilesAreInFinalLocation(Run run) {
        // TODO more integration with RunService
        final List<String> problems = []
        def stats = [
            RunSegment.FilesStatus.PROCESSING_CHECKING,
            RunSegment.FilesStatus.PROCESSING_INSTALLATION,
            RunSegment.FilesStatus.FILES_MISSING
        ]
        List<RunSegment> segments = RunSegment.findAllByRunAndFilesStatusInList(run, stats)
        for (RunSegment segment in segments) {
            List<DataFile> dataFiles = DataFile.findAllByRunSegment(segment)
            for (DataFile dataFile in dataFiles) {
                if (!lsdfFilesService.checkFinalPathDefined(dataFile)) {
                    continue
                }
                final String problem = validateFileIsInFinalLocation(dataFile)
                fillFileStatistics(dataFile, problem == null)
                if (problem != null) {
                    problems << problem
                    segment.filesStatus = RunSegment.FilesStatus.FILES_MISSING
                    segment.save(flush: true)
                }
            }
            // go back to processing state in case the state was files_missing
            // and files are now in the right location
            if (problems.empty && segment.filesStatus == RunSegment.FilesStatus.FILES_MISSING) {
                segment.filesStatus = RunSegment.FilesStatus.PROCESSING_INSTALLATION
                segment.save(flush: true)
            }
        }
        return problems
    }

    /**
     * @return <code>null</code> if the file exists, otherwise an error message. (It is still
     * possible that this method throws an exception.)
     */
    private String validateFileIsInFinalLocation(DataFile dataFile) {
        String path = lsdfFilesService.getFileFinalPath(dataFile)
        return lsdfFilesService.fileExists(path) ? null : "${path} is not readable."
    }

    /**
     * Fill statistics from the file system in the DataFile in a database
     * data size and data of file creation are filed only if file exists
     * in the file system
     * @param dataFile
     * @param exists
     */
    private void fillFileStatistics(DataFile dataFile, boolean exists) {
        dataFile.fileExists = exists
        if (exists) {
            String path = lsdfFilesService.getFileFinalPath(dataFile)
            dataFile.fileSize = lsdfFilesService.fileSize(path)
            dataFile.dateFileSystem = lsdfFilesService.fileCreationDate(path)
        }
        dataFile.save(flush: true)
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
        boolean allExists = true
        def stats = [
            RunSegment.FilesStatus.PROCESSING_CHECKING,
            RunSegment.FilesStatus.PROCESSING_INSTALLATION,
            RunSegment.FilesStatus.FILES_MISSING
        ]
        List<RunSegment> segments = RunSegment.findAllByRunAndFilesStatusInList(run, stats)
        for (RunSegment segment in segments) {
            segment.filesStatus = RunSegment.FilesStatus.FILES_CORRECT
            List<DataFile> dataFiles = DataFile.findAllByRunSegment(segment)
            for (DataFile dataFile in dataFiles) {
                String path = lsdfFilesService.getFileViewByPidPath(dataFile)
                if (!path) {
                    continue
                }
                boolean exists = lsdfFilesService.fileExists(path)
                dataFile.fileLinked = exists
                dataFile.save(flush: true)
                if (!exists) {
                    allExists = false
                    segment.filesStatus = RunSegment.FilesStatus.FILES_MISSING
                }
            }
            segment.save(flush: true)
        }
        return allExists
    }

    /**
     * TODO: check if function needed
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
     * TODO: check if function needed then refactor
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

package de.dkfz.tbi.otp.ngsdata

import java.io.*


class LsdfFilesService {

    // will go somewhere
    String basePath = "$ROOT_PATH/project/"
    String metaDataPath = "~/ngs-isgc/"

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param runId
     */
    void checkAllFiles(long runId) {

        Run run = Run.get(runId)
        if (run == null) {
            println "Run ${runID} not found"
            return
        }


        run.dataFiles.each {DataFile dataFile ->

            boolean exists = fileExists(dataFile)
            if (!exists) {
                println "file ${dataFile.fileName} does not exist"
                return
            }

            dataFile.fileExists = true
            dataFile.fileSize = fileSize(dataFile)
            dataFile.dateFileSystem = fileCreationDate(dataFile)

            String vbpPath = getViewByPidPath(dataFile)
            if (vbpPath != null)
                dataFile.fileLinked = fileExists(vbpPath)
        }
        run.save(flush: true)
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param fileId
     * @return
     */
    String getFilePath(long fileId) {

        DataFile file = DateFile.getAt(fileId)
        if (!file) return null
        return getFilePath(file)
    }


    /**
     * 
     * @param file
     * @return
     */
    String getFilePath(DataFile file) {

        if (!file) {
            println "null file object"
            return null
        }

        if (!file.used) {
            println "File not used in seqTrack, location undefined [${file}]"
            return null
        }

        if (file.fileType.type == FileType.Type.METADATA) {

            String path = metaDataPath + "/data-tracking-orig/" +
                file.run.seqCenter?.dirName + 
                "/run" + file.run.name + file.fileName

            return path
        }

        String seqTypeDir = "";
        if (file.seqTrack) 
            seqTypeDir = file.seqTrack.seqType?.dirName
        else if (file.alignmentLog) 
            seqTypeDir = file.alignmentLog.seqTrack.seqType?.dirName

        String path =
            basePath + file?.project?.dirName + "/sequencing/" +
            seqTypeDir + "/" + file.run.seqCenter?.dirName + 
            "/run" + file.run?.name + "/" + file?.prvFilePath + "/" +
            file?.fileName;

        return path
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param fileId
     * @return
     */
    String getViewByPidPath(long fileId) {

        DataFile file = DataFile.get(fileId)
        if (file == null) return null
        return getViewByPidPath(file)
    }

    /**
     * 
     * @param DataFile
     * @return
     */
    String getViewByPidPath(DataFile file) {

        if (file == null) return null
        if (file.fileType.type == FileType.Type.METADATA) return null

        if (!file.used) {
            println "File not used ${file.fileName}"
            return null
        }

        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        if (seqTrack == null) {
            println "File used but no SeqTrack ${file.fileName}"
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

        //println path
        return path
    }
    ////////////////////////////////////////////////////////////////////////////

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
        if (path == null) return false
        return fileExists(path)
    }



    /**
     * 
     * @param fileId
     * @return
     */
    boolean fileExists(long fileId) {

        DataFile file = DataFiele.get(fileId)
        if (file == null) return false
        return fileExists(file)
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param path
     * @return
     */
    long fileSize(String path) {

        File file = new File(path)
        if (file.isDirectory()) return 0L
        return file.length()
    }



    /**
     * 
     * @param file
     * @return
     */
    long fileSize(DataFile file) {

        String path = getFilePath(file)
        if (path == null) return 0
        return fileSize(path)
    }



    /**
     * 
     * @param fileId
     * @return
     */
    long fileSize(long fileId) {

        DataFile file = DataFile.get(fileId)
        if (file == null) return 0
        return fileSize(file)
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param path
     * @return
     */
    Date fileCreationDate(String path) {

        if (path == null) return null

        File file = new File(path)
        long timestamp = file.lastModified()

        if (timestamp == 0L) return null
        return new Date(timestamp)
    }


    /**
     * 
     * @param file
     * @return
     */
    Date fileCreationDate(DataFile file) {

        if (file == null) return null

        String path = getFilePath(file)
        if (path == null) return null

        return fileCreationDate(path)
    }


    /**
     * 
     * @param fileId
     * @return
     */
    Date fileCreationDate(long fileId) {

        DataFile file = DataFile.get(fileId)
        if (file == null) return null
        return fileCreationDate(file)
    }

    ////////////////////////////////////////////////////////////////////////////

    boolean runInInitialLocation(long runId) {
        
        //File 
    }
    
    /**
     * 
     * @param runId
     * @return
     */
    boolean runInFinalLocation(long runId) {

        Run run = Run.get(runId)
        if (run == null) return false
        return runInFinalLocation(run)
    }

    /**
     * 
     * @param run
     * @return
     */
    boolean runInFinalLocation(Run run) {

        if (run == null) return false
        String [] paths = getAllPathsForRun(run)

        run.finalLocation = false;
        //if (path.length > 1) run.multipleSource = true
        run.save(flush: true)

        boolean locationMissing = false
        paths.each {String path ->

            println path
            File file = new File(path + "/run" + run.name)
            if (!file.isDirectory() || !file.canRead()) 
                locationMissing = true
        }

        if (locationMissing) return false

        run.finalLocation = true
        run.save(flush: true)
        return true
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * 
     */
    String[] getAllPathsForRun(long runId) {

        Run run = Run.getAt(runId)
        if (run == null) return null
        return getAllPathsForRun(run)
    }


    /**
     * 
     * 
     */
    String[] getAllPathsForRun(Run run) {

        if (run == null) return null

        Set<String> paths = new HashSet<String>()

        run.dataFiles.each {DataFile file ->

            if (file.fileType.type == FileType.Type.METADATA) return

            String path = getPathToRun(file)
            if (path == null) paths << run.dataPath
            paths << path
        }

        return (String[])paths.toArray()
    }



    /**
     * 
     * 
     */
    private String getPathToRun(DataFile file) {

        if (file == null) return null
        if (file.fileType.type == FileType.Type.METADATA) return null
        if (!file.used) return null

        SeqTrack seqTrack = file.seqTrack ?: file.alignmentLog.seqTrack
        if (seqTrack == null) return null

        String path =
            basePath + file?.project?.dirName + "/sequencing/" +
            seqTrack.seqType.dirName + "/" + file.run.seqCenter?.dirName + "/"

        return path
    }

    ////////////////////////////////////////////////////////////////////////////
    
    void moveRunToFinalLocations(long runId) {
        
    }
    
    ////////////////////////////////////////////////////////////////////////////
    boolean checkIfRunComplete(long runId) {
        
        
        
    }
    
    boolean checkIfRunComplete(Run run) {
        
    }
    
    ////////////////////////////////////////////////////////////////////////////

    boolean checkAllRuns(String projectName) {

        Project project = Project.findByName(projectName)

        String dir = basePath + "/" + project.dirName + "/sequencing/"
        File baseDir = new File(dir)

        File[] seqDirs = baseDir.listFiles()
        int nMissing = 0;

        for(int i=0; i<seqDirs.size(); i++) {

            File seqTypeDir = seqDirs[i]
            if (!seqTypeDir.isDirectory()) {
                continue
            }

            SeqType seqType = SeqType.findByDirName(seqTypeDir.getName())
            if (seqType == null) {
                continue
            }

            File[] seqCenters = seqTypeDir.listFiles()

            for(int j=0; j<seqCenters.size(); j++) {

                SeqCenter center = SeqCenter.findByDirName(seqCenters[j].getName())
                if (center == null) {
                    continue
                }

                println "\nChecking ${seqTypeDir} ${seqCenters[j]}"
                File[] runs = seqCenters[j].listFiles()

                for(int iRun=0; iRun<runs.size(); iRun++) {

                    if (!runs[iRun].getName().contains("run")) {
                        continue
                    }
                    println "\t ${runs[iRun].getName()}"

                    String runName = runs[iRun].getName().substring(3)
                    Run run = Run.findByName(runName)

                    if (run == null) {
                        nMissing++
                        println "!!! Run ${runName} does not exist !!!"
                    }

                }
            }
        }

        if (nMissing > 0) {
            return false
        }
        return true
    }

    ////////////////////////////////////////////////////////////////////////////
}

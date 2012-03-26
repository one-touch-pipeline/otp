package de.dkfz.tbi.otp.ngsdata

class MetaDataRegistrationService {

    /**
    *
    * looks into directory pointed by mdPath of a Run object
    * and register files that could be meta-data files
    *
    * @param runId - database id or the Run object
    */
    void registerInputFiles(long runId) {
        Run run = Run.get(runId)
        List<RunInitialPath> paths = RunInitialPath.findAllByRun(run)
        for(RunInitialPath path in paths) {
            registerInputFilesForPath(path)
        }
        if (paths.size() > 1) {
            run.multipleSource = true
        }
        run.save(flush: true)
    }

    private void registerInputFilesForPath(RunInitialPath path) {
        File dir = getMetaDataDirectory(path.mdPath, path.run.name)
        processDirectory(path, dir)
    }

    private File getMetaDataDirectory(String path, String runName) {
       List<String> separators = ["/run", "/"]
       for(String separator in separators) {
           String runDir = path + separator + runName
           File dir = new File(runDir)
           if (dir.canRead() && dir.isDirectory()) {
               return dir
           }
       }
       throw new DirectoryNotReadableException(path)
    }

    private void processDirectory(RunInitialPath path, File dir) {
        MetaDataFile metaDataFile
        List<String> fileNames = dir.list()
        for(String fileName in fileNames) {
            //println  fileName
            if (fileBlacklisted(fileName)) {
                continue
            }
            if (fileName.contains("fastq") || fileName.contains("align")) {
                if (isFileRegistered(path, fileName)) {
                    throw new MetaDataFileDuplicationException(path.run.name, fileName)
                }
                metaDataFile = new MetaDataFile(
                    fileName: fileName,
                    filePath: dir.absolutePath,
                    runInitialPath: path,
                    used: false
                )
                metaDataFile.validate()
                //println metaDataFile.errors
                metaDataFile.save(flush: true)
            }
        }
    }

    private boolean fileBlacklisted(String fileName) {
        String[] blacklist = ["wrong", "old"]
        for(String name in blacklist) {
            if (fileName.contains(name)) {
                return true
            }
        }
        return false
    }

    private boolean isFileRegistered(RunInitialPath path, String fileName) {
        MetaDataFile existingFile =
            MetaDataFile.findByRunInitialPathAndFileName(path, fileName)
        return (boolean)existingFile
    }
}

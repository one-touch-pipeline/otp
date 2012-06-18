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
        List<RunSegment> segments =
            RunSegment.findAllByRunAndMetaDataStatus(run, RunSegment.Status.BLOCKED)
        for(RunSegment segment in segments) {
            registerInputFilesForPath(segment)
            segment.metaDataStatus = RunSegment.Status.PROCESSING
            segment.save(flush: true)
        }
        if (segments.size() > 1) {
            run.multipleSource = true
        }
        run.save(flush: true)
    }

    private void registerInputFilesForPath(RunSegment path) {
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

    private void processDirectory(RunSegment path, File dir) {
        MetaDataFile metaDataFile
        List<String> fileNames = dir.list()
        for(String fileName in fileNames) {
            //println  fileName
            if (fileBlacklisted(fileName)) {
                continue
            }
            if (!fileName.endsWith(".tsv")) {
                continue
            }
            if (fileName.contains("fastq") || fileName.contains("align")) {
                if (isFileRegistered(path, fileName)) {
                    throw new MetaDataFileDuplicationException(path.run.name, fileName)
                }
                metaDataFile = new MetaDataFile(
                    fileName: fileName,
                    filePath: dir.absolutePath,
                    runSegment: path,
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

    private boolean isFileRegistered(RunSegment path, String fileName) {
        MetaDataFile existingFile =
            MetaDataFile.findByRunSegmentAndFileName(path, fileName)
        return (boolean)existingFile
    }
}

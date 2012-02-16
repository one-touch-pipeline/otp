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
       log.debug("registering run ${run.name} from ${run.seqCenter}")

       FileType fileType = FileType.findByType(FileType.Type.METADATA)
       DataFile dataFile

       File dir = getMetaDataDirectory(run)
       List<String> fileNames = dir.list()
       fileNames.each { String fileName ->
           if (fileBlacklisted(fileName)) {
               return
           }
           if (fileName.contains("fastq") || fileName.contains("align")) {
               if (isFileRegistered(run, fileName)) {
                   throw new MetaDataFileDuplicationException(run.name, fileName)
               }
               dataFile = new DataFile(
                   pathName: dir.getAbsolutePath(),
                   fileName: fileName
               )
               dataFile.run = run
               dataFile.fileType = fileType
               dataFile.save(flush: true)
           }
       }
       fileType.save(flush: true)
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

    private File getMetaDataDirectory(Run run) {
       List<String> separators = ["/run", "/"]
       for(String separator in separators) {
           String runDir = run.mdPath + separator + run.name
           File dir = new File(runDir)
           if (dir.canRead() && dir.isDirectory()) {
               return dir
           }
       }
       throw new DirectoryNotReadableException(run.mdPath)
   }

   private boolean isFileRegistered(Run run, String fileName) {
       DataFile file = DataFile.findByRunAndFileName(run, fileName)
       return (boolean)file
   }
}

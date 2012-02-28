package de.dkfz.tbi.otp.ngsdata

class MetaDataFileService {

    public String initialLocation(MetaDataFile file) {
        file.filePath + File.separatorChar + file.fileName
    }

    public String finalLocation(MetaDataFile file) {
        final String base = "${home}ngs-icgc/"
        final String path = "/data-tracking-archive/"
        //Run run = file.runInitialPath
        //String path = base + path + file.
    }
}

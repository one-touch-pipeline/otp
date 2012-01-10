package de.dkfz.tbi.otp.ngsdata

class DataFileController {

    def lsdfFilesService

    def scaffold = DataFile

    def showDetails = {

        DataFile dataFile = DataFile.get(params.id)
        println dataFile.project
        
        List<String> keys = new Vector<String>()
        List<String> values = new Vector<String>()

        keys << "file name"
        values << dataFile.fileName

        keys <<  "full path"
        values << lsdfFilesService.getFileFinalPath(dataFile)

        keys << "view-by-pid name"
        values << dataFile.vbpFileName

        keys << "view-by-pid path"
        values << lsdfFilesService.getFileViewByPidPath(dataFile)

        return [keys: keys, values: values]
    }
}

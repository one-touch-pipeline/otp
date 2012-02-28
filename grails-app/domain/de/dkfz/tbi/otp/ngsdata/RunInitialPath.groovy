package de.dkfz.tbi.otp.ngsdata

class RunInitialPath {

    String dataPath                  // path to data (ftp area)
    String mdPath                    // path to meta-data

    static belongsTo = [run : Run]
    static constraints = {
        dataPath()
        mdPath()
    }
}

package de.dkfz.tbi.otp.ngsdata

class Realm {

    String name
    String rootPath
    String webHost
    String host
    String port
    String user
    String timeout

    static constraints = {
        name(unique: true)
    }
}

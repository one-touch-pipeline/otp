package de.dkfz.tbi.otp.ngsdata

class Realm {

    String name              // name of the realm
    String rootPath          // mount path of the file system with data
    String webHost           // web address
    String host              // PBS head address
    String port
    String unixUser
    String timeout
    String pbsOpts

    static constraints = {
        name(unique: true)
    }
}

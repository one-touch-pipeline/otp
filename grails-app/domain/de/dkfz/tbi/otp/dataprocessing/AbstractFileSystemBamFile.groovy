package de.dkfz.tbi.otp.dataprocessing

import java.util.Date;

class AbstractFileSystemBamFile extends AbstractBamFile {

    boolean fileExists
    Date dateCreated = new Date()
    Date dateFromFileSystem
    long fileSize = -1

    static constraints = {
        dateFromFileSystem(nullable: true)
    }
}

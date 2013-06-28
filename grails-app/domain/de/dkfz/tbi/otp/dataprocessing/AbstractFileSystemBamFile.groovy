package de.dkfz.tbi.otp.dataprocessing

/**
 * Represents bam files stored on the file system.
 * Keeps file-system related properties of a bam file.
 *
 *
 */
class AbstractFileSystemBamFile extends AbstractBamFile {

    /**
     * is true if file exists on the file system
     */
    boolean fileExists

    /**
     * date of creation of object in the database
     */
    Date dateCreated = new Date()

    /**
     * date of last modification of the file on the file system
     */
    Date dateFromFileSystem

    /**
     * file size
     */
    long fileSize = -1

    static constraints = {
        dateFromFileSystem(nullable: true)
    }
}

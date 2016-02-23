package de.dkfz.tbi.otp.dataprocessing

/**
 * Represents bam files stored on the file system.
 * Keeps file-system related properties of a bam file.
 *
 *
 */
abstract class AbstractFileSystemBamFile extends AbstractBamFile {

    /**
     * Checksum to verify success of copying.
     * When the file - and all other files handled by the transfer workflow - are copied, its checksum is stored in this property.
     * Otherwise it is null.
     */
    String md5sum

    /** Additional digest, may be used in the future (to verify xz compression) */
    String sha256sum

    /**
     * Is true if file exists on the file system in the processing directory.
     * If the file is only stored in the project directory and was deleted in the processing directory it is marked
     * as fileExists = false!
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
        md5sum nullable: true, matches: /^[0-9a-f]{32}$/
        sha256sum nullable: true
    }
}

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus.*
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

class LinkFilesToFinalDestinationService {

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @SuppressWarnings("GrailsStatelessService")
    @Autowired
    LinkFileUtils linkFileUtils

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    ExecutionHelperService executionHelperService

    @Autowired
    CreateClusterScriptService createClusterScriptService

    @SuppressWarnings("GrailsStatelessService")
    @Autowired
    RemoteShellHelper remoteShellHelper

    @Autowired
    AbstractMergedBamFileService abstractMergedBamFileService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    CreateNotificationTextService createNotificationTextService


    public void prepareRoddyBamFile(RoddyBamFile roddyBamFile) {
        assert roddyBamFile: "roddyBamFile must not be null"
        if (!roddyBamFile.withdrawn) {
            RoddyBamFile.withTransaction {
                assert roddyBamFile.isMostRecentBamFile(): "The BamFile ${roddyBamFile} is not the most recent one. This must not happen!"
                if (!roddyBamFile.config.adapterTrimmingNeeded) {
                    assert roddyBamFile.numberOfReadsFromQa >= roddyBamFile.numberOfReadsFromFastQc: "bam file (${roddyBamFile.numberOfReadsFromQa}) has less number of reads than the sum of all fastqc (${roddyBamFile.numberOfReadsFromFastQc})"
                }
                assert [NEEDS_PROCESSING, INPROGRESS].contains(roddyBamFile.fileOperationStatus)
                roddyBamFile.fileOperationStatus = INPROGRESS
                assert roddyBamFile.save(flush: true)
                roddyBamFile.validateAndSetBamFileInProjectFolder()
            }
        }
    }

    public void linkToFinalDestinationAndCleanup(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "roddyBamFile must not be null"
        assert realm: "realm must not be null"
        if (!roddyBamFile.withdrawn) {
            cleanupWorkDirectory(roddyBamFile, realm)
            executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
            cleanupOldResults(roddyBamFile, realm)
            handleQcCheck(roddyBamFile) {
                linkNewResults(roddyBamFile, realm)
            }
        } else {
            threadLog?.info "The results of ${roddyBamFile} will not be moved since it is marked as withdrawn"
        }
    }

    void linkToFinalDestinationAndCleanupRna(RnaRoddyBamFile roddyBamFile, Realm realm) {
        executeRoddyCommandService.correctPermissionsAndGroups(roddyBamFile, realm)
        cleanupOldRnaResults(roddyBamFile, realm)
        handleQcCheck(roddyBamFile) {
            linkNewRnaResults(roddyBamFile, realm)
        }
    }


    private void handleQcCheck(RoddyBamFile roddyBamFile, Closure linkCall) {
        if (!roddyBamFile.qcTrafficLightStatus || roddyBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED) {
            linkCall()
        } else if (roddyBamFile.qcTrafficLightStatus == AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED) {
            informResultsAreBlocked(roddyBamFile)
        } else {
            throw new RuntimeException("${roddyBamFile.qcTrafficLightStatus} is not a valid qcTrafficLightStatus here, only ${AbstractMergedBamFile.QcTrafficLightStatus.QC_PASSED} and ${AbstractMergedBamFile.QcTrafficLightStatus.BLOCKED} is a valid status.")
        }
        setBamFileValues(roddyBamFile)
    }

    void setBamFileValues(RoddyBamFile roddyBamFile) {
        File md5sumFile = roddyBamFile.workMd5sumFile
        assert md5sumFile.exists(): "The md5sum file of ${roddyBamFile} does not exist"
        assert md5sumFile.text: "The md5sum file of ${roddyBamFile} is empty"
        RoddyBamFile.withTransaction {
            roddyBamFile.fileOperationStatus = PROCESSED
            roddyBamFile.fileSize = roddyBamFile.workBamFile.size()
            roddyBamFile.md5sum = md5sumFile.text.replaceAll("\n", "").toLowerCase(Locale.ENGLISH)
            roddyBamFile.fileExists = true
            roddyBamFile.dateFromFileSystem = new Date(roddyBamFile.workBamFile.lastModified())
            assert roddyBamFile.save(flush: true)
            abstractMergedBamFileService.setSamplePairStatusToNeedProcessing(roddyBamFile)
        }
    }

    /**
     * Link files (replaces existing files)
     */
    void linkNewResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        Map<File, File> linkMapSourceLink = [:]

        //collect links for files and qa merge directory
        ['Bam', 'Bai', 'Md5sum'].each {
            linkMapSourceLink.put(roddyBamFile."work${it}File", roddyBamFile."final${it}File")
        }
        linkMapSourceLink.put(roddyBamFile.workMergedQADirectory, roddyBamFile.finalMergedQADirectory)

        if (roddyBamFile.seqType.isWgbs()) {
            linkMapSourceLink.put(roddyBamFile.workMergedMethylationDirectory, roddyBamFile.finalMergedMethylationDirectory)
            if (roddyBamFile.getContainedSeqTracks()*.getLibraryDirectoryName().unique().size() > 1) {
                [roddyBamFile.workLibraryQADirectories.values().asList().sort(), roddyBamFile.finalLibraryQADirectories.values().asList().sort()].transpose().each {
                    linkMapSourceLink.put(it[0], it[1])
                }
                [roddyBamFile.workLibraryMethylationDirectories.values().asList().sort(), roddyBamFile.finalLibraryMethylationDirectories.values().asList().sort()].transpose().each {
                    linkMapSourceLink.put(it[0], it[1])
                }
            }
            linkMapSourceLink.put(roddyBamFile.workMetadataTableFile, roddyBamFile.finalMetadataTableFile)
        }

        //collect links for every execution store
        [roddyBamFile.getWorkExecutionDirectories(), roddyBamFile.getFinalExecutionDirectories()].transpose().each {
            linkMapSourceLink.put(it[0], it[1])
        }

        //collect links for the single lane qa
        Map<SeqTrack, File> workSingleLaneQADirectories = roddyBamFile.workSingleLaneQADirectories
        Map<SeqTrack, File> finalSingleLaneQADirectories = roddyBamFile.finalSingleLaneQADirectories
        workSingleLaneQADirectories.each { seqTrack, singleLaneQaWorkDir ->
            File singleLaneQcDirFinal = finalSingleLaneQADirectories.get(seqTrack)
            linkMapSourceLink.put(singleLaneQaWorkDir, singleLaneQcDirFinal)
        }

        if (roddyBamFile.baseBamFile?.isOldStructureUsed()) {
            lsdfFilesService.deleteFilesRecursive(realm, [roddyBamFile.baseBamFile.finalMergedQADirectory])
        }

        //create the collected links
        linkFileUtils.createAndValidateLinks(linkMapSourceLink, realm)
    }

    String createResultsAreBlockedSubject(RoddyBamFile roddyBamFile) {
        StringBuilder subject = new StringBuilder()
        if (!roddyBamFile.project.mailingListName) {
            subject << 'TO BE SENT: '
        }

        subject << createNotificationTextService.createMessage(
                "notification.template.alignment.qcTrafficBlockedSubject",
                [
                        roddyBamFile: roddyBamFile,
                ]
        )

        return subject.toString()
    }


    String createResultsAreBlockedMessage(RoddyBamFile roddyBamFile) {
        return createNotificationTextService.createMessage(
                "notification.template.alignment.qcTrafficBlockedMessage",
                [
                        roddyBamFile         : roddyBamFile,
                        link                 : createNotificationTextService.createOtpLinks([roddyBamFile.project], 'alignmentQualityOverview', 'index'),
                        emailSenderSalutation: ProcessingOptionService.findOptionAssure(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION, null, null),
                ]
        )
    }

    void informResultsAreBlocked(RoddyBamFile roddyBamFile) {
        List<String> recipients = [
                roddyBamFile.project.mailingListName,
                ProcessingOptionService.getValueOfProcessingOption(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION),
        ].findAll()
        String subject = createResultsAreBlockedSubject(roddyBamFile)
        String content = createResultsAreBlockedMessage(roddyBamFile)
        mailHelperService.sendEmail(subject, content, recipients)
    }


    void linkNewRnaResults(RnaRoddyBamFile roddyBamFile, Realm realm) {
        File baseDirectory = roddyBamFile.getBaseDirectory()
        Map links = roddyBamFile.workDirectory.listFiles().findAll {
            !it.name.startsWith(".")
        }.collectEntries { File source ->
            [(source): new File(baseDirectory, source.name)]
        }
        linkFileUtils.createAndValidateLinks(links, realm)
    }

    void cleanupWorkDirectory(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> expectedFiles = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
        ]
        if (roddyBamFile.seqType.isWgbs()) {
            expectedFiles.add(roddyBamFile.workMethylationDirectory)
            expectedFiles.add(roddyBamFile.workMetadataTableFile)
        }
        List<File> foundFiles = roddyBamFile.workDirectory.listFiles() ?: []
        List<File> filesToDelete = foundFiles - expectedFiles

        lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
    }


    void cleanupOldResults(RoddyBamFile roddyBamFile, Realm realm) {
        assert roddyBamFile: "Input roddyBamFile must not be null"
        assert realm: "Input realm must not be null"
        assert !roddyBamFile.isOldStructureUsed()

        List<File> filesToDelete = []
        List<File> roddyDirsToDelete = []
        if (roddyBamFile.baseBamFile) {
            if (!roddyBamFile.baseBamFile.isOldStructureUsed()) {
                filesToDelete.add(roddyBamFile.baseBamFile.workBamFile)
                filesToDelete.add(roddyBamFile.baseBamFile.workBaiFile)
                //the md5sum is kept: roddyBamFile.baseBamFile.workMd5sumFile
            }
        } else {
            List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
            if (roddyBamFiles) {
                List<File> workDirs = roddyBamFiles.findAll { !it.isOldStructureUsed() }*.workDirectory
                filesToDelete.addAll(workDirs)
                filesToDelete.add(roddyBamFile.finalExecutionStoreDirectory)
                filesToDelete.add(roddyBamFile.finalQADirectory)
                if (roddyBamFile.seqType.isWgbs()) {
                    filesToDelete.add(roddyBamFile.finalMethylationDirectory)
                }
                roddyDirsToDelete.addAll(workDirs)
                roddyBamFiles.findAll {
                    it.isOldStructureUsed()
                }.each {
                    roddyDirsToDelete.addAll(it.finalExecutionDirectories)
                    roddyDirsToDelete.addAll(it.finalSingleLaneQADirectories.values())
                }
                if (roddyBamFiles.max { it.identifier }.oldStructureUsed) {
                    roddyDirsToDelete.add(roddyBamFile.finalMergedQADirectory)
                }
            }
        }

        if (filesToDelete) {
            lsdfFilesService.deleteFilesRecursive(realm, filesToDelete)
        }
    }

    void cleanupOldRnaResults(RnaRoddyBamFile roddyBamFile, Realm realm) {
        List<RoddyBamFile> roddyBamFiles = RoddyBamFile.findAllByWorkPackageAndIdNotEqual(roddyBamFile.mergingWorkPackage, roddyBamFile.id)
        List<File> workDirs = roddyBamFiles*.workDirectory
        if (workDirs) {
            lsdfFilesService.deleteFilesRecursive(realm, workDirs)
        }
        String cmd = "find ${roddyBamFile.getBaseDirectory()} -maxdepth 1 -lname '.merging*/*' -delete;"
        remoteShellHelper.executeCommandReturnProcessOutput(realm, cmd)
    }
}

package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.tracking.ProcessingTimeStatisticsService
import de.dkfz.tbi.otp.utils.MailHelperService
import org.springframework.scheduling.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import grails.validation.ValidationException
import org.joda.time.*

class DataFileService {

    static transactional = false
    static final int MAX_RESULTS = 1000

    LsdfFilesService lsdfFilesService

    MailHelperService mailHelperService

    //12h
    @Scheduled(fixedDelay = 43200000l)
    void setFileExistsForAllDataFiles() {
        Date startDate = new Date()
        try {
            (0..countDataFiles() / MAX_RESULTS).each {
                DataFile.withTransaction {
                    getFastqDataFiles().each {
                        String path = lsdfFilesService.getFileFinalPath(it)
                        if (path) {
                            File file = new File(path)
                            it.fileExists = file.exists()
                        }
                        it.dateLastChecked = new Date()
                        try {
                            assert it.save(flush: true)
                        } catch (ValidationException e) {
                            throw new RuntimeException("Error while saving datafile with id: ${it.id}", e)
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            log.error("error ${e.getLocalizedMessage()}", e)
            String recipientsString = ProcessingOptionService.findOption(
                    ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS,
                    null,
                    null,
            )
            if (recipientsString) {
                mailHelperService.sendEmail("Error: DataFileService.setFileExistsForAllDataFiles() failed", "${e.getLocalizedMessage()}\n${e.getCause()}", recipientsString)
            }
        }
        log.info("DataFileService.setFileExistsForAllDataFiles() duration: ${ProcessingTimeStatisticsService.getFormattedPeriod(startDate, new Date())}")
    }

    List<DataFile> getFastqDataFiles() {
        return DataFile.createCriteria().list {
            eq('fileWithdrawn', false)
            fileType {
                eq('type', Type.SEQUENCE)
                or {
                    eq('subType', 'fastq')
                    eq('subType', 'fq.gz')
                    eq('subType', 'fastq')
                    eq('subType', 'seq')
                }
            }
            seqTrack {
                eq('dataInstallationState', DataProcessingState.FINISHED)
            }
            order('dateLastChecked', 'asc')
            maxResults(MAX_RESULTS)
        }
    }

    int countDataFiles() {
        return DataFile.createCriteria().count {
            eq('fileWithdrawn', false)
            fileType {
                eq('type', Type.SEQUENCE)
                or {
                    eq('subType', 'fastq')
                    eq('subType', 'fq.gz')
                    eq('subType', 'fastq')
                    eq('subType', 'seq')
                }
            }
            seqTrack {
                eq('dataInstallationState', DataProcessingState.FINISHED)
            }
        }
    }
}

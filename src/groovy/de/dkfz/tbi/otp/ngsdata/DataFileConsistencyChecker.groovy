package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*
import grails.validation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.*

@Scope("singleton")
@Component
class DataFileConsistencyChecker {

    static final int MAX_RESULTS = 1000

    @Autowired
    LsdfFilesService lsdfFilesService

    @Autowired
    MailHelperService mailHelperService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    ProcessingOptionService processingOptionService

    //12h
    @Scheduled(fixedDelay = 43200000L, initialDelay = 60000L)
    void setFileExistsForAllDataFiles() {
        if (schedulerService.isActive()) {
            Date startDate = new Date()
            try {
                (0..countDataFiles() / MAX_RESULTS).each {
                    DataFile.withNewSession {
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
                }
            } catch (RuntimeException e) {
                log.error("error ${e.getLocalizedMessage()}", e)
                ProcessingOption.withNewSession {
                    String recipientsString = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
                    if (recipientsString) {
                        mailHelperService.sendEmail("Error: DataFileConsistencyChecker.setFileExistsForAllDataFiles() failed", "${e.getLocalizedMessage()}\n${e.getCause()}", recipientsString)
                    }
                }
            }
            log.info("DataFileConsistencyChecker.setFileExistsForAllDataFiles() duration: ${ProcessingTimeStatisticsService.getFormattedPeriod(startDate, new Date())}")
        }
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

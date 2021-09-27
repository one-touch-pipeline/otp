/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.FileType.Type
import de.dkfz.tbi.otp.ngsdata.SeqTrack.DataProcessingState
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.util.TimeUtils

@Scope("singleton")
@Component
@Slf4j
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
                    SessionUtils.withNewSession {
                        DataFile.withTransaction {
                            getFastqDataFiles().each {
                                String path = lsdfFilesService.getFileFinalPath(it)
                                if (path) {
                                    File file = new File(path)
                                    it.fileExists = file.exists()
                                }
                                it.dateLastChecked = new Date()
                                try {
                                    assert it.save(deepValidate: false)
                                } catch (ValidationException e) {
                                    throw new RuntimeException("Error while saving datafile with id: ${it.id}", e)
                                }
                            }
                        }
                    }
                }
            } catch (RuntimeException e) {
                log.error("error ${e.getLocalizedMessage()}", e)
                SessionUtils.withNewSession {
                    String recipientsString = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_ERRORS)
                    if (recipientsString) {
                        mailHelperService.sendEmail("Error: DataFileConsistencyChecker.setFileExistsForAllDataFiles() failed",
                                "${e.getLocalizedMessage()}\n${e.getCause()}", recipientsString)
                    }
                }
            }
            log.info("DataFileConsistencyChecker.setFileExistsForAllDataFiles() duration: " +
                    "${TimeUtils.getFormattedDurationWithDays(startDate, new Date())}")
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
                    eq('subType', 'seq')
                }
            }
            seqTrack {
                eq('dataInstallationState', DataProcessingState.FINISHED)
            }
        }
    }
}

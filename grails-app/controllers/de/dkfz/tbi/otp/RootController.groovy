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
package de.dkfz.tbi.otp

import grails.converters.JSON
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.project.ProjectService

class RootController {
    ProjectService projectService
    ProjectSelectionService projectSelectionService
    SpringSecurityService springSecurityService
    StatisticService statisticService
    ProcessingOptionService processingOptionService

    @SuppressWarnings('DuplicateNumberLiteral') // 60 seconds vs 60 minutes
    private static final long FOUR_WEEKS_IN_MS = 1000L * 60 * 60 * 24 * 7 * 4


    def intro() {
        if (springSecurityService.isLoggedIn()) {
            forward action: "start"
            return
        }

        String messageText, type
        switch (params.login) {
            case 'required':
                messageText = message(code: "login.message.pleaseLogin")
                type = "warning"
                break
            case 'failed':
                messageText = message(code: "login.message.loginFailed")
                type = "danger"
                break
        }

        String aboutOtp = processingOptionService.findOptionAsString(OptionName.GUI_ABOUT_OTP)

        return [
                projects    : Project.count,
                lanes       : SeqTrack.count,
                username    : session.getAttribute(SpringSecurityUtils.SPRING_SECURITY_LAST_USERNAME_KEY),
                message     : messageText,
                type        : type,
                aboutOtp    : aboutOtp,
        ]
    }

    JSON introPatientsBySeqType() {
        List<String> labels = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List patientsCountBySeqType = statisticService.patientsCountPerSequenceType(null)

        patientsCountBySeqType.each {
            labels.add(it[0])
            values.add(it[1])
            projectSequenceCount += it[1]
        }

        Map dataToRender = [
                labels: labels,
                data  : values,
        ]
        render dataToRender as JSON
    }

    JSON introSamplesBySeqType() {
        List<String> labels = []
        List<Integer> values = []
        int projectSequenceCount = 0

        List sampleCountBySeqType = statisticService.sampleCountPerSequenceType(null)

        int filterCount = sampleCountBySeqType.collect { it[1] }.sum() / 100

        sampleCountBySeqType.each {
            if (it[1] < filterCount) {
                return
            }
            values.add(it[1])
            projectSequenceCount += it[1]
        }

        sampleCountBySeqType.each {
            if (it[1] < filterCount) {
                return
            }
            labels.add("${it[0]} ${Math.round(it[1] * 100 / projectSequenceCount)} %")
        }

        Map dataToRender = [
                labels: labels,
                data  : values,
        ]
        render dataToRender as JSON
    }

    def start() {
        if (!springSecurityService.isLoggedIn()) {
            forward action: "intro"
            return
        }

        List<Project> projects = [projectSelectionService.selectedProject]

        HibernateCriteriaBuilder c
        Date fourWeeksAgo = new Date(System.currentTimeMillis() - FOUR_WEEKS_IN_MS)

        int individualsFinder = projects ? Individual.countByProjectInList(projects) : []
        c = Sample.createCriteria()
        int samples = c.count {
            individual {
                'in'('project', projects)
            }
        }
        c = SeqTrack.createCriteria()
        int seqTracks = c.count {
            sample {
                individual {
                    'in'('project', projects)
                }
            }
        }

        c = DataFile.createCriteria()
        int newIndividuals = c.get {
            seqTrack {
                sample {
                    projections {
                        countDistinct("individual")
                    }
                    individual {
                        'in'('project', projects)
                    }
                }
            }
            gt('dateCreated', fourWeeksAgo)
        }
        c = DataFile.createCriteria()
        int newSamples = c.get {
            seqTrack {
                projections {
                    countDistinct("sample")
                }
                sample {
                    individual {
                        'in'('project', projects)
                    }
                }
            }
            gt('dateCreated', fourWeeksAgo)
        }
        c = DataFile.createCriteria()
        int newSeqTracks = c.get {
            projections {
                countDistinct("seqTrack")
            }
            seqTrack {
                sample {
                    individual {
                        'in'('project', projects)
                    }
                }
            }
            gt('dateCreated', fourWeeksAgo)
        }

        return [
                numberIndividuals   : individualsFinder,
                numberSamples       : samples,
                numberSeqTracks     : seqTracks,
                numberNewIndividuals: newIndividuals,
                numberNewSamples    : newSamples,
                numberNewSeqTracks  : newSeqTracks,
        ]
    }
}

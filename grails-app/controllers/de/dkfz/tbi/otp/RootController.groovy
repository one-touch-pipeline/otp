package de.dkfz.tbi.otp

import grails.converters.JSON
import grails.orm.HibernateCriteriaBuilder
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*

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

        ProjectSelection selection = projectSelectionService.getSelectedProject()

        HibernateCriteriaBuilder c
        Date fourWeeksAgo = new Date(System.currentTimeMillis() - FOUR_WEEKS_IN_MS)

        int individualsFinder = selection.projects ? Individual.countByProjectInList(selection.projects) : []
        c = Sample.createCriteria()
        int samples = c.count {
            individual {
                'in'('project', selection.projects)
            }
        }
        c = SeqTrack.createCriteria()
        int seqTracks = c.count {
            sample {
                individual {
                    'in'('project', selection.projects)
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
                        'in'('project', selection.projects)
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
                        'in'('project', selection.projects)
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
                        'in'('project', selection.projects)
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

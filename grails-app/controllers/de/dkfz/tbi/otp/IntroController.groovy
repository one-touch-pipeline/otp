package de.dkfz.tbi.otp

import de.dkfz.tbi.otp.ngsdata.*
import grails.converters.*
import grails.plugin.springsecurity.*


class IntroController {

    StatisticService statisticService

    def index() {
        String messageText, type
        switch (params.login) {
            case 'required':
                messageText = message(code:"intro.message.pleaseLogin")
                type = "warning"
                break;
            case 'failed':
                messageText = message(code:"intro.message.loginFailed")
                type = "danger"
                break;
        }

        return [
                projects: Project.count,
                lanes: SeqTrack.count,
                username: session.getAttribute(SpringSecurityUtils.SPRING_SECURITY_LAST_USERNAME_KEY),
                message: messageText,
                type: type,
        ]
    }

    JSON patientsBySeqType() {
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

    JSON samplesBySeqType() {
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
}

package de.dkfz.tbi.otp.tracking

import spock.lang.Specification

class ProcessingTimeStatisticsServiceSpec extends Specification {

    void "getFormattedPeriod, when d1 is missing, return empty String"() {
        expect:
        "" == ProcessingTimeStatisticsService.getFormattedPeriod(null, new Date())
    }

    void "getFormattedPeriod, when d2 is missing, return empty String"() {
        expect:
        "" == ProcessingTimeStatisticsService.getFormattedPeriod(new Date(), null)
    }

    void "getFormattedPeriod, when period is negative, return formatted String marked negativ "() {
        given:
        int dateDiff = 1
        Date date = new Date()

        expect:
        "-${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date.minus(dateDiff))

    }

    void "getFormattedPeriod, when period is positive, return formatted String"() {
        given:
        int dateDiff = 1
        Date date = new Date()

        expect:
        "${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date.plus(dateDiff))
    }

    void "getFormattedPeriod, when period is longer than a week, return formatted String with days as most superior unit"() {
        given:
        int dateDiff = 14
        Date date = new Date()

        expect:
        "-${dateDiff.toString().padLeft(2, '0')}d 00h 00m" == ProcessingTimeStatisticsService.getFormattedPeriod(date, date.minus(dateDiff))
    }
}

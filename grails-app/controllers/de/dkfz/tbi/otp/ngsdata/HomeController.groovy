package de.dkfz.tbi.otp.ngsdata

class HomeController {

    def homeService

    Map index() {
        Map queryResult = homeService.projectQuery()
        return [projectQuery: queryResult]
    }
}

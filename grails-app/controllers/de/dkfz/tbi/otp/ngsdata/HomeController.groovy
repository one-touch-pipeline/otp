package de.dkfz.tbi.otp.ngsdata

class HomeController {

    HomeService homeService
    ProjectGroupService projectGroupService

    Map index() {

        Map queryResult = homeService.projectQuery()
        return [
                projectQuery: queryResult,
                projectGroups: ["OTP"] + projectGroupService.availableProjectGroups()*.name,
        ]
    }
}

package de.dkfz.tbi.otp.ngsdata

class HomeController {

    HomeController homeService
    ProjectGroupService projectGroupService

    Map index() {

        Map queryResult = homeService.projectQuery()
        return [
                projectQuery: queryResult,
                projectGroups: ["OTP"] + projectGroupService.availableProjectGroups()*.name,
        ]
    }
}

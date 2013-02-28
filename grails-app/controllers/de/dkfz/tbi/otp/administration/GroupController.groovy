package de.dkfz.tbi.otp.administration

class GroupController {

    def groupService

    def index() {
        Map usersByGroup = groupService.usersByGroup()
        [usersByGroup: usersByGroup]
    }
}

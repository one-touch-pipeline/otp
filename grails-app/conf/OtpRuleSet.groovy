ruleset {

    description '''
        The custom Rules for the OTP-Project
        '''

    rule("file:grails-app/codenarcRules/ScheduledServiceBugRule.groovy")
    rule("file:grails-app/codenarcRules/DoNotCreateServicesWithNewRule.groovy")
}

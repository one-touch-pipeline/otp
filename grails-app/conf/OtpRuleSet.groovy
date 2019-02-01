ruleset {

    description '''
        The custom Rules for the OTP-Project
        '''

    rule("file:grails-app/codenarcRules/ScheduledServiceBugRule.groovy")
    rule("file:grails-app/codenarcRules/DoNotCreateServicesWithNewRule.groovy")
    rule("file:grails-app/codenarcRules/EnumForBeanNameRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForValidatorsRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForStartJobsRule.groovy")
    rule("file:grails-app/codenarcRules/AnnotationsForJobsRule.groovy")
}

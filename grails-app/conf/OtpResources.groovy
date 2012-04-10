modules = {
    core {
        dependsOn 'jquery'
        resource url: '/js/jquery/jquery.i18n.properties-min-1.0.9.js'
        resource url: '/js/otp.js'
    }
    crashRecovery {
        dependsOn 'jquery-ui, core'
        resource url: '/js/crashrecovery.js'
    }
    userAdministration {
        dependsOn 'core'
        resource url: '/js/useradministration.js'
    }
}

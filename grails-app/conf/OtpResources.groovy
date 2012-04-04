modules = {
    core {
        dependsOn 'jquery'
        resource url: '/js/otp.js'
    }
    crashRecovery {
        dependsOn 'jquery-ui, core'
        resource url: '/js/crashrecovery.js'
    }
}

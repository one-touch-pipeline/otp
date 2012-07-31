modules = {
    application {
    }
    'style' {
        resource url:'/css/otp.less'
    }
    core {
        dependsOn 'jquery'
        resource url: '/js/jquery/jquery.i18n.properties-min-1.0.9.js'
        resource url: '/js/otp.js'
    }
    editorSwitch {
        dependsOn 'core'
        resource url: '/js/editorswitch.js'
    }
    changeLog {
        dependsOn 'jquery-ui, core'
        resource url: '/js/changelog.js'
    }
    crashRecovery {
        dependsOn 'jquery-ui, core'
        resource url: '/js/crashrecovery.js'
    }
    userAdministration {
        dependsOn 'jquery-ui, core'
        resource url: '/js/useradministration.js'
    }
    graphDracula {
        dependsOn 'jquery'
        resource url: '/js/dracula/raphael-min.js'
        resource url: '/js/dracula/dracula_graffle.js'
        resource url: '/js/dracula/dracula_graph.js'
    }
}

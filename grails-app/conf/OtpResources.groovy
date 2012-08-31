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
    qunit {
        resource url: '/css/qunit.css'
        resource url: '/js/qunit.js'
    }
    testSuite {
        dependsOn 'qunit, userAdministration'
        resource url: '/js/test/group-creation-dialog-tests.js'
    }
    lightbox {
        dependsOn 'jquery'
        resource url: '/js/lightbox/js/lightbox.js'
        resource url: '/js/lightbox/css/lightbox.css'
        resource url: '/js/lightbox/images/close.png'
        resource url: '/js/lightbox/images/loading.gif'
        resource url: '/js/lightbox/images/next.png'
        resource url: '/js/lightbox/images/prev.png'
        resource url: '/js/lightbox/images/bg-checker.png'
        resource url: '/js/lightbox/images/box.png'
        resource url: '/js/lightbox/images/bullet.gif'
        resource url: '/js/lightbox/images/donate.png'
        resource url: '/js/lightbox/images/speech-bubbles.png'
        resource url: '/js/lightbox/images/favicon.gif'
    }
}

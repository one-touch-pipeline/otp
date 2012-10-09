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
    jqueryUI {
        dependsOn 'jquery'
        resource url: '/js/jquery/jquery-ui.js'
        resource url: '/css/jquery/jquery-ui.css'
    }
    jqueryDatatables {
        dependsOn 'jquery'
        resource url: '/js/jquery/dataTables.js'
        resource url: '/css/jquery/demo_table.css'
    }
    editorSwitch {
        dependsOn 'core'
        resource url: '/js/editorswitch.js'
    }
    changeLog {
        dependsOn 'jqueryUI, core'
        resource url: '/js/changelog.js'
    }
    crashRecovery {
        dependsOn 'jqueryUI, core'
        resource url: '/js/crashrecovery.js'
    }
    userAdministration {
        dependsOn 'jqueryUI, core'
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
        resource url: '/js/test/format-timespan-tests.js'
    }
    lightbox {
        dependsOn 'jquery'
        resource url: '/js/jquery/lightbox.js'
        resource url: '/css/jquery/lightbox.css'
    }
}

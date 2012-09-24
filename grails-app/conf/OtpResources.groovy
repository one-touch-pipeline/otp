modules = {
    application {
    }
    'style' {
        resource url:'/css/otp.less'
    }
    core {
        dependsOn 'jquery'
        resource url: '/js/jquery/jquery.i18n.properties-min-1.0.9.js'
        resource url: '/js/jquery/jquery.form.js'
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
    jqueryValidation {
        dependsOn 'jquery'
        resource url: '/js/jquery/validation/jquery.validate.min.js'
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
        dependsOn 'jqueryUI, jqueryValidation, core'
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
        resource url: '/js/jquery/qunit.js'
    }
    testSuite {
        dependsOn 'qunit, userAdministration, workflows'
        resource url: '/js/test/group-creation-dialog-tests.js'
        resource url: '/js/test/format-timespan-tests.js'
        resource url: '/js/test/create-link-tests.js'
    }
    lightbox {
        dependsOn 'jquery'
        resource url: '/js/jquery/lightbox.js'
        resource url: '/css/jquery/lightbox.css'
    }
    workflows {
        dependsOn 'jqueryDatatables, core, graphDracula, jqueryUI'
        resource url: '/js/workflows.js'
    }
    editSamples {
        dependsOn 'core'
        resource url: '/js/editsamples.js'
    }
}

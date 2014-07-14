modules = {
    application {
    }
    'style' {
        resource url:'/css/otp.less'
    }
    'info' {
        dependsOn 'jquery'
        resource url: '/js/login.js'
        resource url: '/css/info.less', attrs:[rel: 'stylesheet/less', type:'css']
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
        //DataTables 1.10.0
        dependsOn 'jquery'
        resource url: '/css/dataTable/jquery.dataTables.css'
        resource url: '/css/dataTable/jquery.dataTables_themeroller.css'
        resource url: '/css/dataTable/dataTables.fixedColumns.css'
        resource url: '/css/dataTable/dataTables.fixedHeader.css'
        resource url: '/css/dataTable/dataTables.scroller.css'
        resource url: '/css/dataTable/dataTables.tableTools.css'
        resource url: '/js/dataTable/jquery.dataTables.js'
        resource url: '/js/dataTable/dataTables.fixedColumns.js'
        resource url: '/js/dataTable/dataTables.fixedHeader.js'
        resource url: '/js/dataTable/dataTables.scroller.js'
        resource url: '/js/dataTable/dataTables.tableTools.js'
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
    rGraph {
        dependsOn 'core'
        resource url:'/js/rgraph/RGraph.bar.js'
        resource url:'/js/rgraph/RGraph.common.annotate.js'
        resource url:'/js/rgraph/RGraph.common.context.js'
        resource url:'/js/rgraph/RGraph.common.core.js'
        resource url:'/js/rgraph/RGraph.common.dynamic.js'
        resource url:'/js/rgraph/RGraph.common.effects.js'
        resource url:'/js/rgraph/RGraph.common.key.js'
        resource url:'/js/rgraph/RGraph.common.resizing.js'
        resource url:'/js/rgraph/RGraph.common.tooltips.js'
        resource url:'/js/rgraph/RGraph.common.zoom.js'
        resource url:'/js/rgraph/RGraph.hbar.js'
        resource url:'/js/rgraph/RGraph.line.js'
        resource url:'/js/rgraph/RGraph.pie.js'
        resource url:'/js/rgraph/RGraph.scatter.js'
        resource url:'/js/rgraph/RGraph.waterfall.js'
    }
    graph {
        dependsOn 'rGraph'
        resource url:'/js/graph.js'
    }
    defaultPageDependencies {
        dependsOn 'core'
        dependsOn 'jquery'
        dependsOn 'jqueryDatatables'
        dependsOn 'jqueryUI'
    }
}

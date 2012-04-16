includeTargets << grailsScript("_GrailsCompile")

eventSetClasspath = { rootLoader ->
    classpathSet = false
    println "Set Classpath handler"
    String buildDir = "./ast-build"
    String astLib = "./ast-build/ast.jar"
    ant.delete(dir: buildDir)
    ant.mkdir(dir: buildDir)
    ant.groovyc(srcdir: "./ast", destdir: buildDir)
    ant.jar(destfile: astLib, basedir: buildDir) {
        ant.service(type: "org.codehaus.groovy.transform.ASTTransformation") {
            ant.provider(classname: "de.dkfz.tbi.otp.job.ast.DecisionJobTransformation")
            ant.provider(classname: "de.dkfz.tbi.otp.job.ast.JobTransformation")
            ant.provider(classname: "de.dkfz.tbi.otp.job.ast.StartJobTransformation")
        }
    }
    grailsSettings.compileDependencies << new File(astLib)
    rootLoader.addURL(new File(astLib).toURI().toURL())

    classpath()
}

eventCompileStart = {
    // copy the messages.properties
    ant.mkdir(dir: "web-app/js/i18n/")
    ant.copy(file: "grails-app/i18n/messages.properties", todir: "web-app/js/i18n/")
}

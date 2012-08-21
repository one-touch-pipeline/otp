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
    ant.copy(file: "ast/de/dkfz/tbi/otp/job/scheduler/JobExecution.groovy", tofile: "src/groovy/de/dkfz/tbi/otp/job/scheduler/JobExecution.groovy")

    def proc = "git rev-parse --short HEAD".execute()
    proc.waitFor()
    ant.mkdir(dir: "grails-app/views/templates/")
    FileOutputStream out = new FileOutputStream("grails-app/views/templates/_version.gsp", false)
    out << proc.in.text
    out << " ("
    // get the branch name
    proc = "git symbolic-ref HEAD".execute()
    proc.waitFor()
    String branchName = proc.in.text
    if (branchName.startsWith("refs/heads/")) {
        branchName = branchName.substring(11, branchName.length()-1)
    }
    out << branchName
    out << ")"
}

eventCompileEnd = {
    ant.delete(file: "src/groovy/de/dkfz/tbi/otp/job/scheduler/JobExecution.groovy")
}

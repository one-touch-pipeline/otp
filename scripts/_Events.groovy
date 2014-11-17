includeTargets << grailsScript("_GrailsCompile")

eventSetClasspath = { rootLoader ->
    classpathSet = false
    println "Set Classpath handler"
    String buildDir = "./ast-build"
    String astLib = "./ast-build/ast.jar"
    // Delete the *contents* of the ast-build directory only, such that ast-build can be (and stay) a symlink.
    ant.delete {
        ant.fileset(dir: buildDir, includes: "**/*")
    }
    ant.mkdir(dir: buildDir)
    ant.groovyc(srcdir: "./ast", destdir: buildDir)
    ant.jar(destfile: astLib, basedir: buildDir) {
        ant.service(type: "org.codehaus.groovy.transform.ASTTransformation") {
            ant.provider(classname: "de.dkfz.tbi.otp.job.ast.JobTransformation")
            ant.provider(classname: "de.dkfz.tbi.otp.job.ast.StartJobTransformation")
        }
    }
    grailsSettings.compileDependencies << new File(astLib)
    rootLoader.addURL(new File(astLib).toURI().toURL())

    classpath()
}

eventCompileStart = {

    // Compile the test helper classes if we are in the test environment. Related improvement request: GRAILS-7750
    if (grailsEnv == "test") {
        projectCompiler.srcDirectories << "test/helper"
    }

    // copy the messages.properties
    ant.mkdir(dir: "web-app/js/i18n/")
    ant.copy(file: "grails-app/i18n/messages.properties", todir: "web-app/js/i18n/")

    def proc = "git rev-parse --short HEAD".execute()
    proc.waitFor()
    ant.mkdir(dir: "grails-app/views/templates/")
    FileOutputStream out = new FileOutputStream("grails-app/views/templates/_version.gsp", false)
    out << proc.in.text
    out << " ("
    // get the branch name
    // first check for an environment variable
    // the GIT_BRANCH variable is available in Jenkins
    String branchName = System.getenv("GIT_BRANCH")
    if (!branchName) {
        // not specified through environment - try to resolve through git
        proc = "git symbolic-ref HEAD".execute()
        proc.waitFor()
        branchName = proc.in.text
    }
    if (branchName.startsWith("refs/heads/")) {
        branchName = branchName.substring(11, branchName.length()-1)
    }
    if (branchName.startsWith("origin/")) {
        branchName = branchName.substring(7)
    }
    out << branchName
    out << ")"
}

eventCompileEnd = {
}

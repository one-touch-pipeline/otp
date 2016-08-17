includeTargets << grailsScript("_GrailsCompile")

eventTestPhaseStart = { args ->
    // Save the name of the test phase (unit, integration, ...) in a system property
    // to be able to access it later (while bootstrapping or setting the data source)
    System.properties['grails.test.phase'] = args
}

eventCompileStart = { target ->
    createFileWithGitVersionAndBranch()

    // Compile the test helper classes if we are in the test environment. Related improvement request: GRAILS-7750
    if (grailsEnv == "test" || grailsEnv == "WORKFLOW_TEST") {
        projectCompiler.srcDirectories << "test/helper"
    }
}

def createFileWithGitVersionAndBranch() {
    def proc = "git rev-parse --short HEAD".execute()
    proc.waitFor()
    String revision = proc.in.text.trim()
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
    if (branchName.startsWith("production/")) {
        branchName = "Version ${branchName.substring(11)}"
    } else {
        branchName = "Branch ${branchName}"
    }
    ant.mkdir(dir: "grails-app/views/templates/")
    new File("grails-app/views/templates/_version.gsp").text = "${branchName} (${revision})"
}

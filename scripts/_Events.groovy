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
    String CI_COMMIT_REF_NAME = System.getenv("CI_COMMIT_REF_NAME")
    String CI_COMMIT_SHA = System.getenv("CI_COMMIT_SHA")
    String version, branch, revision
    if (CI_COMMIT_REF_NAME && CI_COMMIT_SHA) {
        revision = CI_COMMIT_SHA.substring(0, 10)
        if (CI_COMMIT_REF_NAME.startsWith("production")) {
            version = "Version ${CI_COMMIT_REF_NAME.substring(11)}"
        } else {
            branch = "Branch ${CI_COMMIT_REF_NAME}"
        }
    } else {
        def proc = "git rev-parse --short HEAD".execute()
        proc.waitFor()
        revision = proc.in.text.trim()

        proc = "git symbolic-ref HEAD".execute()
        proc.waitFor()
        branch = proc.in.text
        if (branch.startsWith('refs/heads/')) {
            branch = branch.substring(11)
        }
        branch = "Branch ${branch}"
    }

    ant.mkdir(dir: "grails-app/views/templates/")
    new File("grails-app/views/templates/_version.gsp").text = "${version ?: branch} (${revision})"
}

package de.dkfz.tbi.otp.ngsdata

import org.junit.After
import org.junit.Before
import org.junit.Test

class ProjectProgressServiceTests {

    ProjectProgressService projectProgressService

    TestData testData

    List<Project> projects

    Run run1, run2, run3

    @Before
    void setUp() {
        testData = new TestData()
        testData.createObjects()

        Project project = DomainFactory.createProject().save(flush: true)
        projects = Project.list()

        run1 = createRunWithDatafile(project, 5)
        run2 = createRunWithDatafile(project, 7)
        run3 = createRunWithDatafile(project, 8)
    }

    @After
    void tearDown() {
        testData = null
        projects = null
        run1 = run2 = run3 = null
    }

    private Run createRunWithDatafile(Project project, int month) {
        Run run = testData.createRun([ project: project, name: "Run${month}"]).save(flush: true)
        testData.createDataFile([run: run, project: project, dateFileSystem: new Date(2000, month, 2)]).save(flush: true)
        return run
    }

    @Test
    void testListOfRuns_NoDataFilesInDateRange() {
        Date date = new Date(2000, 9, 2)
        Date toDate = new Date(2000, 10, 2)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 0 == runs.size()
    }

    @Test
    void testListOfRuns_AllDataFilesInDateRange() {
        Date date = new Date(2000, 2, 2)
        Date toDate = new Date(2000, 10, 2)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 3 == runs.size()
        assert runs.contains(run1)
        assert runs.contains(run2)
        assert runs.contains(run3)
    }

    @Test
    void testListOfRuns_OneDataFilesInDateRange() {
        Date date = new Date(2000, 6, 2)
        Date toDate = new Date(2000, 8, 1)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 1 == runs.size()
        assert runs.contains(run2)
    }

    @Test
    void testListOfRuns_ReverseDataRange() {
        Date date = new Date(2000, 10, 2)
        Date toDate = new Date(2000, 2, 1)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 0 == runs.size()
    }

    @Test
    void testSamples() {
        List<Project> projects = Project.list()
        Date date = new Date() - 50
        Date toDate = new Date()
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        for (Run run in runs) {
            Set<String> samples = projectProgressService.getSamples(run)
            println samples
        }
    }

    @Test
    void testProject() {
        List<String> names = Project.list()*.name
        List<Project> projects = projectProgressService.getProjectsFromNameList(names)
        println projects
    }
}

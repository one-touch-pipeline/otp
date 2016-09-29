package de.dkfz.tbi.otp.ngsdata

import org.junit.After
import org.junit.Before
import org.junit.Test

class ProjectProgressServiceTests {

    ProjectProgressService projectProgressService

    List<Project> projects

    Run run1, run2, run3

    @Before
    void setUp() {
        run1 = createRunWithDatafile(5)
        run2 = createRunWithDatafile(7)
        run3 = createRunWithDatafile(8)

        projects = Project.list()
    }

    @After
    void tearDown() {
        projects = null
        run1 = run2 = run3 = null
    }

    private Run createRunWithDatafile(int month) {
        Run run = DomainFactory.createRun([name: "Run${month}"])
        DomainFactory.createDataFile([run: run, dateFileSystem: new Date(2000, month, 2)])
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
        }
    }

    @Test
    void testProject() {
        List<String> names = Project.list()*.name
        List<Project> projects = projectProgressService.getProjectsFromNameList(names)
    }
}

package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class ProjectProgressServiceTests {

    def projectProgressService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testListOfRuns() {
        List<Project> projects = Project.list()
        Date date = new Date() - 50
        List<Run> runs = projectProgressService.getListOfRuns(projects, date)
        for (Run run in runs) {
            println run.name
        }
    }

    @Test
    void testSamples() {
        List<Project> projects = Project.list()
        Date date = new Date() - 50
        List<Run> runs = projectProgressService.getListOfRuns(projects, date)
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

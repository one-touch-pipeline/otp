/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.After
import org.junit.Test

import de.dkfz.tbi.otp.project.Project

import java.time.temporal.ChronoUnit
import java.time.Instant

@Rollback
@Integration
class ProjectProgressServiceTests {

    ProjectProgressService projectProgressService

    List<Project> projects

    Run run1, run2, run3

    void setupData() {
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
        setupData()
        Date date = new Date(2000, 9, 2)
        Date toDate = new Date(2000, 10, 2)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 0 == runs.size()
    }

    @Test
    void testListOfRuns_AllDataFilesInDateRange() {
        setupData()
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
        setupData()
        Date date = new Date(2000, 6, 2)
        Date toDate = new Date(2000, 8, 1)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 1 == runs.size()
        assert runs.contains(run2)
    }

    @Test
    void testListOfRuns_ReverseDataRange() {
        setupData()
        Date date = new Date(2000, 10, 2)
        Date toDate = new Date(2000, 2, 1)
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        assert 0 == runs.size()
    }

    @Test
    void testSamples() {
        setupData()
        List<Project> projects = Project.list()
        Date date = Date.from(Instant.now().minus(50, ChronoUnit.DAYS))
        Date toDate = new Date()
        List<Run> runs = projectProgressService.getListOfRuns(projects, date, toDate)
        for (Run run in runs) {
            projectProgressService.getSamples(run)
        }
    }

    @Test
    void testProject() {
        setupData()
        List<String> names = Project.list()*.name
        projectProgressService.getProjectsFromNameList(names)
    }
}

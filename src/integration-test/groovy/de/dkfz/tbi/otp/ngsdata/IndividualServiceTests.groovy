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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.joda.time.DateTime
import org.joda.time.DateTimeUtils
import org.junit.Test
import org.springframework.security.access.AccessDeniedException

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles

import static org.junit.Assert.*

@Rollback
@Integration
class IndividualServiceTests implements UserAndRoles {
    IndividualService individualService
    private static long ARBITRARY_TIMESTAMP = 1337

    void setupData() {
        createUserAndRoles()
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testGetIndividualByName() {
        setupData()
        Individual individual = mockIndividual()
        // a user should not be able to get the Individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual("test")
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual("test1"))
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // operator user should find this individual
            assertSame(individual, individualService.getIndividual("test"))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual("test1"))
        }
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual("test"))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual("test1"))
            // grant read to testuser
            addUserWithReadAccessToProject(User.findByUsername(TESTUSER), individual.project)
        }
        // now the user should be allowed to read this individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertSame(individual, individualService.getIndividual("test"))
        }
        // but a different user should still not be allowed to get the Individual
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual("test")
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual("test1"))
        }
    }

    @Test
    void testGetIndividualById() {
        setupData()
        Individual individual = mockIndividual()
        // a user should not be able to get the Individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual(individual.id)
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual(individual.id + 1))
        }
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual(individual.id))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual(individual.id + 1))
        }
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // admin user should find this individual
            assertSame(individual, individualService.getIndividual(individual.id))
            // searching for something else should not retrieve this one
            assertNull(individualService.getIndividual(individual.id + 1))
            // grant read to testuser
            addUserWithReadAccessToProject(User.findByUsername(TESTUSER), individual.project)
        }
        // now the user should be allowed to read this individual
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertSame(individual, individualService.getIndividual(individual.id))
        }
        // but a different user should still not be allowed to get the Individual
        SpringSecurityUtils.doWithAuth(USER) {
            TestCase.shouldFail(AccessDeniedException) {
                individualService.getIndividual(individual.id)
            }
            // but trying to access an individual that does not exist should work
            assertNull(individualService.getIndividual(individual.id + 1))
        }
    }


    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualAsAdmin() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("test")
        Project project2 = mockProject("test2")
        Project project3 = mockProject("test3")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())

        SpringSecurityUtils.doWithAuth(ADMIN) {
            // without any constraints we should get all individuals
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)
        }
    }

    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualAsOperator() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("test")
        Project project2 = mockProject("test2")
        Project project3 = mockProject("test3")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // without any constraints we should get all individuals
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)
        }
    }

    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualWithFilterAsAdmin() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("project")
        Project project2 = mockProject("project1")
        Project project3 = mockProject("project2")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // no filter should not change anything
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // filter with one character should not change anything
            assertEquals(15, individualService.countIndividual("1"))
            results = individualService.listIndividuals(0, 15, true, 0, "1")
            assertEquals(15, results.size())
            // filter with two characters should not change anything
            assertEquals(15, individualService.countIndividual("10"))
            results = individualService.listIndividuals(0, 15, true, 0, "10")
            assertEquals(15, results.size())
            // filter with three characters should start the filter, we filter on individual15 through the pid
            assertEquals(1, individualService.countIndividual("109"))
            results = individualService.listIndividuals(0, 15, true, 0, "109")
            assertEquals(1, results.size())
            assertSame(individual15, results[0])

            // let's test the sorting by filter on a text that will include all results
            assertEquals(15, individualService.countIndividual("test"))
            results = individualService.listIndividuals(0, 15, true, 0, "test")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "test")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "test")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "test")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "test")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "test")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "test")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)

            // now test filter on various elements, pid was already tested
            // try filter on project name
            assertEquals(5, individualService.countIndividual("project1"))
            results = individualService.listIndividuals(0, 15, false, 0, "project1")
            assertEquals(5, results.size())
            results.each {
                assertSame(project2, it.project)
            }
            // try filter on mock full name
            assertEquals(1, individualService.countIndividual("testh"))
            results = individualService.listIndividuals(0, 15, false, 0, "testh")
            assertEquals(1, results.size())
            assertSame(individual8, results[0])
            // try filter on mock pid
            assertEquals(1, individualService.countIndividual("asgd"))
            results = individualService.listIndividuals(0, 15, false, 0, "asgd")
            assertEquals(1, results.size())
            assertSame(individual10, results[0])
            // try filter on type
            assertEquals(4, individualService.countIndividual("cell"))
            results = individualService.listIndividuals(0, 15, false, 0, "cell")
            assertEquals(4, results.size())
            results.each {
                assertSame(Individual.Type.CELLLINE, it.type)
            }
            // try a combined filter
            assertEquals(3, individualService.countIndividual("%g%"))
            results = individualService.listIndividuals(0, 15, false, 0, "%g%")
            assertEquals(3, results.size())
        }
    }

    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualWithFilterAsOperator() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("project")
        Project project2 = mockProject("project1")
        Project project3 = mockProject("project2")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            // no filter should not change anything
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // filter with one character should not change anything
            assertEquals(15, individualService.countIndividual("1"))
            results = individualService.listIndividuals(0, 15, true, 0, "1")
            assertEquals(15, results.size())
            // filter with two characters should not change anything
            assertEquals(15, individualService.countIndividual("10"))
            results = individualService.listIndividuals(0, 15, true, 0, "10")
            assertEquals(15, results.size())
            // filter with three characters should start the filter, we filter on individual15 through the pid
            assertEquals(1, individualService.countIndividual("109"))
            results = individualService.listIndividuals(0, 15, true, 0, "109")
            assertEquals(1, results.size())
            assertSame(individual15, results[0])

            // let's test the sorting by filter on a text that will include all results
            assertEquals(15, individualService.countIndividual("test"))
            results = individualService.listIndividuals(0, 15, true, 0, "test")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "test")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "test")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "test")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "test")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "test")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "test")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)

            // now test filter on various elements, pid was already tested
            // try filter on project name
            assertEquals(5, individualService.countIndividual("project1"))
            results = individualService.listIndividuals(0, 15, false, 0, "project1")
            assertEquals(5, results.size())
            results.each {
                assertSame(project2, it.project)
            }
            // try filter on mock full name
            assertEquals(1, individualService.countIndividual("testh"))
            results = individualService.listIndividuals(0, 15, false, 0, "testh")
            assertEquals(1, results.size())
            assertSame(individual8, results[0])
            // try filter on mock pid
            assertEquals(1, individualService.countIndividual("asgd"))
            results = individualService.listIndividuals(0, 15, false, 0, "asgd")
            assertEquals(1, results.size())
            assertSame(individual10, results[0])
            // try filter on type
            assertEquals(4, individualService.countIndividual("cell"))
            results = individualService.listIndividuals(0, 15, false, 0, "cell")
            assertEquals(4, results.size())
            results.each {
                assertSame(Individual.Type.CELLLINE, it.type)
            }
            // try a combined filter
            assertEquals(3, individualService.countIndividual("%g%"))
            results = individualService.listIndividuals(0, 15, false, 0, "%g%")
            assertEquals(3, results.size())
        }
    }

    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualWithACL() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("test")
        Project project2 = mockProject("test2")
        Project project3 = mockProject("test3")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())

        // no ACL defined, testuser should not see anything
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertEquals(0, individualService.countIndividual(""))
            assertEquals(0, individualService.listIndividuals(0, 15, true, 0, "").size())
        }

        SpringSecurityUtils.doWithAuth(ADMIN) {
            // let's grant privs for one of the projects
            aclUtilService.addPermission(project1, TESTUSER, BasePermission.READ)
        }
        // now testuser should be able to see those
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertEquals(5, individualService.countIndividual(""))
            assertEquals(5, individualService.listIndividuals(0, 15, true, 0, "").size())
        }
        // other user should not be able to see something
        SpringSecurityUtils.doWithAuth(USER) {
            assertEquals(0, individualService.countIndividual(""))
            assertEquals(0, individualService.listIndividuals(0, 15, true, 0, "").size())
        }

        // now grant privs to the other projects
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // let's grant privs for one of the projects
            aclUtilService.addPermission(project2, TESTUSER, BasePermission.READ)
            aclUtilService.addPermission(project3, TESTUSER, BasePermission.READ)
        }
        // now the same tests as in the admin case
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            // without any constraints we should get all individuals
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)
        }
    }

    /**
     * Method individualService.listIndividuals has changed that this need to be adapted.
     */
    @Ignore
    @Test
    void testListIndividualWithFilterAndACL() {
        setupData()
        // let's create a few Projects and Individuals
        Project project1 = mockProject("project")
        Project project2 = mockProject("project1")
        Project project3 = mockProject("project2")
        Individual individual1 = new Individual(pid: "123", mockPid: "1234", mockFullName: "testa", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual1.save())
        Individual individual2 = new Individual(pid: "122", mockPid: "6234", mockFullName: "testb", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual2.save())
        Individual individual3 = new Individual(pid: "121", mockPid: "4758", mockFullName: "testc", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual3.save())
        Individual individual4 = new Individual(pid: "120", mockPid: "4623", mockFullName: "testd", project: project1, type: Individual.Type.UNDEFINED)
        assertNotNull(individual4.save())
        Individual individual5 = new Individual(pid: "119", mockPid: "1243", mockFullName: "teste", project: project2, type: Individual.Type.REAL)
        assertNotNull(individual5.save())
        Individual individual6 = new Individual(pid: "118", mockPid: "1843", mockFullName: "testf", project: project3, type: Individual.Type.POOL)
        assertNotNull(individual6.save())
        Individual individual7 = new Individual(pid: "117", mockPid: "0946", mockFullName: "testg", project: project1, type: Individual.Type.CELLLINE)
        assertNotNull(individual7.save())
        Individual individual8 = new Individual(pid: "116", mockPid: "2462", mockFullName: "testh", project: project2, type: Individual.Type.UNDEFINED)
        assertNotNull(individual8.save())
        Individual individual9 = new Individual(pid: "115", mockPid: "5678", mockFullName: "testi", project: project3, type: Individual.Type.REAL)
        assertNotNull(individual9.save())
        Individual individual10 = new Individual(pid: "114", mockPid: "asgd", mockFullName: "testj", project: project1, type: Individual.Type.POOL)
        assertNotNull(individual10.save())
        Individual individual11 = new Individual(pid: "113", mockPid: "tyew", mockFullName: "testk", project: project2, type: Individual.Type.CELLLINE)
        assertNotNull(individual11.save())
        Individual individual12 = new Individual(pid: "112", mockPid: "bhrs", mockFullName: "testl", project: project3, type: Individual.Type.UNDEFINED)
        assertNotNull(individual12.save())
        Individual individual13 = new Individual(pid: "111", mockPid: "3477", mockFullName: "testm", project: project1, type: Individual.Type.REAL)
        assertNotNull(individual13.save())
        Individual individual14 = new Individual(pid: "110", mockPid: "awrs", mockFullName: "testn", project: project2, type: Individual.Type.POOL)
        assertNotNull(individual14.save())
        Individual individual15 = new Individual(pid: "109", mockPid: "yrgf", mockFullName: "testo", project: project3, type: Individual.Type.CELLLINE)
        assertNotNull(individual15.save())

        // no ACL defined, testuser should not see anything
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertEquals(0, individualService.countIndividual("test"))
            assertEquals(0, individualService.listIndividuals(0, 15, true, 0, "test").size())
        }

        SpringSecurityUtils.doWithAuth(ADMIN) {
            // let's grant privs for one of the projects
            aclUtilService.addPermission(project1, TESTUSER, BasePermission.READ)
        }
        // now testuser should be able to see those
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            assertEquals(5, individualService.countIndividual("test"))
            assertEquals(5, individualService.listIndividuals(0, 15, true, 0, "test").size())
        }
        // other user should not be able to see something
        SpringSecurityUtils.doWithAuth(USER) {
            assertEquals(0, individualService.countIndividual("test"))
            assertEquals(0, individualService.listIndividuals(0, 15, true, 0, "test").size())
        }

        // now grant privs to the other projects
        SpringSecurityUtils.doWithAuth(ADMIN) {
            // let's grant privs for one of the projects
            aclUtilService.addPermission(project2, TESTUSER, BasePermission.READ)
            aclUtilService.addPermission(project3, TESTUSER, BasePermission.READ)
        }
        // now the same tests as in the admin case
        SpringSecurityUtils.doWithAuth(TESTUSER) {
            // no filter should not change anything
            assertEquals(15, individualService.countIndividual(""))
            List<Individual> results = individualService.listIndividuals(0, 15, true, 0, "")
            assertEquals(15, results.size())
            // filter with one character should not change anything
            assertEquals(15, individualService.countIndividual("1"))
            results = individualService.listIndividuals(0, 15, true, 0, "1")
            assertEquals(15, results.size())
            // filter with two characters should not change anything
            assertEquals(15, individualService.countIndividual("10"))
            results = individualService.listIndividuals(0, 15, true, 0, "10")
            assertEquals(15, results.size())
            // filter with three characters should start the filter, we filter on individual15 through the pid
            assertEquals(1, individualService.countIndividual("109"))
            results = individualService.listIndividuals(0, 15, true, 0, "109")
            assertEquals(1, results.size())
            assertSame(individual15, results[0])

            // let's test the sorting by filter on a text that will include all results
            assertEquals(15, individualService.countIndividual("test"))
            results = individualService.listIndividuals(0, 15, true, 0, "test")
            assertEquals(15, results.size())
            // should be ordered ascending by pid
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${109+i}".toString(), ind.pid)
            }
            // let's use same options but order descending
            results = individualService.listIndividuals(0, 15, false, 0, "test")
            assertEquals(15, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${123-i}".toString(), ind.pid)
            }
            // now let's restrict on five elements with offset of three
            results = individualService.listIndividuals(3, 5, true, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${112+i}".toString(), ind.pid)
            }
            // and descending sorting
            results = individualService.listIndividuals(3, 5, false, 0, "test")
            assertEquals(5, results.size())
            results.eachWithIndex { Individual ind, int i ->
                assertEquals("${120-i}".toString(), ind.pid)
            }
            // and a corner case
            results = individualService.listIndividuals(14, 5, false, 0, "test")
            assertEquals(1, results.size())
            assertSame(individual15, results.first())

            // test the sorting by mock full name
            results = individualService.listIndividuals(0, 5, true, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual1, results[0])
            assertSame(individual2, results[1])
            assertSame(individual3, results[2])
            assertSame(individual4, results[3])
            assertSame(individual5, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 1, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual14, results[1])
            assertSame(individual13, results[2])
            assertSame(individual12, results[3])
            assertSame(individual11, results[4])

            // test sorting by mock pid
            results = individualService.listIndividuals(0, 5, true, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual7, results[0])
            assertSame(individual1, results[1])
            assertSame(individual5, results[2])
            assertSame(individual6, results[3])
            assertSame(individual8, results[4])
            // other direction
            results = individualService.listIndividuals(0, 5, false, 2, "test")
            assertEquals(5, results.size())
            assertSame(individual15, results[0])
            assertSame(individual11, results[1])
            assertSame(individual12, results[2])
            assertSame(individual14, results[3])
            assertSame(individual10, results[4])

            // test sorting by project id
            results = individualService.listIndividuals(0, 5, true, 3, "test")
            assertEquals(5, results.size())
            results.each {
                assertSame(project1, it.project)
            }
            // other direction
            results = individualService.listIndividuals(0, 5, false, 3, "test")
            results.each {
                assertSame(project3, it.project)
            }

            // test sorting by type
            results = individualService.listIndividuals(0, 5, true, 4, "test")
            assertEquals(5, results.size())
            assertEquals(Individual.Type.CELLLINE, results[0].type)
            assertEquals(Individual.Type.CELLLINE, results[1].type)
            assertEquals(Individual.Type.CELLLINE, results[2].type)
            assertEquals(Individual.Type.CELLLINE, results[3].type)
            assertEquals(Individual.Type.POOL, results[4].type)
            // other direction
            results = individualService.listIndividuals(0, 5, false, 4, "test")
            assertEquals(Individual.Type.UNDEFINED, results[0].type)
            assertEquals(Individual.Type.UNDEFINED, results[1].type)
            assertEquals(Individual.Type.UNDEFINED, results[2].type)
            assertEquals(Individual.Type.REAL, results[3].type)
            assertEquals(Individual.Type.REAL, results[4].type)

            // now test filter on various elements, pid was already tested
            // try filter on project name
            assertEquals(5, individualService.countIndividual("project1"))
            results = individualService.listIndividuals(0, 15, false, 0, "project1")
            assertEquals(5, results.size())
            results.each {
                assertSame(project2, it.project)
            }
            // try filter on mock full name
            assertEquals(1, individualService.countIndividual("testh"))
            results = individualService.listIndividuals(0, 15, false, 0, "testh")
            assertEquals(1, results.size())
            assertSame(individual8, results[0])
            // try filter on mock pid
            assertEquals(1, individualService.countIndividual("asgd"))
            results = individualService.listIndividuals(0, 15, false, 0, "asgd")
            assertEquals(1, results.size())
            assertSame(individual10, results[0])
            // try filter on type
            assertEquals(4, individualService.countIndividual("cell"))
            results = individualService.listIndividuals(0, 15, false, 0, "cell")
            assertEquals(4, results.size())
            results.each {
                assertSame(Individual.Type.CELLLINE, it.type)
            }
            // try a combined filter
            assertEquals(3, individualService.countIndividual("%g%"))
            results = individualService.listIndividuals(0, 15, false, 0, "%g%")
            assertEquals(3, results.size())
        }
    }

    @Test
    void testCreateCommentString_WhenSeveralPropertiesEqual_ShouldJustAddDifferentOnes() {
        setupData()
        String operation = "operation"
        Map mapA = [a: 1, b: 2, c: 3]
        Map mapB = [a: 1, b: 3, c: 4]
        Date date = new Date()

        assert """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
Old:
b: 2
c: 3
New:
b: 3
c: 4
""" == individualService.createCommentString(operation, mapA, mapB, date, null)
    }

    @Test
    void testCreateCommentString_WhenCommentWithAdditionalInformation_ShouldReturnCorrectString() {
        setupData()
        String operation = "operation"
        Map mapA = [a: 1]
        Map mapB = [a: 2]
        Date date = new Date()
        String additionalInformation = "additional information"

        assert """== operation - ${date.format("yyyy-MM-dd HH:mm")} ==
${additionalInformation}
Old:
a: 1
New:
a: 2
""" == individualService.createCommentString(operation, mapA, mapB, date, additionalInformation)
    }

    @Test
    void testCreateComment_WhenCommentAlreadyExists_ShouldAddNewComment() {
        setupData()
        Individual indOld = DomainFactory.createIndividual(comment: DomainFactory.createComment(comment: "old comment"))
        Individual indNew = DomainFactory.createIndividual()
        String operation = "operation"
        Map mapOld = [individual: indOld]
        Map mapNew = [individual: indNew]

        DateTimeUtils.setCurrentMillisFixed(ARBITRARY_TIMESTAMP)

        SpringSecurityUtils.doWithAuth(OPERATOR) {
            individualService.createComment(operation, mapOld, mapNew)
        }

        assert """== operation - ${new DateTime().toDate().format("yyyy-MM-dd HH:mm")} ==
Old:
individual: ${indOld}
New:
individual: ${indNew}

${indOld.comment.comment}""" == indNew.comment.comment

        DateTimeUtils.setCurrentMillisSystem()
    }

    @Test
    void testCreateComment_WhenParametersNull_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment(null, null, null)
        }
    }

    @Test
    void testCreateComment_WhenMapsHaveDifferentKeySets_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment(null, [individual: DomainFactory.createIndividual(), a: 1], [individual: DomainFactory.createIndividual()])
        }
    }

    @Test
    void testCreateComment_WhenInputMapsContainNoIndividual_ShouldFail() {
        setupData()
        TestCase.shouldFail(AssertionError) {
            individualService.createComment("", [property: null], [property: null])
        }
    }

    private Individual mockIndividual(String pid = "test", Project project = null) {
        Individual ind = new Individual(pid: pid, mockPid: pid, mockFullName: pid, project: project ?: mockProject(), type: Individual.Type.REAL)
        assertNotNull(ind.save())
        return ind
    }

    private Project mockProject(String name = "test") {
        return Project.findOrSaveWhere(
                name: name,
                dirName: name,
                realm: DomainFactory.createRealm(),
                qcThresholdHandling: QcThresholdHandling.NO_CHECK,
        )
    }
}

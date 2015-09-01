package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.ngsdata.Sample

class IdCompareHelperUnitTests {

    void test_hashCode_notSavedObject_shouldReturn0() {
        Project project = new Project()

        assert 0 == IdCompareHelper.hashCode(project)
    }

    void test_hashCode_savedObject_shouldReturnHashOfId() {
        Long id = 4
        Project project = new Project(id: id)

        assert id.hashCode() == IdCompareHelper.hashCode(project)
    }

    void test_hashCode_nullObject_shouldReturn0() {
        assert 0 == IdCompareHelper.hashCode(null)
    }

    void test_equals_bothAreNull_shouldReturnTrue() {
        assert IdCompareHelper.equals(null, null)
    }

    void test_equals_FirstIsNull_shouldReturnFalse() {
        assert !IdCompareHelper.equals(new Project(), null)
    }

    void test_equals_SecondIsNull_shouldReturnFalse() {
        assert !IdCompareHelper.equals(null, new Project())
    }

    void test_equals_bothObjectsAreIdenticalButNotSaved_shouldReturnTrue() {
        Project p = new Project()
        assert IdCompareHelper.equals(p, p)
    }

    void test_equals_bothObjectsOfSameClassAndHaveSameId_shouldReturnTrue() {
        Project project1 = new Project(id: 3)
        Project project2 = new Project(id: 3)
        assert IdCompareHelper.equals(project1, project2)
    }

    void test_equals_twoDifferentUnsavedObjectsOfSameClass_shouldReturnFalse() {
        Project project1 = new Project()
        Project project2 = new Project()
        assert !IdCompareHelper.equals(project1, project2)
    }

    void test_equals_SavedObjectsOfDifferentClassWithSameId_shouldReturnFalse() {
        Project project = new Project(id: 3)
        Sample sample = new Sample(id: 3)
        assert !IdCompareHelper.equals(project, sample)
    }

    void test_equals_twoDifferentSavedObjectsOfSameClass_shouldReturnFalse() {
        Project project1 = new Project(id: 3)
        Project project2 = new Project(id: 4)
        assert !IdCompareHelper.equals(project1, project2)
    }

    void test_equals_FirstObjectIsSavedAndSecondIsNotSaved_shouldReturnFalse() {
        Project project1 = new Project(id: 3)
        Project project2 = new Project()
        assert !IdCompareHelper.equals(project1, project2)
    }

    void test_equals_FirstObjectIsNotSavedAndSecondIsSaved_shouldReturnFalse() {
        Project project1 = new Project(id: 3)
        Project project2 = new Project()
        assert !IdCompareHelper.equals(project1, project2)
    }


}

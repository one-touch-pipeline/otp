/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.alignment.cellRanger

class CellRangerAlignmentOneLaneExpectedCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 1, expected: CELLS, enforced: null]
    }
}

class CellRangerAlignmentOneLaneEnforcedCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 1, expected: null, enforced: CELLS]
    }
}

class CellRangerAlignmentOneLaneNeitherCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 1, expected: null, enforced: null]
    }
}

class CellRangerAlignmentTwoLanesExpectedCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 2, expected: CELLS, enforced: null]
    }
}

class CellRangerAlignmentTwoLanesEnforcedCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 2, expected: null, enforced: CELLS]
    }
}

class CellRangerAlignmentTwoLanesNeitherCellsWorkflowTests extends AbstractCellRangerAlignmentWorkflowTests {

    @Override
    Map<String, Integer> getTestParameters() {
        return [nLanes: 2, expected: null, enforced: null]
    }
}

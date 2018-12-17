package de.dkfz.tbi.otp.utils

import groovy.transform.Immutable

@Immutable
class ProcessOutput {
    String stdout
    String stderr
    int exitCode

    ProcessOutput assertExitCodeZero() {
        assert exitCode == 0 : "Expected exit code to be 0, but it is ${exitCode}\nstdout: ${stdout}\nstderr: ${stderr}"
        return this
    }

    ProcessOutput assertStderrEmpty() {
        assert stderr.isEmpty() : "Expected stderr to be empty, but it is ${stderr}"
        return this
    }

    ProcessOutput assertExitCodeZeroAndStderrEmpty() {
        assertStderrEmpty()
        assertExitCodeZero()
        return this
    }
}

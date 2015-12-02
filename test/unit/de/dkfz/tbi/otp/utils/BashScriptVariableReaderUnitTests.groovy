package de.dkfz.tbi.otp.utils

import org.junit.Test
import de.dkfz.tbi.TestCase

class BashScriptVariableReaderUnitTests extends TestCase {

    @Test
    void testExecuteAndGetVariableValues() {
        final Map<String, String> expectedVariables = [
                'CALLING_PBS_RESOURCES': '-l walltime=20:00:00,nodes=1,mem=400m',
                'EO_MAIL_OPTS': '-o $ROOTPATH/DummyPipelines -j oe -M dummy@example.com -m a',
                'RESULTS_PER_PIDS_DIR': 'STORAGE_ROOT/analysis/dummy_project/exome_analysis/results_per_pid',
                'TOOLS_DIR': '/home/dummy_user/ngs2/trunk/tools',
                'MISSING_VARIABLE': '',
        ]
        final Map<String, String> actualVariables = BashScriptVariableReader.executeAndGetVariableValues("""
ANALYSIS_DIR=STORAGE_ROOT/analysis/dummy_project/exome_analysis
RESULTS_PER_PIDS_DIR=\${ANALYSIS_DIR}/results_per_pid
#TOOLS_DIR=/home/dummy_user/ngs2/trunk/fools
TOOLS_DIR=/home/dummy_user/ngs2/trunk/tools
### cluster parameters and resources
CALLING_PBS_RESOURCES="-l walltime=20:00:00,nodes=1,mem=400m"
EMAIL=dummy@example.com
CLUSTER_EO=$ROOTPATH/DummyPipelines # path to your cluster eo directory
EO_MAIL_OPTS="-o \$CLUSTER_EO -j oe -M \$EMAIL -m a"
""", expectedVariables.keySet())
        assert expectedVariables == actualVariables
    }

    @Test
    void testExecuteAndGetVariableValues_failingStatement() {
        try {
            BashScriptVariableReader.executeAndGetVariableValues("""
LANG=C
missing_dummy_command
""", new HashSet(['HELLO']))
            throw new AssertionError('Expected exception has not been thrown.')
        } catch (final RuntimeException e) {
            assert e.message.startsWith('Script failed with exit code 127. Error output:\n' +
                    'bash: line 3: missing_dummy_command: command not found')
        }
    }

    @Test
    void testExecuteAndGetVariableValues_failingAndSucceedingStatements1() {
        final Map<String, String> expectedVariables = [
                'HELLO': 'WORLD',
        ]
        final Map<String, String> actualVariables = BashScriptVariableReader.executeAndGetVariableValues("""
missing_dummy_command
HELLO=WORLD
echo foo
""", expectedVariables.keySet())
        assert expectedVariables == actualVariables
    }

    @Test
    void testExecuteAndGetVariableValues_failingAndSucceedingStatements2() {
        try {
            BashScriptVariableReader.executeAndGetVariableValues("""
set -e
LANG=C
missing_dummy_command
HELLO=WORLD
echo foo
""", new HashSet(['HELLO']))
            throw new AssertionError('Expected exception has not been thrown.')
        } catch (final RuntimeException e) {
            assert e.message.startsWith('Script failed with exit code 127. Error output:\n' +
                    'bash: line 4: missing_dummy_command: command not found')
        }
    }
}

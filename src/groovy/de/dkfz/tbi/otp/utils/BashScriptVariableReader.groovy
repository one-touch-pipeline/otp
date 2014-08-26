package de.dkfz.tbi.otp.utils

import java.util.regex.Matcher

class BashScriptVariableReader {

    /**
     * Executes a Bash script and returns the values of variables which the script sets.
     * @param script A Bash script. (<strong>Not</strong> the path to a script file.)
     * @param variableNames Do not include '$' as a prefix. Use only names that require no escaping.
     * @return A mapping of variable names to variable values.
     */
    static Map<String, String> executeAndGetVariableValues(final String script, final Set<String> variableNames) {

        // The rest of the code in this method assumes that the iteration order over the variable names is stable, so
        // put them into a list.
        final List<String> variableNamesList = new ArrayList<String>(variableNames)

        final StringBuilder scriptWithEchos = new StringBuilder(script)
        scriptWithEchos.append('\nprintf "\\n$?\\n"\n')
        final StringBuilder regex = new StringBuilder("\n(-?[0-9]{1,9})\n")
        variableNamesList.each {
            scriptWithEchos.append("echo ${it}=\$${it}\n")
            regex.append("(${it})=(.*)\n")
        }
        regex.append('$')

        final Process process = 'bash'.execute()
        final CharArrayWriter err = new CharArrayWriter()
        process.consumeProcessErrorStream(err)
        process.withWriter { Writer writer ->
            writer << scriptWithEchos
        }
        process.waitForOrKill(5000L)
        final Closure checkExitCode = { final int exitCode ->
            if (exitCode != 0) {
                throw new RuntimeException("Script failed with exit code ${exitCode}. Error output:\n${err}")
            }
        }
        // process.exitValue() is the exit code of the last command of the *modified* script, i.e. the last echo command
        // generated above, so it will usually be 0. Exception: If 'set -e' is used in the script, it is the exit code
        // of the failed statement if any.
        checkExitCode(process.exitValue())

        final String output = process.text
        final Matcher matcher = output =~ regex.toString()
        if (!matcher.find() || !matcher.hitEnd()) {
            throw new RuntimeException("Output does not match the regular expression.\nOutput:\n${output}\nRegular expression:\n${regex}")
        }
        int group = 1
        // Check the exit code of the last command of the original script. (We printed it to the output.) That would be
        // the exit code of the process if we had not modified the script.
        checkExitCode(Integer.valueOf(matcher.group(group++)))
        final Map variables = [:]
        variableNamesList.each {
            assert matcher.group(group++) == it
            variables.put(it, matcher.group(group++))
        }

        assert variables.keySet() == variableNames
        return variables
    }

}

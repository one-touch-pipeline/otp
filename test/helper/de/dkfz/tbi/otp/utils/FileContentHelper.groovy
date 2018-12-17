package de.dkfz.tbi.otp.utils


class FileContentHelper {

    static String createXmlContentForRoddyWorkflowConfig(String label) {
        return("""
<configuration configurationType='project' name='${label}'>
</configuration>
""")
   }
}

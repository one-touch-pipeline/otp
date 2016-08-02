//Load SNV config file in OTP


import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

Project project = Project.findByName("some project name")
SeqType seqType = SeqType.findByNameAndLibraryLayout("EXON", "PAIRED")
File configFile = new File("path to file")
String externalScriptVersion = 'some version' // i.e. 1.0.166


println project
assert project : "Project does not exist"
println seqType
assert seqType : "SeqType does not exist"
println configFile
println externalScriptVersion
assert configFile.exists() : "Config file does not exist"


//SnvConfig.createFromFile(project, seqType, configFile, externalScriptVersion)
''

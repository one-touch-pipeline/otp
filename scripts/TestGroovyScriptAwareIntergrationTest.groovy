import de.dkfz.tbi.otp.ngsdata.*

println "HELLO"
Project project = new Project(name: "HIPO", dirName: "dirName", realmName: "DKFZ")
project.validate()
project.save()
println "created project ${project}"
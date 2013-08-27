package de.dkfz.tbi.otp.ngsdata

class HomeService {
    ProjectService projectService

    Map projectQuery() {
         List<String> projectNames = projectService.getAllProjects()*.name

         Map queryMap = [:]

         projectNames.each { String projectName ->
             List seq = Sequence.createCriteria().listDistinct {
                 eq("projectName", projectName)
                 projections {
                     groupProperty("seqTypeName")
                 }
                 order ("seqTypeName")
             }
             queryMap[projectName] = seq.toListString().replace("[", "").replace("]", "")
         }
         return queryMap
    }
}

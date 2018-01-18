<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="configureAnalysis.title" args="${[project.name]}"/></title>
    <asset:javascript src="configureThresholds.js"/>
</head>
<body>
    <div class="body">
        <div  style="clear: both">
            <g:form >
                <input name="project.id" type="hidden" value="${project.id}"/>
                <h3><g:message code="configureAnalysis.title" args="${[project.name]}"/></h3>
                <table border="2" class="blue_label">
                    <thead>
                        <tr>
                            <th colspan="2">
                                    </th>
                                <g:each var="seqType" in="${seqTypes}">
                                    <th colspan="2">${seqType}</th>
                                </g:each>
                        </tr>
                        <tr>
                            <th><g:message code="configureAnalysis.sampleTypes"/></th>
                            <th><g:message code="configureAnalysis.typ"/></th>
                                <g:each var="seqType" in="${seqTypes}">
                                    <th><g:message code="configureAnalysis.laneCount"/></th>
                                    <th><g:message code="configureAnalysis.coverage"/></th>
                                </g:each>
                        <tr>
                    </thead>
                    <tbody class="table-body-box">
                        <g:each var="sampleType" in="${sampleTypes}">
                            <tr>
                                <td>${sampleType.name}</td>
                                <td width="3em"> <g:select  name="${project}!${sampleType.name}" from='${categories}' value='${groupedDiseaseTypes[sampleType.id] ? groupedDiseaseTypes[sampleType.id][0].category : de.dkfz.tbi.otp.ngsdata.SampleType.Category.UNDEFINED }' class="dropDown"/> </td>
                                <g:each var="seqType" in="${seqTypes}">
                                    <td width="1em"><g:textField onkeypress="return numberCheck(event);" name="${project}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!numberOfLanes" value="${groupedThresholds.get(sampleType.id)?.get(seqType.id)?.get(0)?.numberOfLanes}"/></td>
                                    <td width="1em"><g:textField onkeypress="return numberCheck(event);" name="${project}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!coverage" value="${groupedThresholds.get(sampleType.id)?.get(seqType.id)?.get(0)?.coverage}"/></td>
                                </g:each>
                            </tr>
                        </g:each>
                    </tbody>
                </table>
                <g:submitButton class="blue_label" name="submit" value="submit" onclick="return submitCheck();" ondblclick="return false;"/>
            </g:form>

            <script>
                function numberCheck(event) {
                    return $.otp.submitThreshold.isNumeric(event);
                };
                function submitCheck(event) {
                    return $.otp.submitThreshold.submitAlert();
                };
            </script>
        </div>
    </div>
</body>
</html>

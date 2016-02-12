<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="main" />
    <title><g:message code="otp.menu.snv.processing" /></title>
    <asset:javascript src="pages/snv/index/snv.js"/>
</head>
<body>
    <div class="body">
        <form class="blue_label" id="projectsGroupbox">
            <span class="blue_label"><g:message
                    code="home.projectfilter" /> :</span>
            <g:select class="criteria" id="projectName" name='projectName'
                from='${projects}' value='${project}'
                onChange='submit();'></g:select>
        </form>
    <div  style="clear: both">
    <g:form >
        <input name="projectName" type="hidden" value="${project}"/>
        <h3> <g:message code="snv.title.configuration"/></h3>
        <table border="2" class="blue_label">
            <thead>
                <tr>
                    <th colspan="2">
                            </th>
                        <g:each var="seqType" in="${alignableSeqType}">
                            <th colspan="2">${seqType}</th>
                        </g:each>
                </tr>
                <tr>
                    <th><g:message code="snv.index.sampleTypes"/></th>
                    <th><g:message code="snv.index.typ"/></th>
                        <g:each var="seqType" in="${alignableSeqType}">
                            <th><g:message code="snv.index.laneCount"/></th>
                            <th><g:message code="snv.index.coverage"/></th>
                        </g:each>
                <tr>
            </thead>
            <tbody class="table-body-box">
                <g:each var="sampleType" in="${sampleTypes}">
                    <tr>
                        <td>${sampleType.name}</td>
                        <td width="3em"> <g:select  name="${project}!${sampleType.name}" from='${categories}' value='${groupedDiseaseTypes[sampleType.id] ? groupedDiseaseTypes[sampleType.id][0].category : de.dkfz.tbi.otp.ngsdata.SampleType.Category.IGNORED }' class="dropDown"/> </td>
                        <g:each var="seqType" in="${alignableSeqType}">
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
            return $.otp.submitSNV.isNumeric(event);
        };
        function submitCheck(event) {
            return $.otp.submitSNV.submitAlert();
        };
</script>
    </div>
    <div style="width: 20px; height: 20px;"></div>
    <h3><g:message code="snv.individual.table"/></h3>
    <div class=" listOfIndividualsForSNV otp">
                     <otp:dataTable
                    codes="${[
                        'snv.index.individual',
                    ] }"
                        id="individualsPerProject" />
        </div>
     <asset:script>
      $(function() {
            $.otp.Snv.registerIndividualIds();
        });
    </asset:script>
    </div>
</body>
</html>

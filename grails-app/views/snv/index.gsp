<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<title><g:message code="otp.menu.snv" /></title>
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
        <table border="2" class="blue_label">
            <thead>
                <tr>
                    <th colspan="2"><g:message
                            code="overview.statistic.seq.name" /></th>
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
                        <td width="3em"> <g:select  name="${project}!${sampleType.name}" from='${categories}' value='${groupDesieseType[sampleType] ? groupDesieseType[sampleType][0].category : de.dkfz.tbi.otp.ngsdata.SampleTypePerProject.Category.UNKNOWN }' class="dropDown"/> </td>
                        <g:each var="seqType" in="${alignableSeqType}">
                            <td width="1em"><g:textField onkeypress="return numberCheck(event);" name="${project}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!numberOfLanes" value="${groupdThresholds.get(sampleType)?.get(seqType)?.get(0)?.numberOfLanes}"/></td>
                            <td width="1em"><g:textField onkeypress="return numberCheck(event);" name="${project}!${sampleType.name}!${seqType.name}!${seqType.libraryLayout}!coverage" value="${groupdThresholds.get(sampleType)?.get(seqType)?.get(0)?.coverage}"/></td>
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
    </div>
</body>
</html>

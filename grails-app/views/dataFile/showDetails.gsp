<%@page import="com.sun.xml.internal.org.jvnet.mimepull.DataFile"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">

    <h1>Details of a Data File</h1>

    <table>
       <tr>
            <td class="myKey">run name</td>
            <td class="myValue"><b>${dataFile.run.name}</b></td>
       </tr>
       <tr>
            <td class="myKey">file name</td>
            <td class="myValue">${dataFile.fileName}</td>
       </tr>
       <g:if test="${!dataFile.prvFilePath.isAllWhitespace()}">
       <tr>
            <td class="myKey">file path from run directory</td>
            <td class="myValue">${dataFile.prvFilePath}</td>
       </tr>
       </g:if>
       <tr>
            <td class="myKey">view by pid name</td>
            <td class="myValue">${dataFile.vbpFileName}</td>
       </tr>
       <g:if test="${!dataFile.vbpFilePath.isAllWhitespace()}">
       <tr>
            <td class="myKey">view by pid path</td>
            <td class="myValue">${dataFile.vbpFilePath}</td>
       </tr>
       </g:if>
       <tr>
            <td class="myKey">check sum (md5)</td>
            <td class="myValue">${dataFile.md5sum}</td>
       </tr>
       <tr>
            <td class="myKey">full path</td>
            <td class="myValue">${values.get(0)}</td>
       </tr>  
       <tr>
            <td class="myKey">view by pid full path</td>
            <td class="myValue">${values.get(1)}</td>
       </tr>
       <tr>
            <td class="myKey">file exists</td>
            <td class="${dataFile.fileExists}">${dataFile.fileExists}</td>
       </tr>
       <tr>
            <td class="myKey">file linked</td>
            <td class="${dataFile.fileLinked}">${dataFile.fileLinked}</td>
       </tr>
       <tr>
            <td class="myKey">file size</td>
            <td class="myValue">${dataFile.fileSizeString()}</td>
       </tr>
    </table>

    <H1>Dates</H1>
    <table>
        <tr>
            <td class="myKey">run execution date</td>
            <td class="myValue">${dataFile.dateExecuted?.format("yyyy-MM-dd")}</td>
       </tr>
       <tr>
            <td class="myKey">file system date</td>
            <td class="myValue">${dataFile.dateFileSystem?.format("yyyy-MM-dd HH:mm")}</td>
       </tr>
       <tr>
            <td class="myKey">database registration date</td>
            <td class="myValue">${dataFile.dateCreated.format("yyyy-MM-dd HH:mm")}</td>
       </tr>
    </table>

   <H1>Meta Data Status</H1>
    <table>
       <tr>
            <td class="myKey">project</td>
            <td class="myValue">${dataFile.project}</td>
       </tr>
       <tr>
            <td class="myKey">is file used</td>
            <td class="${dataFile.used}">${dataFile.used}</td>
       </tr>
       <tr>
            <td class="myKey">is meta-data valid</td>
            <td class="${dataFile.metaDataValid}">${dataFile.metaDataValid}</td>
       </tr>
       <tr>
            <td class="myKey">is withdrawn</td>
            <td class="${dataFile.fileWithdrawn}">${dataFile.fileWithdrawn}</td>
       </tr>

    </table>

    <H1>Meta Data Entries</H1>

    <table>
    <g:each var="metaDataEntry" in="${entries}">
       <tr>
            <td class="myKey">${metaDataEntry.key.name}</td>
            <td class="myValue"}">${metaDataEntry.value}</td>
            <td class="${metaDataEntry.status}">${metaDataEntry.status}</td>
            <td>${metaDataEntry.source}</td>
       </tr>
    </g:each> 
    </table>

  </div>
</body>
</html>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${g.message(code: "metadataImport.details.title")}</title>
</head>

<body>
<div class="body">
    <h3>${g.message(code: "metadataImport.details.multiFiles.headline")}</h3>
    <ul>
    <g:each var="metaDataFile" in="${metaDataFiles}" >
        <li><g:link action="details" id="${metaDataFile.runSegment.id}">${metaDataFile.filePath}/${metaDataFile.fileName}</g:link></li>
    </g:each>
    </ul>
</div>
</body>
</html>
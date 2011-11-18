<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Run details</title>
</head>
<body>
  <div class="body">

    <h1>General</h1>
    
    <table>
       <tr> 
            <td class="myKey">name</td>
            <td class="myValue">${run.name}</td>
       </tr>  
       <tr> 
            <td class="myKey">sequencing center</td>
            <td class="myValue">${run.seqCenter.name.toLowerCase()}</td>
       </tr>
       <tr> 
            <td class="myKey">sequencing technology</td>
            <td class="myValue">${run.seqTech}</td>
       </tr>
       <tr>
            <td class="myKey">date executed</td>
            <td class="myValue">
                ${(new Date(run.dateExecuted.getTime())).format("yyyy-MM-dd")}
            </td>
       </tr>
       <tr>
            <td class="myKey">date created</td>
            <td class="myValue">${run.dateCreated}</td>
       </tr>
       <tr>
            <td class="myKey">data path</td>
            <td class="myValue">${run.dataPath}</td>
       </tr>
       <tr>
            <td class="myKey">meta data path</td>
            <td class="myValue">${run.mdPath}</td>
       </tr>
    </table>


    <h1>Status</h1>
    <table>
       <tr>
            <td class="myKey">is complete</td>
            <td class="myValue">${run.complete}</td>
       </tr>
               <tr>
            <td class="myKey">all files used</td>
            <td class="myValue">${run.allFilesUsed}</td>
       </tr>
       <tr>
            <td class="myKey">data moved to final location</td>
            <td class="myValue">${run.finalLocation}</td>
       </tr>
            <tr>
            <td class="myKey">run from multiple sources</td>
            <td class="myValue">${run.multipleSource}</td>
       </tr>
    </table>


    <div class="myHeader">
        Data Files
    </div>

    <div class="myContent">

        <table>

           <g:each var="file" in="${run.dataFiles}">
           <g:if test="${file.fileType.type.toString() == 'METADATA'}">
           <tr>
                <td>${file.fileName}</td>
                <td>${(new Date(file.dateCreated.getTime())).format("yyyy-MM-dd")}</td>
                <td>${file.fileType.type}</td>
<%--                <td>${file.fileExists}</td>--%>
<%--                <td>${file.used}</td>--%>
           </tr>
           </g:if>
           </g:each>
        </table>

        <table>
        <g:each var="track" in="${run.seqTracks}">

            <tr>
                <td class="miniHeader" colspan="3">${track}</td>
                <td class="miniHeader">${track.insertSize}</td>
                <td class="miniHeader">${track.nBaseString()}</td>
            </tr>
            <g:each var="file" in="${track.dataFiles}">
                <tr>
                    <td>-</td>
                    <td>s</td>
                    <td>${file.fileName}</td>
                    <td>${file.fileSize/1e9} GB</td>
                    <td>
                        <g:if test="${file.metaDataValid}">md valid</g:if>
                        <g:else>md invalid</g:else>
                    </td>
                </tr>
            </g:each>
            <g:each var="alignment" in="${track.alignmentLog}">
                <g:each var="file" in="${alignment.dataFiles}">
                    <tr>
                        <td>-</td>
                        <td>a</td>
                        <td>${file.fileName}</td>
                        <td>${alignment.alignmentParams.programName}
                        <td>${alignment.executedBy}</td>
                    </tr>
                </g:each>
            </g:each> 
          </g:each>
        </table>
      </div>


  </div>
</body>
</html>
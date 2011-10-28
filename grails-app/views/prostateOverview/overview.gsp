<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<meta name="layout" content="main"/>
<title>Insert title here</title>
</head>
<body>
  <div class="body">
 	
 	<table>
 	
 		<g:each var="head" in="${header}">
 			<th colspan=3>${head}
 		</g:each>

 	<g:each var="name" in="${names}"> 
 		<tr><th rowspan=3 class="sample">${name}
 	 		
 	 	<g:each var="type" in ="${types}">
 	 	
 	 		<tr class="${type}">	
 	 		<td> &nbsp;	
 	 		<td>${type.toLowerCase()}
 	 		
 	 		<g:each var="key" in="${keys}">

 			<td> 
 				<g:if test="${table[name][type] != null && table[name][type][key] != null}">
 					<g:link controller="seqScan" action="show" id="${table[name][type][key].id}">
 						${table[name][type][key]?.center}
 					</g:link>  
 				</g:if>
 			</td>
 			
 			<td> 
 				<g:if test="${table[name][type] != null && table[name][type][key] != null}">
 					<g:link controller="seqScan" action="show" id="${table[name][type][key].id}">
 						${table[name][type][key]?.lanes}
 					</g:link>  
 				</g:if>
 			</td>
 		
 			<td>
 				<g:if test="${table[name][type] != null && table[name][type][key] != null}">
 					<g:link controller="seqScan" action="show" id="${table[name][type][key]?.id}">
 					
 						<div class="${table[name][type][key]?.qav}">
 						${table[name][type][key]?.qa}
                        </div>
 					</g:link>
 				</g:if>	
 			</td>
 		
 			</g:each>
 		</g:each>

		<tr><td class="tinyRow">&nbsp;
		
 	</g:each>
 
 	</table>
 
  </div>
</body>
</html>
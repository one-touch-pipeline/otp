<div title="Output parameters">
    <p>
    Please specify the output parameters of the failed Job.
    </p>
    <ul>
        <g:each var="parameter" in="${parameters}">
            <li>
                <h3>${parameter.name}</h3>
                <g:if test="${parameter.description}">
                    <p>${parameter.description}</p>
                </g:if>
                <g:if test="${parameter.className}">
                    <p>Parameter references domain object of type ${parameter.className}. Please enter the object id.</p>
                </g:if>
                <p>
                    <input type="text" name="${parameter.id}"/>
                </p>
            </li>
        </g:each>
    </ul>
</div>

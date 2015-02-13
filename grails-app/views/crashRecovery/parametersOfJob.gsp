<div title="Output parameters">
    <p>
        Please specify the output parameters of the failed Jobs.
    </p>
    <ul class="crashRecoveryDialog">
        <g:each var="parametersPerJob" in="${parametersPerJobs}">
            <li>
                <div class='jobName'>Job: ${parametersPerJob.jobName} (${parametersPerJob.id})</div>
                <g:if test="${parametersPerJob.parameter.isEmpty()}">
                    The job has no output parameter
                </g:if>
                <g:else>
                    <ul>
                        <g:each var="parameter" in="${parametersPerJob.parameter}">
                            <li>
                                <div>${parameter.name}</div>
                                <g:if test="${parameter.description}">
                                    <p>${parameter.description}</p>
                                </g:if>
                                <g:if test="${parameter.className}">
                                    <p>Parameter references domain object of type ${parameter.className}. Please enter the object id.</p>
                                </g:if>
                                <p>
                                    <input type="text" name="${parametersPerJob.id}!${parameter.id}"/>
                                </p>
                            </li>
                        </g:each>
                    </ul>
                </g:else>
            </li>
        </g:each>
    </ul>
</div>

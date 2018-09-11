% for version in data["versions"]:
% for section in version["sections"]:
% if not (section["label"] == "Other" and nb_sections == 1):

${section["label"]}
${"~" * len(section["label"])}
% endif
% for commit in section["commits"]:
% if commit["body"]:
<%
    import re
    cleaned_body = "\n".join(filter(lambda x: not re.match(r'^\s*$', x), commit["body"].split("\n")))

    entry = cleaned_body
%>${entry}
% endif
% endfor
% endfor

% endfor

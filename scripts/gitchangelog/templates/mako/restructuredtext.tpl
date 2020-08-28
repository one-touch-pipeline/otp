% if data["title"]:
${data["title"]}
${"=" * len(data["title"])}


% endif
% for version in data["versions"]:
<%
title = "%s (%s)" % (version["tag"], version["date"]) if version["tag"] else opts["unreleased_version_label"]

nb_sections = len(version["sections"])
%>${title}
${"=" * len(title)}
% for section in version["sections"]:
% if not (section["label"] == "Other" and nb_sections == 1):

${section["label"]}
${"-" * len(section["label"])}
% endif
% for commit in section["commits"]:
<%
    entry = indent('\n'.join(textwrap.wrap(commit["subject"])), first="- ").strip()
%>${entry}
% if commit["body"]:
<%
    import re
    cleaned_body = "\n".join(filter(lambda x: not re.match(r'^\s*$', x), commit["body"].split("\n")))
    entry = indent(cleaned_body, " "*4)+"\n"
%>${entry}
% endif
% endfor
% endfor

% endfor

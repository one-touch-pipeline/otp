#!/bin/bash

HOOK_NAMES="applypatch-msg pre-applypatch post-applypatch pre-commit prepare-commit-msg commit-msg post-commit pre-rebase post-checkout post-merge pre-receive update post-receive post-update pre-auto-gc"
HOOK_DIR=$(git rev-parse --show-toplevel)/.git/hooks

for hook in ${HOOK_NAMES}; do
    # if the hook already exists, is executable, and is not a symlink, rename it to *.local
    if [ ! -h ${HOOK_DIR}/${hook} -a -x ${HOOK_DIR}/${hook} ]; then
        mv ${HOOK_DIR}/${hook} ${HOOK_DIR}/${hook}.local
    fi
    # create the symlink, overwriting the file if it exists
    ln -sf ../../bashScripts/git-hooks/git-hook ${HOOK_DIR}/${hook}
done

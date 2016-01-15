#!/bin/bash

SSH_KEY=~/.ssh/id_rsa
SSH_AUTH_SOCK=~/ssh-agent-otp.socket
export SSH_AUTH_SOCK

if ssh-add -l &>/dev/null
then
    echo "ssh-agent already running."
else
    rm -f ${SSH_AUTH_SOCK}
    nohup ssh-agent -a ${SSH_AUTH_SOCK} &>/dev/null
    echo "ssh-agent started."
fi


KEYS_IN_AGENT="$(ssh-add -L | awk '{print $2}')"
KEYS_ON_DISK="$(awk '{print $2}' ${SSH_KEY}.pub)"

if [ "${KEYS_IN_AGENT}" = "${KEYS_ON_DISK}" ]
then
    echo "Passphrase already known to ssh-agent."
else
    ssh-add ${SSH_KEY}
fi

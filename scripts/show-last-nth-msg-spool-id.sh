#!/bin/bash

# leave these values hardcode here, or make empty to have prompts
HOST="http://localhost:8080"
USER="admin"
PW="admin"
VPN="default"
QUEUE=""
NUM=0

echo "┌ This utility returns the Message Spool ID of a message from the back of ┐"
echo "│ a queue, the nth-newest message.  This ID can then be used with a queue │"
echo "│ browser to skip all the messages until then. Requires curl and xmllint. │"
echo "└─────────────────────────────────────────────────────────────────────────┘"

# read in some vars, or hardcode them above
if [[ $HOST == "" ]]; then
    echo -n "SEMP URL (e.g. https://aaron.messaging.solace.cloud:943): "
    read HOST
fi
if [[ $USER == "" ]]; then
    echo -n "SEMP username: "
    read USER
fi
if [[ $PW == "" ]]; then
    echo -n "SEMP password: "
    stty -echo
    read PW
    stty echo
    echo
fi
if [[ $VPN == "" ]]; then
    echo -n "Message VPN: "
    read VPN
fi
if [[ $QUEUE == "" ]]; then
    echo -n "Queue: "
    read QUEUE
fi
if [[ $NUM == 0 ]]; then
    echo -n "Get Message Spool ID for nth-last message: n="
    read NUM
fi

# here we go, try to get message details...

RGMIDs=`curl -s --compressed -u $USER:$PW $HOST/SEMP -d "<rpc><show><queue><name>$QUEUE</name><vpn-name>$VPN</vpn-name><messages/><newest/><count/><num-elements>$NUM</num-elements></queue></show></rpc>" | xmllint --xpath '//message-id/text()' - `

# uncomment this to dump them all to the console
#echo $RGMIDs

if [[ $RGMIDs == "" ]]; then
    echo "## Problem with SEMP request, probably too many messages! Try less than 28000."
    echo "## (or modify this script to support paging via <more-cookie>)"
    exit 1
fi

IFS=$'\n'
names=($RGMIDs)

echo
echo "The MsgSpoolID of the ${#names[@]}th-last message on queue '$QUEUE' is: ${names[${#names[@]}-1]} "

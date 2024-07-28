#!/bin/bash

# leave these values hardcode here, or make empty to have prompts
HOST="http://localhost:8080"
USER="admin"
PW="admin"
VPN="default"
S_QUEUE=""
D_QUEUE=""
NUM=0

echo "┌ This utility copies the last/newest n messages from one queue to another. ┐"
echo "│ It uses SEMPv1 to retrieve message details (RGMIDs) and SEMPv1 again to   │"
echo "│ copy each message one-by-one.  Requires curl, xmllint, and cut.           │"
echo "└───────────────────────────────────────────────────────────────────────────┘"

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
if [[ $S_QUEUE == "" ]]; then
    echo -n "Source Queue: "
    read S_QUEUE
fi
if [[ $D_QUEUE == "" ]]; then
    echo -n "Dest Queue: "
    read D_QUEUE
fi
if [[ $NUM == 0 ]]; then
    echo -n "Number of messages from back of queue: "
    read NUM
fi

# here we go, try to get message details...

RGMIDs=`curl -s --compressed -u $USER:$PW $HOST/SEMP -d "<rpc><show><queue><name>$S_QUEUE</name><vpn-name>$VPN</vpn-name><messages/><newest/><detail/><count/><num-elements>$NUM</num-elements></queue></show></rpc>" | xmllint --xpath '//replication-group-msg-id' - | cut -d'>' -f2 | cut -d'<' -f1`

if [[ $RGMIDs == "" ]]; then
    echo "## Problem with SEMP request, probably too many messages! Try less than 4900."
    echo "## (or modify this script to support paging via <more-cookie>)"
    exit 1
fi

echo
echo "SEMP call successful. Copying messages..."

IFS=$'\n'
names=($RGMIDs)

COUNT=0;
# change this to a regular forward for loop to copy the messages newest-to-oldest
for (( i=0; i < ${#names[@]}; i++ ))    # reverse order (newest-to-oldest)
#for (( i=${#names[@]}-1; i >= 0; i-- ))  # original order (oldest-to-newest)
do
    #echo "$i: ${names[$i]}"
    RESP=`curl -q -s -u $USER:$PW $HOST/SEMP -d "<rpc><admin><message-spool><vpn-name>$VPN</vpn-name><copy-message><source/><queue-name>$S_QUEUE</queue-name><destination/><queue-name>$D_QUEUE</queue-name><message/><replication-group-msg-id>${names[$i]}</replication-group-msg-id></copy-message></message-spool></admin></rpc>"`
    if [[ $RESP != *"ok"* ]]; then
        echo "Had problems with ${names[$i]}:"
	echo $RESP
	echo
	echo "$COUNT messages (out of $NUM) successfully copied."
	echo "Quitting!"
	exit 1
    fi
    ((COUNT++));
done

echo "Last $COUNT messages successfully copied from Queue '$S_QUEUE'."

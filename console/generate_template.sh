#!/bin/bash
# To create a vm template out of the contents of init.sh to pass to manager VM.

HEXRAW=`xxd -p init.sh`
HEX=`echo $HEXRAW | sed 's/ //g'`
sed 's:CONTEXT\ =\ \[:CONTEXT\ =\ \[\
\ \ USERDATA\ =\ \"'"$HEX"'\",:' < template.vm > final_template.vm

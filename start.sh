#!/bin/bash
#qdel $(qstat | grep ava360 | awk '{print $1;}')
sleep 2
kill $(pgrep java -U ava360)
sleep 2
./ibisServer.sh
cd worker ; rm * ; cd ..
./compile.sh
./batsMaster.sh

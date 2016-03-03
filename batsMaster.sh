#!/bin/bash
#Start the BATS master
whichMaster=9
# {0=Mine,1=Naive,2=MinMin,3=MinMax,4=DummyMinMax, 5=Sampling, 7=Spot, 8=Energy, 9=SimpleEnergy}
deadline=24
budget=400
size=53
schedulesDump=$SPL_BATS_HOME/bats_schedules
port=9000
iplServerOutput=$SPL_BATS_HOME/iplServer.out
address=$(grep --text "^Ibis server running on" $iplServerOutput | awk '{print $5}')
poolname="master_pool"

cd $SPL_BATS_HOME/

java -cp $SPL_BATS_HOME/lib/*:$SPL_BATS_HOME/:$IPL_HOME/lib/*:$IPL_HOME/external/* \
	-Dlog4j.configuration=log4j.properties:$IPL_HOME/log4j.properties \
	-Dibis.pool.name=$poolname \
	-Dibis.server.address=$address \
	-Dibis.server.port=$port \
	org.koala.runnersFramework.runners.bot.BoTRunner \
	1> $SPL_BATS_HOME/out.sampl.log \
	2> $SPL_BATS_HOME/err.sampl.log


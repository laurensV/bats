#!/bin/bash

[ -z "$SPL_BATS_HOME" ] && echo "Need to set SPL_BATS_HOME" && exit 1
[ -z "$IPL_HOME" ] && echo "Need to set IPL_HOME" && exit 1

mkdir $SPL_BATS_HOME/temp 

javac -cp $SPL_BATS_HOME/lib/*:$IPL_HOME/lib/*:$IPL_HOME/external/* \
	$SPL_BATS_HOME/src/org/koala/runnersFramework/runners/bot/*.java \
	$SPL_BATS_HOME/src/org/koala/runnersFramework/runners/bot/listener/*.java \
	$SPL_BATS_HOME/src/org/koala/runnersFramework/runners/bot/util/*.java \
	-d $SPL_BATS_HOME/temp/

cd $SPL_BATS_HOME/temp
jar cf $SPL_BATS_HOME/lib/conpaas-bot.jar .

rm -r $SPL_BATS_HOME/temp

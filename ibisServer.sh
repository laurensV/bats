#!/bin/bash
# Start the Ibis server
port=9000
iplServerOutput=$SPL_BATS_HOME/iplServer.out

# aws code
#wget http://169.254.169.254/latest/meta-data/public-ipv4
#public-ip=$(cat public-ipv4)
#echo $public-ip

# aws code
#-Dsmartsockets.external.manual=$(cat public-ipv4) \
        
java -classpath "$IPL_HOME/lib/*:$IPL_HOME/external/*" \
        -Dlog4j.configuration=file:$IPL_HOME/log4j.properties  \
        -Xmx256M ibis.ipl.server.Server \
        --events --errors --stats  --port $port \
       2> $iplServerOutput &

# test if the file is non-empty
test -s $iplServerOutput
while [ $? -ne 0 ]
do
        sleep 1
        test -s $iplServerOutput
done

address=$(grep "^Ibis server running on" $iplServerOutput | awk '{print $5}')
poolname="master_pool"


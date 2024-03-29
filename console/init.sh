#!/bin/sh

# -------------------------------------------------------------------------- #
# Copyright 2002-2011, OpenNebula Project Leads (OpenNebula.org)             #
#                                                                            #
# Licensed under the Apache License, Version 2.0 (the "License"); you may    #
# not use this file except in compliance with the License. You may obtain    #
# a copy of the License at                                                   #
#                                                                            #
# http://www.apache.org/licenses/LICENSE-2.0                                 #
#                                                                            #
# Unless required by applicable law or agreed to in writing, software        #
# distributed under the License is distributed on an "AS IS" BASIS,          #
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   #
# See the License for the specific language governing permissions and        #
# limitations under the License.                                             #
#--------------------------------------------------------------------------- #

if [ -f /mnt/context.sh ]
then
  . /mnt/context.sh
fi

##############################################################
#Please set all variables accordingly:
ONE_USER=
ONE_PASSWD=
#paste the content of your ~/.ssh/id_rsa.pub between quotes
PUBKEY=""
#use the xtreemfs url where the latest BaTS jars reside (e.g. 10.0.0.1/TaskFarm)
FRESH_BATS=

echo $HOSTNAME > /etc/hostname
hostname $HOSTNAME
sed -i "/127.0.1.1/s/ubuntu/$HOSTNAME/" /etc/hosts
echo "10.0.0.200  n00.cumulus.zib.de n00" >> /etc/hosts
echo "10.0.0.201  n01.cumulus.zib.de n01" >> /etc/hosts 
echo "10.0.0.202  n02.cumulus.zib.de n02" >> /etc/hosts
echo "10.0.0.203  n03.cumulus.zib.de n03" >> /etc/hosts
echo "10.0.0.204  n04.cumulus.zib.de n04" >> /etc/hosts
echo "10.0.0.205  n05.cumulus.zib.de n05" >> /etc/hosts
echo "10.0.0.206  n06.cumulus.zib.de n06" >> /etc/hosts
echo "10.0.0.207  n07.cumulus.zib.de n07" >> /etc/hosts
echo "10.0.0.208  n08.cumulus.zib.de n08" >> /etc/hosts
echo "10.0.0.209  n09.cumulus.zib.de n09" >> /etc/hosts
echo "10.0.0.210  n10.cumulus.zib.de n10" >> /etc/hosts
echo "10.0.0.211  n11.cumulus.zib.de n11" >> /etc/hosts
echo "10.0.0.212  n12.cumulus.zib.de n12" >> /etc/hosts
echo "10.0.0.213  n13.cumulus.zib.de n13" >> /etc/hosts
echo "10.0.0.214  n14.cumulus.zib.de n14" >> /etc/hosts
echo "10.0.0.215  n15.cumulus.zib.de n15" >> /etc/hosts
echo "10.0.0.216  n16.cumulus.zib.de n16" >> /etc/hosts
echo "10.0.0.217  n17.cumulus.zib.de n17" >> /etc/hosts
echo "10.0.0.218  n18.cumulus.zib.de n18" >> /etc/hosts
echo "10.0.0.219  n19.cumulus.zib.de n19" >> /etc/hosts
echo "10.0.0.220  n20.cumulus.zib.de n20" >> /etc/hosts
echo "10.0.0.221  n21.cumulus.zib.de n21" >> /etc/hosts
echo "10.0.0.222  n22.cumulus.zib.de n22" >> /etc/hosts
echo "10.0.0.223  n23.cumulus.zib.de n23" >> /etc/hosts
echo "10.0.0.224  n24.cumulus.zib.de n24" >> /etc/hosts
echo "10.0.0.225  n25.cumulus.zib.de n25" >> /etc/hosts
echo "10.0.0.226  n26.cumulus.zib.de n26" >> /etc/hosts
echo "10.0.0.227  n27.cumulus.zib.de n27" >> /etc/hosts
echo "10.0.0.228  n28.cumulus.zib.de n28" >> /etc/hosts
echo "10.0.0.229  n29.cumulus.zib.de n29" >> /etc/hosts
echo "10.0.0.230  n30.cumulus.zib.de n30" >> /etc/hosts
echo "10.0.0.231  n31.cumulus.zib.de n31" >> /etc/hosts

##############################################################

export ONE_USER
export ONE_PASSWD
export ONE_AUTH_CONTENT=$ONE_USER:$ONE_PASSWD


# no files passed in the contextualization, so no id_rsa.pub
# set here your own key to enable access to the vm

export PUBKEY
if [ -n "$PUBKEY" ]; then
	mkdir -p /root/.ssh
	echo $PUBKEY >> /root/.ssh/authorized_keys
	chmod 600 /root/.ssh/authorized_keys
	chmod 700 /root/.ssh	
fi
##############################################################

if [ -n "$IP_PUBLIC" ]; then
	ifconfig eth0 $IP_PUBLIC
fi
 
if [ -n "$NETMASK" ]; then
	ifconfig eth0 netmask $NETMASK
fi

if [ -f /mnt/$ROOT_PUBKEY ]; then
	mkdir -p /root/.ssh
	cat /mnt/$ROOT_PUBKEY >> /root/.ssh/authorized_keys
	#chmod -R 600 /root/.ssh/
	chmod 600 /root/.ssh/authorized_keys
	chmod 700 /root/.ssh
fi

if [ -n "$USERNAME" ]; then
	useradd -s /bin/bash -m $USERNAME
	if [ -f /mnt/$USER_PUBKEY ]; then
		mkdir -p /home/$USERNAME/.ssh/
		cat /mnt/$USER_PUBKEY >> /home/$USERNAME/.ssh/authorized_keys
		chown -R $USERNAME:$USERNAME /home/$USERNAME/.ssh
		chmod 600 /home/$USERNAME/.ssh/authorized_keys
	fi
fi

# added:
if [ -n "$IP_GATEWAY" ]; then
        # Change gateway from default; note eth0 is already active
        (           
            ip route add default via $IP_GATEWAY
            echo Now set IP_GATEWAY to $IP_GATEWAY
        ) >> /var/log/context.log 2>&1

fi

# added:
if [ -n "$NAMESERVER" ]; then
        echo "Setting DNS server to $NAMESERVER" >>/var/log/context.log
        echo "nameserver $NAMESERVER" >/etc/resolv.conf
fi

#=============================================================================
# Specifics for the Bats manager
#-----------------------------------------------------------------------------

export BATS_HOME=/home/opennebula/BoT
export BATS_HOME_LIB=$BATS_HOME/lib
export IPL_HOME=$BATS_HOME/ibis

export FRESH_BATS

mkdir $BATS_HOME_LIB
#----------------------------------------------------------------------------

#============================================================================
# The worker's init.sh. Export the filename so that the JVM knows where it is.
# this is the content of a workers init.sh
# with escaped variables

export WORKER_INIT_SH=$BATS_HOME/init.sh

cat > $WORKER_INIT_SH <<EOF
#!bin/bash

# THIS FILE IS THE INIT.SH FOR THE WORKERS. IT WILL BE CONVERTED TO A HEX STRING AND PASSED TO THE WORKERS.

if [ -f /mnt/context.sh ]
then
  . /mnt/context.sh
fi

echo \$HOSTNAME > /etc/hostname
hostname \$HOSTNAME
sed -i "/127.0.1.1/s/ubuntu/\$HOSTNAME/" /etc/hosts
echo "10.0.0.200  n00.cumulus.zib.de n00" >> /etc/hosts
echo "10.0.0.201  n01.cumulus.zib.de n01" >> /etc/hosts 
echo "10.0.0.202  n02.cumulus.zib.de n02" >> /etc/hosts
echo "10.0.0.203  n03.cumulus.zib.de n03" >> /etc/hosts
echo "10.0.0.204  n04.cumulus.zib.de n04" >> /etc/hosts
echo "10.0.0.205  n05.cumulus.zib.de n05" >> /etc/hosts
echo "10.0.0.206  n06.cumulus.zib.de n06" >> /etc/hosts
echo "10.0.0.207  n07.cumulus.zib.de n07" >> /etc/hosts
echo "10.0.0.208  n08.cumulus.zib.de n08" >> /etc/hosts
echo "10.0.0.209  n09.cumulus.zib.de n09" >> /etc/hosts
echo "10.0.0.210  n10.cumulus.zib.de n10" >> /etc/hosts
echo "10.0.0.211  n11.cumulus.zib.de n11" >> /etc/hosts
echo "10.0.0.212  n12.cumulus.zib.de n12" >> /etc/hosts
echo "10.0.0.213  n13.cumulus.zib.de n13" >> /etc/hosts
echo "10.0.0.214  n14.cumulus.zib.de n14" >> /etc/hosts
echo "10.0.0.215  n15.cumulus.zib.de n15" >> /etc/hosts
echo "10.0.0.216  n16.cumulus.zib.de n16" >> /etc/hosts
echo "10.0.0.217  n17.cumulus.zib.de n17" >> /etc/hosts
echo "10.0.0.218  n18.cumulus.zib.de n18" >> /etc/hosts
echo "10.0.0.219  n19.cumulus.zib.de n19" >> /etc/hosts
echo "10.0.0.220  n20.cumulus.zib.de n20" >> /etc/hosts
echo "10.0.0.221  n21.cumulus.zib.de n21" >> /etc/hosts
echo "10.0.0.222  n22.cumulus.zib.de n22" >> /etc/hosts
echo "10.0.0.223  n23.cumulus.zib.de n23" >> /etc/hosts
echo "10.0.0.224  n24.cumulus.zib.de n24" >> /etc/hosts
echo "10.0.0.225  n25.cumulus.zib.de n25" >> /etc/hosts
echo "10.0.0.226  n26.cumulus.zib.de n26" >> /etc/hosts
echo "10.0.0.227  n27.cumulus.zib.de n27" >> /etc/hosts
echo "10.0.0.228  n28.cumulus.zib.de n28" >> /etc/hosts
echo "10.0.0.229  n29.cumulus.zib.de n29" >> /etc/hosts
echo "10.0.0.230  n30.cumulus.zib.de n30" >> /etc/hosts
echo "10.0.0.231  n31.cumulus.zib.de n31" >> /etc/hosts

##########################################################
# don't escape PUBKEY. it will be the same as the manager's
if [ -n "$PUBKEY" ]; then
        mkdir -p /root/.ssh
        echo $PUBKEY >> /root/.ssh/authorized_keys
        chmod 600 /root/.ssh/authorized_keys
        chmod 700 /root/.ssh
fi
##########################################################

if [ -n "\$IP_PUBLIC" ]; then
        ifconfig eth0 \$IP_PUBLIC
fi

if [ -n "\$NETMASK" ]; then
        ifconfig eth0 netmask \$NETMASK
fi

if [ -f /mnt/\$ROOT_PUBKEY ]; then
        mkdir -p /root/.ssh
        cat /mnt/\$ROOT_PUBKEY >> /root/.ssh/authorized_keys
        #chmod -R 600 /root/.ssh/
        chmod 600 /root/.ssh/authorized_keys
        chmod 700 /root/.ssh
fi

if [ -n "\$USERNAME" ]; then
        useradd -s /bin/bash -m \$USERNAME
        if [ -f /mnt/\$USER_PUBKEY ]; then
                mkdir -p /home/\$USERNAME/.ssh/
                cat /mnt/\$USER_PUBKEY >> /home/\$USERNAME/.ssh/authorized_keys
                chown -R \$USERNAME:\$USERNAME /home/\$USERNAME/.ssh
                chmod 600 /home/\$USERNAME/.ssh/authorized_keys
        fi
fi

# added:
if [ -n "\$GATEWAY" ]; then
        # Change gateway from default; note eth0 is already active
        (
            ip route add default via \$GATEWAY
            echo Now set GATEWAY to \$GATEWAY
        ) >> /var/log/context.log 2>&1

fi

# added:
if [ -n "\$DNS" ]; then
        echo "Setting DNS server to \$DNS" >>/var/log/context.log
        echo "nameserver \$DNS" >/etc/resolv.conf
fi

########################################################################################
# Specs for BaTS worker

export BATS_HOME=/home/opennebula/BoT
export IPL_HOME=\$BATS_HOME/ibis
export MOUNT_FOLDER=\$BATS_HOME/mount
mkdir \$MOUNT_FOLDER

########################################################################################
# Copy the freshest version of conpaas-bot.jar
# don't escape FRESH_BATS.  use the same for both master and worker
mount.xtreemfs --interrupt-signal=0 $FRESH_BATS \$MOUNT_FOLDER

# need conpaas-bot.jar and one_libraries
mkdir \$BATS_HOME/lib
cp \$MOUNT_FOLDER/lib/*.jar \$BATS_HOME/lib 1> \$BATS_HOME/mounting.log 2> \$BATS_HOME/mounting.err.log

umount.xtreemfs \$MOUNT_FOLDER
########################################################################################

########################################################################################
# Create the one_auth file from contextualization variable ONE_AUTH_CONTENT
# and set it as an environment variable for the JVM

export ONE_AUTH=\$BATS_HOME/.one_auth
export VM_ID
export ONE_XMLRPC

echo \$ONE_AUTH_CONTENT > \$ONE_AUTH
########################################################################################

MOUNTURL=$FRESH_BATS

echo "Running java OneVMWorker vm_id = \$VM_ID, one_xmlrpc = \$ONE_XMLRPC, one_auth = \$ONE_AUTH, location=\$LOCATION, elect=\$ELECTIONNAME, pool=\$POOLNAME, servaddr=\$SERVERADDRESS, speedfct=\$SPEEDFACTOR" > \$BATS_HOME/info.txt
echo "MOUNTURL=\$MOUNTURL" >> \$BATS_HOME/info.txt
echo "MOUNT_FOLDER=\$MOUNT_FOLDER" >> \$BATS_HOME/info.txt

# \$MOUNTURL is an environment variable, passed down from the contextualization process
mount.xtreemfs --interrupt-signal=0 \$MOUNTURL \$MOUNT_FOLDER 1>> \$BATS_HOME/mounting.log 2>> \$BATS_HOME/mounting.err.log

cd /home/opennebula

# The JVM will set as working directory \$MOUNT_FOLDER in the JVM 
java                                                                                            \
        -classpath \$BATS_HOME/lib/*:\$IPL_HOME/lib/*                                             \
        -Dibis.location=\$LOCATION                                                               \
        org.koala.runnersFramework.runners.bot.OneVMWorker \$ELECTIONNAME \$POOLNAME \$SERVERADDRESS \$SPEEDFACTOR \
        1> \$MOUNT_FOLDER/logs/vm_\$VM_ID.out.log 2> \$MOUNT_FOLDER/logs/vm_\$VM_ID.err.log

umount.xtreemfs \$MOUNT_FOLDER

# shutdown -h now
EOF
#end of worker script
chmod a+r $WORKER_INIT_SH

# set as env var the hex of init.sh, so that the JVM passes it right to the worker's contextualization
export HEX_FILE=$BATS_HOME/worker_init_sh.hex
xxd -p $WORKER_INIT_SH > $HEX_FILE
#============================================================================

#-----------------------------------------------------------------------------
# Create the one_auth file in the master virtual machine.
mkdir -p /root/.one
# if there is an one_auth file, it will have priority over the above set ONE_AUTH_CONTENT
if [ -f /mnt/$ONE_AUTH_FILE ]; then
        cat /mnt/$ONE_AUTH_FILE > /root/.one/one_auth
elif [ -n "$ONE_AUTH_CONTENT" ]; then
	echo $ONE_AUTH_CONTENT > /root/.one/one_auth
fi

chmod 600 /root/.one/one_auth
chmod 700 /root/.one
# env var required by the .jar
export ONE_AUTH=/root/.one/one_auth
export ONE_XMLRPC=http://cumulus.zib.de:2633/RPC2
#-----------------------------------------------------------------------------

#-----------------------------------------------------------------------------
# Get the freshest copy of conpaas-bot.jar
MOUNT_FOLDER=mount
mount_log=$BATS_HOME/mount.log

mkdir $MOUNT_FOLDER

mount.xtreemfs --interrupt-signal=0 $FRESH_BATS $MOUNT_FOLDER > $mount_log

echo >> $mount_log

cp $MOUNT_FOLDER/lib/*.jar $BATS_HOME_LIB >> $mount_log

echo >> $mount_log


mkdir -p $BATS_HOME/ClusterConfiguration
cp $MOUNT_FOLDER/config/clusterConf.xml $BATS_HOME/ClusterConfiguration/


mkdir -p $BATS_HOME/BagOfTasks
cp $MOUNT_FOLDER/config/bagMountTest.bot $BATS_HOME/BagOfTasks/bagOfTasks.bot

umount.xtreemfs $MOUNT_FOLDER >> $mount_log
rm -r $MOUNT_FOLDER
#-----------------------------------------------------------------------------

#-----------------------------------------------------------------------------
# Start the Ibis server
port=8999
iplServerOutput=$BATS_HOME/iplServer.out

java -classpath "$IPL_HOME/lib/*" \
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
#-----------------------------------------------------------------------------

#-----------------------------------------------------------------------------

#-----------------------------------------------------
# Start the BATS master
whichMaster=5
# {0=Mine,1=Naive,2=MinMin,3=MinMax,4=DummyMinMax}
deadline=24
budget=400
size=1000
schedulesDump=$BATS_HOME/bats_schedules

cd $BATS_HOME/

java -cp $BATS_HOME_LIB/*:$BATS_HOME/:$IPL_HOME/lib/* \
	-Dlog4j.configuration=log4j.properties:$IPL_HOME/log4j.properties \
	-Dibis.pool.name=$poolname \
	-Dibis.server.address=$address \
	-Dibis.server.port=$port \
	org.koala.runnersFramework.runners.bot.BoTRunner $whichMaster $deadline $budget $size $schedulesDump \
	1> $BATS_HOME/out.sampl.log \
	2> $BATS_HOME/err.sampl.log

#-----------------------------------------------------

#-----------------------------------------------------
# Prepare schedule information reporting and execution phase scripts.


SCHEDULES_FILE="bats_schedules"
file=$BATS_HOME/$SCHEDULES_FILE

SCHEDULE_NUMBER=0

cd $BATS_HOME


echo "#!/bin/bash" >> list_sched.sh
echo "java -cp $BATS_HOME_LIB/*:$BATS_HOME/:$IPL_HOME/lib/* \\
	-Dlog4j.configuration=log4j.properties:$IPL_HOME/log4j.properties \\
	-Dibis.pool.name=$poolname \\
	-Dibis.server.address=$address \\
	-Dibis.server.port=$port \\
	org.koala.runnersFramework.runners.bot.Executor list $file" >> list_sched.sh
chmod +x list_sched.sh


echo "#!/bin/bash" >> execute_console.sh
echo "export HEX_FILE=$BATS_HOME/worker_init_sh.hex" >> execute_console.sh
echo "export ONE_AUTH=/root/.one/one_auth" >> execute_console.sh
echo "export ONE_XMLRPC=http://cumulus.zib.de:2633/RPC2" >> execute_console.sh
echo "java -cp $BATS_HOME_LIB/*:$BATS_HOME/:$IPL_HOME/lib/* \\
	-Dlog4j.configuration=log4j.properties:$IPL_HOME/log4j.properties \\
	-Dibis.pool.name=$poolname \\
	-Dibis.server.address=$address \\
	-Dibis.server.port=$port \\
	org.koala.runnersFramework.runners.bot.ExecutorConsole \$1 $file" >> execute_console.sh
chmod +x execute_console.sh

# fin
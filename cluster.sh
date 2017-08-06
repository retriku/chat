#!/bin/bash

set -e
wd=`pwd`

startNode() {
  node=$1
  ./target/universal/stage/bin/chat \
    -Dhttp.port=900$node -Dakka.remote.netty.tcp.port=255$node \
    -Dpidfile.path=$wd/target/node$node.pid \
    -Dnode.id=$node \
    -Dakka.cluster.seed-nodes.0=akka.tcp://application@127.0.0.1:2551 \
    -Dakka.cluster.seed-nodes.1=akka.tcp://application@127.0.0.1:2552 \
    -Dakka.cluster.seed-nodes.2=akka.tcp://application@127.0.0.1:2553 \
     &
}

stopNode() {
  pidfile=$wd/target/node$1.pid
  if [ -e $pidfile ]
  then
    if kill -0 `cat $pidfile`
    then
        kill `cat $pidfile`
    fi
    rm $pidfile
  fi
}

stop() {
    stopNode 1
    stopNode 2
    stopNode 3

    if [ -e target/nginx.pid ]
    then
        kill `cat target/nginx.pid`
    fi
}

start() {
    # Ensure the project is built
    sbt stage

    startNode 1
    startNode 2
    startNode 3

    nginx -p $wd -c nginx.conf &
}

case $1 in

    restart)

        stop ; start
    ;;

    stop)

        stop
    ;;

    start)

        start
    ;;

esac

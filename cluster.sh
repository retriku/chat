#!/bin/bash

set -e
wd=`pwd`

startNode() {
  node=$1
  ./target/universal/stage/bin/chat \
    -jvm-debug 9999 \
    -Dhttp.port=900$node -Dakka.remote.netty.tcp.port=255$node \
    -Dpidfile.path=$wd/target/node$node.pid \
    -Dnode.id=$node \
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
#    stopNode 2
#    stopNode 3

    if [ -e target/nginx.pid ]
    then
        kill `cat target/nginx.pid`
    fi
}

start() {
    # Ensure the project is built
    rm -rf /tmp/chat
    sbt clean stage

    startNode 1
#    startNode 2
#    startNode 3

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

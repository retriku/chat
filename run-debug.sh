#!/bin/bash

killall -9 java ; sbt -jvm-debug 9999 -Dakka.remote.netty.tcp.port=2551 -Dhttp.port=9001 clean run
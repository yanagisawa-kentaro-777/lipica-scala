#!/bin/bash

java -Dfile.encoding=utf-8 -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -XX:+UseCompilerSafepoints -XX:+UseOnStackReplacement -XX:CompileThreshold=200 -XX:+UseBiasedLocking -XX:ReservedCodeCacheSize=32M -Xmn32M -XX:MaxNewSize=48M -XX:SurvivorRatio=4 -XX:MaxTenuringThreshold=15 -XX:TargetSurvivorRatio=80 -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseParNewGC -Xms512M -Xmx1024M -classpath "./jars/*":"./conf/" org.lipicalabs.lipica.EntryPoint $* &

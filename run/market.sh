#!/usr/bin/env bash

jarPath="../jade/jade.jar"
classesPath="../classes"
msInterval=100
verbose="false"
numProducer=5
numConsumer=50

agentString=""

if [ "$1" = "help" ]
  then
    echo "usage:"
    echo "./market.sh <jadeJarFile> <path/to/classes/folder> <msRoundInterval> <verbose> <numProducer> <numConsumer>"
    echo "./market.sh ../jade/jade.jar ../classes 100 false 5 50"
    exit 0
fi

if [ "$1" != "" ]
  then jarPath=$1
fi

if [ "$2" != "" ]
  then classesPath=$2
fi

if [ "$3" != "" ]
  then msInterval=$3
fi

if [ "$4" != "" ]
  then verbose=$4
fi

if [ "$5" != "" ]
  then numProducer=$5
fi

if [ "$6" != "" ]
  then numConsumer=$6
fi


echo "JADE / Agents will run with:"
echo "jadeJarFile $jarPath"
echo "classesPath $classesPath"
echo "msInterval $msInterval"
echo "verbose $verbose"
echo "numProducer $numProducer"
echo "numConsumer $numConsumer"


for ((i=0;i<$numProducer;i++))
    do
      agentString="$agentString;producer_$i:marketSimulation.ProducerAgent($msInterval,$verbose)"
    done

for ((i=0;i<$numConsumer;i++))
    do
      agentString="$agentString;consumer_$i:marketSimulation.ConsumerAgent($msInterval,$verbose)"
    done

java -cp ${jarPath}:${classesPath} jade.Boot -gui -agents ${agentString}
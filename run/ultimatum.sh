#!/usr/bin/env bash

msInterval=5000
jarPath="$PWD/$jarPath"
classesPath="$PWD/$classesPath"
playerAgentCount=5
bankAgentRounds=10
bankAgentMinAmount=100
bankAgentMaxAmount=1000
playerAgentStrategy="PROFIT"
playerAgentWinThreshold=5000

agentString=""

if [ "$1" = "help" ]
  then
    echo "example:"
    echo "./ultimatum.sh <jadeJarFile> <path/to/classes/folder> <msRoundInterval> <playerAgentCount> <bankAgentRounds> \
<bankAgentMinAmount> <bankAgentMaxAmount> <playerAgentWinThreshold> <playerAgentStrategy>"
    echo "./ultimatum.sh ./jade.jar ./classes 5000 5 10 100 1000 5000 <PROFIT || EQUALITY>"
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
  then playerAgentCount=$4
fi

if [ "$5" != "" ]
  then bankAgentRounds=$5
fi

if [ "$6" != "" ]
  then bankAgentMinAmount=$6
fi

if [ "$7" != "" ]
  then bankAgentMaxAmount=$7
fi

if [ "$8" != "" ]
  then playerAgentWinThreshold=$8
fi

if [ "$9" != "" ]
  then playerAgentStrategy=$9
fi

echo "Selected values:"
echo "jadeJarFile $jarPath"
echo "classesPath $classesPath"
echo "msInterval $msInterval"
echo "playerAgentCount $playerAgentCount"
echo "bankAgentRounds $bankAgentRounds"
echo "bankAgentMinAmount $bankAgentMinAmount"
echo "bankAgentMaxAmount $bankAgentMaxAmount"
echo "playerAgentWinThreshold $playerAgentWinThreshold"
echo "playerAgentStrategy $playerAgentStrategy"

for ((i=0;i<$playerAgentCount;i++))
do
  if [[ ${i} -eq 0 ]]
      then agentString="player$i:ultimatumGame.PlayerAgent($playerAgentStrategy,$playerAgentWinThreshold)"
    else agentString="$agentString;player$i:ultimatumGame.PlayerAgent($playerAgentStrategy,$playerAgentWinThreshold)"
  fi
done

agentString="$agentString;bank:ultimatumGame.BankAgent($bankAgentRounds,$bankAgentMinAmount,$bankAgentMaxAmount,$msInterval)"

java -cp ${jarPath}:${classesPath} jade.Boot -gui -agents ${agentString}
#!/bin/bash
# 권한 문제로 Docker-compose 시 mouse 시 실행이 안되므로 확인을 해야 한다.
echo "create kuku board topic"

cd /opt/kafka/bin/

./kafka-topics.sh --bootstrap-server localhost:9092 --create -topic kuku-board-article --replication-factor 1 --partitions 3
./kafka-topics.sh --bootstrap-server localhost:9092 --create -topic kuku-board-comment --replication-factor 1 --partitions 3
./kafka-topics.sh --bootstrap-server localhost:9092 --create -topic kuku-board-like --replication-factor 1 --partitions 3
./kafka-topics.sh --bootstrap-server localhost:9092 --create -topic kuku-board-view --replication-factor 1 --partitions 3
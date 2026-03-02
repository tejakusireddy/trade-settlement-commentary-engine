#!/usr/bin/env bash

set -euo pipefail

BROKER="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"

TOPICS=(
  "trade.events"
  "trade.breaches"
  "commentary.requests"
  "commentary.completed"
  "commentary.approved"
  "trade.dlq"
)

echo "Waiting for Kafka at ${BROKER}..."
until kafka-topics --bootstrap-server "${BROKER}" --list >/dev/null 2>&1; do
  sleep 2
done

echo "Kafka is reachable. Creating topics..."
for topic in "${TOPICS[@]}"; do
  kafka-topics \
    --bootstrap-server "${BROKER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions 3 \
    --replication-factor 1
done

echo "Done. Current topic list:"
kafka-topics --bootstrap-server "${BROKER}" --list

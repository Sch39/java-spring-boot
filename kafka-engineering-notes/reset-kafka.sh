#!/usr/bin/env bash
cd ~/kafka_2.13-4.3.1 # set to your kafka path
set -euo pipefail

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-localhost:9092}"

echo "=== Deleting Kafka topics ==="

while IFS= read -r topic; do
    [[ -z "$topic" || "$topic" == __* ]] && continue

    echo "Deleting topic: $topic"

    kafka-topics.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --delete \
        --topic "$topic"
done < <(
    kafka-topics.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --list
)

echo "=== Deleting consumer groups ==="

while IFS= read -r group; do
    [[ -z "$group" ]] && continue

    echo "Deleting group: $group"

    kafka-consumer-groups.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --delete \
        --group "$group"
done < <(
    kafka-consumer-groups.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --list
)

echo "=== Kafka reset commands completed ==="
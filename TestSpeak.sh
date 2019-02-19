#!/bin/bash

SERVER="test.mosquitto.org"
TOPIC="rcr/Speak"
VOICE="mb/mb-vz1"
#VOICE="mb/mb-us1"

echo "[Speak] Esperando en $SERVER - $TOPIC"
mosquitto_sub -h "$SERVER" -t "$TOPIC" | tee >(espeak -v "$VOICE" -a 150 -s 160 -p 30 -g 0 -b 1)


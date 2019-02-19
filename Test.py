#!/usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import paho.mqtt.client as paho
import json
import time
import uuid
import subprocess
import unicodedata
try:
    import Queue
except:
    import queue as Queue

MQTT_SERVER = 'test.mosquitto.org'
MQTT_PORT = 1883
TOPIC_CMD = 'rcr/Command'
TOPIC_SPEAK = 'rcr/Speak'

messages = Queue.Queue( 1 )

def sendToSpeak( mqtt_client, msg ):
    global TOPIC_SPEAK

    mqtt_client.publish( TOPIC_SPEAK, msg )


def mqtt_on_message( mqtt_client, userdata, message ):
    global messages

    # si no se ha procesado el ultimo mensaje lo eliminamos
    try:
        messages.get_nowait()
    except Queue.Empty:
        pass

    # agregamos el mensaje
    try:
        messages.put_nowait( message )
    except Queue.Full:
            pass


def mqtt_on_connect( mqtt_client, userdata, flags, rc ):
    global MQTT_SERVER, MQTT_PORT, TOPIC_CMD

    if( rc == 0 ):
        mqtt_client.subscribe( TOPIC_CMD )
        print( "[Test] Esperando en mqtt://%s:%s - %s" % ( MQTT_SERVER, MQTT_PORT, TOPIC_CMD ) )
    else:
        print( "[Test] Sin conexión con mqtt://%s:%s" % ( MQTT_SERVER, MQTT_PORT ) )

def mqtt_on_disconnect(client, userdata, rc):
    global MQTT_SERVER, MQTT_PORT

    print( "[Test] Desconectado de mqtt://%s:%s" % ( MQTT_SERVER, MQTT_PORT ) )


def main():
    global MQTT_SERVER, MQTT_PORT, messages

    print( '[Test] Iniciando sistema' )
    subprocess.Popen( '/bin/bash ./TestSpeak.sh', shell=True )

    mqtt_client = paho.Client( 'Test-' + uuid.uuid4().hex )
    mqtt_client.on_connect = mqtt_on_connect
    mqtt_client.on_disconnect = mqtt_on_disconnect
    mqtt_client.on_message = mqtt_on_message
    mqtt_client.connect( MQTT_SERVER, MQTT_PORT )
    mqtt_client.loop_start()
    abort = False
    while( not abort ):
        message = messages.get()

        # hacemos el manejo del payload que viene en utf-8 (se supone)
        # la idea es cambiar tildes y otros caracteres especiales
        # y llevar todo a minuscula
        cmd = message.payload.decode('utf-8').lower()
        cmd = ''.join((c for c in unicodedata.normalize('NFD', cmd) if unicodedata.category(c) != 'Mn'))
        print( "[Test] Mensaje recibido:", message.payload, "<<" + cmd + ">>" )

        # comandos recibidos
        if( cmd == 'salir' ):
            abort = True
        elif( cmd == 'que hora es' ):
            now = time.localtime()
            sendToSpeak( mqtt_client, 'son las %d horas con %d minutos' % (now.tm_hour, now.tm_min) )
        else:
            sendToSpeak( mqtt_client, cmd )

    mqtt_client.loop_stop()
    print( '[Test] Finalizando' )


#--
main()

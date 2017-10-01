import atexit
import logging
import picamera
import socket
import sys
from threading import *
from connection import *
from logging.handlers import RotatingFileHandler
from multi_socket_output import *
from settings import *

MAX_CONNECTIONS = 10
PORT = 43334

# create the logger
logger = logging.getLogger("camera")
logger.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
handler = RotatingFileHandler("camera.log", maxBytes=1000000, backupCount=5)
handler.setFormatter(formatter)
logger.addHandler(handler)

# create the exit handler
def exit_handler():
	sock.close()
	logger.info('socket closed')
	
atexit.register(exit_handler)
logger.info('exit handler registered')

# create the threads dictionary
threads = {}

# initialize the camera
camera = picamera.PiCamera()
camera.resolution = (WIDTH, HEIGHT)
camera.framerate = FPS
camera.led = False
output = MultiSocketOutput()
camera.start_recording(output, format='h264', bitrate=BPS)

# create the socket
sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
logger.info('socket created')

# bind the socket
try:
	sock.bind(('', PORT))
except socket.error as msg:
	logger.error('Bind Error: ' + str(msg[0]) + ' - ' + msg[1])
	sys.exit()
logger.info('bind complete')
 
# listen to the socket
sock.listen(MAX_CONNECTIONS)
logger.info('listening')

# create a thread for each connection
while True:
	# wait for a connection
	conn, addr = sock.accept()
	name = addr[0] + ':' + str(addr[1])
	logger.info('command connection from ' + name)
	 
	# start a new connection thread
	thread = Connection(name, conn, camera, output)
	thread.start()
	threads[name] = thread
	
	# remove dead threads
	remove = []
	for name, thread in threads.items():
		if not thread.isAlive():
			remove.append(name)
	for name in remove:
		del threads[name]
	remove = []

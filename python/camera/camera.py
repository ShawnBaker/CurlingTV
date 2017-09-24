import atexit
import picamera
import socket
import sys
from threading import *
from connection import *
from multi_socket_output import *
from settings import *

MAX_CONNECTIONS = 10
PORT = 43334

# create the exit handler
def exit_handler():
	sock.close()
	print('socket closed')
atexit.register(exit_handler)
print('exit handler registered')

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
print('socket created')

# bind the socket
try:
	sock.bind(('', PORT))
except socket.error as msg:
	print('Bind Error: ' + str(msg[0]) + ' - ' + msg[1])
	sys.exit()
print('bind complete')
 
# listen to the socket
sock.listen(MAX_CONNECTIONS)
print('listening')

# create a thread for each connection
while True:
	# wait for a connection
	conn, addr = sock.accept()
	name = addr[0] + ':' + str(addr[1])
	print('command connection from ' + name)
	 
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

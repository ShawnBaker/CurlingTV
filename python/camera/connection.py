import socket
import threading
from multi_socket_output import *
from settings import *
from image import *
from video import *

RECEIVE_SIZE = 1024

# handles a command connection
class Connection (threading.Thread):

	def __init__(self, name, command_socket, camera, output):
		threading.Thread.__init__(self)
		self.name = name
		self.command_socket = command_socket
		self.camera = camera
		self.output = output
		self.video_socket = None
		self.video = None
		self.video_port = 0
		self.image = None
		self.image_port = 0

	def run(self):
		# read commands forever
		while True:
			 
			# read a command from the client
			print('waiting for a command')
			command_bytes = self.command_socket.recv(RECEIVE_SIZE)
			if not command_bytes:
				print('NOT a command')
				break
			command = command_bytes.decode('ascii')
			print('command = ' + command)

			# gets the name of this device
			if command == 'get_name':
				send_bytes = DEVICE_NAME.encode('ascii')
				self.command_socket.sendall(send_bytes)
				
			# gets the video and image parameters
			elif command == 'get_video_params':
				params = "%d,%d,%d,%d" % (WIDTH, HEIGHT, FPS, BPS)
				send_bytes = params.encode('ascii')
				self.command_socket.sendall(send_bytes)
				
			# gets a port for video, spawns a thread to wait for a connection on that port
			elif command == 'get_video_port':
				try:
					# create a video connection if necessary
					if not self.output.contains_connection(self.name):
						# close any open socket
						if self.video_socket is not None:
							self.video_socket.close()

						# create a socket and bind it to an available port
						self.video_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
						self.video_socket.bind(('', 0))
						self.video_port = self.video_socket.getsockname()[1]
						print('video socket on port %d' % self.video_port)
					
						# start a new video thread
						self.video = Video(self.name, self.video_socket, self.output)
						self.video.start()
					
					# report the port number back to the client
					send_bytes = str(self.video_port).encode('ascii')
					self.command_socket.sendall(send_bytes)
				except socket.error as msg:
					print('Bind Error: ' + str(msg[0]) + ' - ' + msg[1])

			# gets a port for an image, spawns a thread to wait for a connection on that port
			elif command == 'get_image_port':
				try:
					# create an image connection if necessary
					if self.image is None or not self.image.is_alive():

						# create a socket and bind it to an available port
						image_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
						image_socket.bind(('', 0))
						self.image_port = image_socket.getsockname()[1]
						print('image socket on port %d' % self.image_port)
					
						# start a new image thread
						self.image = Image(image_socket, self.camera)
						self.image.start()
					
					# report the port number back to the client
					send_bytes = str(self.image_port).encode('ascii')
					self.command_socket.sendall(send_bytes)
				except socket.error as msg:
					print('Bind Error: ' + str(msg[0]) + ' - ' + msg[1])

		# close the connection
		if self.video_socket is not None:
			print('close video socket ' + self.name)
			self.video_socket.close()
		if self.output.contains_connection(self.name):
			self.output.remove_connection(self.name)
		self.command_socket.close()
		print('closed command connection with ' + self.name)

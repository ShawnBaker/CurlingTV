import socket
import threading
from multi_socket_output import *
from settings import *
from video import *

RECEIVE_SIZE = 1024

# handles a command connection
class Connection (threading.Thread):

	def __init__(self, name, command_socket, output):
		threading.Thread.__init__(self)
		self.name = name
		self.command_socket = command_socket
		self.output = output
		self.video_socket = None
		self.video = None
		self.port = 0

	def run(self):
		# read commands forever
		while True:
			 
			# read a command from the client
			command = self.command_socket.recv(RECEIVE_SIZE)
			if not command: 
				break
			#print 'command = ' + command

			if command == 'get_name':
				self.command_socket.sendall(DEVICE_NAME)
			elif command == 'get_video_params':
				params = "%d,%d,%d,%d" % (WIDTH, HEIGHT, FPS, BPS)
				self.command_socket.sendall(params)
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
						self.port = self.video_socket.getsockname()[1]
						print 'video socket on port %d' % self.port
					
						# start a new video thread
						self.video = Video(self.name, self.video_socket, self.output)
						self.video.start()
					
					# report the port number back to the client
					self.command_socket.sendall(str(self.port))
				except socket.error, msg:
					print 'Bind Error: ' + str(msg[0]) + ' - ' + msg[1]

		# close the connection
		if self.video_socket is not None:
			self.video_socket.close()
		if self.output.contains_connection(self.name):
			self.output.remove_connection(self.name)
		self.command_socket.close()
		print 'closed command connection with ' + self.name

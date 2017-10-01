import logging
import socket
import threading
from multi_socket_output import *

# handles a video connection
class Video (threading.Thread):

	def __init__(self, name, sock, output):
		threading.Thread.__init__(self)
		self.name = name
		self.sock = sock
		self.output = output
		self.logger = logging.getLogger("camera")

	def run(self):
		# wait for the client to connect
		self.sock.listen(1)

		conn, addr = self.sock.accept()
		self.logger.info('video connection from ' + addr[0] + ':' + str(addr[1]))
		
		# add the socket to the list of output sockets
		self.output.add_connection(self.name, conn)

import io
import logging
import socket
import struct
import threading
from multi_socket_output import *

# handles a video connection
class Image (threading.Thread):

	def __init__(self, sock, camera):
		threading.Thread.__init__(self)
		self.sock = sock
		self.camera = camera
		self.logger = logging.getLogger("camera")

	def run(self):
		# list for one connection
		self.sock.listen(1)
		self.logger.info('image: listening')

		# wait for the client to connect
		conn, addr = self.sock.accept()
		self.logger.info('image: connection from ' + addr[0] + ':' + str(addr[1]))
		connFile = conn.makefile('wb')

		# get an image from the camera
		stream = io.BytesIO()
		#camera = picamera.PiCamera()
		self.camera.capture(stream, 'jpeg', use_video_port=True)
		self.logger.info('image: captured size = ' + str(stream.tell()))

		# write the image length
		connFile.write(struct.pack('<L', stream.tell()))
		connFile.flush()
		self.logger.info('image: wrote size')
			
		# write the image data
		stream.seek(0)
		connFile.write(stream.read())
		self.logger.info('image: wrote data')

		# close the connection
		connFile.close()
		conn.close()
		self.sock.close()
		self.logger.info('image: closed')
	
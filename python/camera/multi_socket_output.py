import logging
import picamera

class MultiSocketOutput(object):
	def __init__(self):
		self.connections = {}
		self.logger = logging.getLogger("camera")

	def write(self, s):
		remove = []
		for name, conn in self.connections.items():
			try:
				conn.sendall(s)
			except socket.error as msg:
				self.logger.error('socket error')
				remove.append(name)
			except socket.timeout:
				self.logger.error('socket timeout')
		for name in remove:
			self.remove_connection(name)

	def flush(self):
		self.logger.info('flush')
		
	def add_connection(self, name, conn):
		self.connections[name] = conn
		self.logger.info('add %d' % len(self.connections))
		
	def remove_connection(self, name):
		try:
			self.connections[name].close()
		except socket.error as msg:
			self.logger.error('close socket error: ' + msg)
		del self.connections[name]
		self.logger.info('remove %d' % len(self.connections))

	def contains_connection(self, name):
		return name in self.connections

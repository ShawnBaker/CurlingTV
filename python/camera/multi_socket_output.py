import picamera

class MultiSocketOutput(object):
	def __init__(self):
		self.connections = {}

	def write(self, s):
		remove = []
		for name, conn in self.connections.iteritems():
			try:
				conn.sendall(s)
			except socket.error, msg:
				print 'socket error'
				remove.append(name)
			except socket.timeout:
				print 'socket timeout'
		for name in remove:
			self.remove_connection(name)

	def flush(self):
		print('flush')
		
	def add_connection(self, name, conn):
		self.connections[name] = conn
		print 'add %d' % len(self.connections)
		
	def remove_connection(self, name):
		try:
			self.connections[name].close()
		except socket.error, msg:
			print 'close socket error: ' + msg
		del self.connections[name]
		print 'remove %d' % len(self.connections)

	def contains_connection(self, name):
		return name in self.connections

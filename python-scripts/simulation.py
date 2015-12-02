import fnss
import networkx as nx
from networkx.algorithms import bipartite
from math import log, floor
from networkx.utils import create_degree_sequence
#import pylab as pl
#import numpy as np
#import powerlaw

#try:
#    import matplotlib.pyplot as plt
#    import matplotlib
#except:
#    raise



class path(object):

	def __init__(self, src, dst, ancestors = set(), length = 0):
		self.src = src
		self.dst = dst
		self.ancestors = ancestors
		self.length = length

	def __str__(self):
		return str(self.src) + '-' + str(self.dst)

	def __eq__(self, other):
		return (self.src == other.src and self.dst == other.dst) or \
			(self.src == other.dst and self.dst == other.src)

	def __hash__(self):
		if self.src < self.dst:
			return(hash((self.src, self.dst)))
		else:
			return(hash((self.dst, self.src)))

	def is_sub_path(self, other):
		return self in other.ancestors

	def is_conntected(self, other):
		return ((self.src == other.src) or (self.src == other.dst) or \
			(self.dst == other.src) or self.dst == other.dst) and \
			not self.is_sub_path(other) and not other.is_sub_path(self)
		
	def connect(self, other):
		if self.src == other.src:
			return (self.src, path(self.dst, other.dst, \
				self.ancestors.union(other.ancestors).union([self, other]), \
				self.length + other.length))

		if self.src == other.dst:
			return (self.src, path(self.dst, other.src, \
				self.ancestors.union(other.ancestors).union([self, other]), \
				self.length + other.length))


		if self.dst == other.src:
			return (self.dst, path(self.src, other.dst, \
				self.ancestors.union(other.ancestors).union([self, other]), \
				self.length + other.length))


		if self.dst == other.dst:
			return (self.dst, path(self.src, other.src, \
				self.ancestors.union(other.ancestors).union([self, other]), \
				self.length + other.length))


		return (None, None)	


def generate_random_graph_powerlaw(n):
	m = 1
	p = 0.7
	#H = powerlaw_cluster_graph(n, m, p)

	gamma = 2.5
	powerlaw_gamma = lambda N: nx.utils.powerlaw_sequence(N, exponent=gamma)
	z = nx.utils.create_degree_sequence(n, powerlaw_gamma, max_tries=1000)
	G = nx.configuration_model(z)

	while not nx.is_connected(G):
		powerlaw_gamma = lambda N: nx.utils.powerlaw_sequence(N, exponent=gamma)
		z = nx.utils.create_degree_sequence(n, powerlaw_gamma, max_tries=100)
		G = nx.configuration_model(z)

	G = [n for n in nx.connected_component_subgraphs(G)][0]
	G.remove_edges_from(G.selfloop_edges())

	return G

def generate_topology(params):
	
	#t = fnss.fat_tree_topology(8)

	#topology = nx.Graph()
	#topology.add_edges_from(t.edges())
	#topology.add_edges_from([(0,1) , (0,2), (0,3), (1,4), (1,5), (2,6), (2,7), (3,8), (3,9)])
	#topology.add_edges_from([(1,4) , (2,4), (3,4), (4,5), (5,6), (6,7), (7,8), (8,9), (8,10), (8,11)])
	#topology.add_edges_from([(0,1) , (0,2), (1,4), (1,5), (2,6), (2,7)])

	topology = generate_random_graph_powerlaw(params[0])
	return topology

def find_all_Shortest_paths(topology):

	edge_switches = []
	core_switches = []
	for n in topology.nodes():
		if topology.degree(n) == 1:
			edge_switches.append(n)
		else:
			core_switches.append(n)

	shortest_paths = {}
	
	visited = []
	for n1 in edge_switches:
		visited.append(n1)
		for n2 in edge_switches:
			if not n2 in visited:
				p = nx.shortest_path(topology, source = n1, target = n2)
				shortest_paths[path(n1, n2)] = p

	return shortest_paths


def make_path_graph(shortest_paths):
	paths_graph = nx.Graph()
	
	set_a = shortest_paths.keys()
	paths_graph.add_nodes_from(set_a, bipartite = 0)

	#set_b = 
	for k in set_a:
		pre = None
		for node in shortest_paths[k]:
			if pre != None:
				p = path(pre, node, length = 1)
				paths_graph.add_node(p, bipartite = 1)
				paths_graph.add_edge(k, p)
			pre = node

	return paths_graph


def aggregate(paths_graph):

	paths_graph_c = paths_graph.copy()
	sub_paths = [n for n,d in paths_graph_c.nodes(data=True) if d['bipartite'] == 1]
	

	#print('#####')
	changed_flag = True
	while changed_flag:
		sub_paths = remove_low_degrees_sort(paths_graph_c, sub_paths)
		changed_flag = False
		visited = []

		#for n in sub_paths:
		#	print n , paths_graph_c.degree(n)
		
		for p1 in sub_paths:
			visited.append(p1)
			s1 = set(paths_graph_c.neighbors(p1)) 

			for p2 in sub_paths:
				if not p2 in visited and p1.is_conntected(p2):
					
					s2 = set(paths_graph_c.neighbors(p2))
					shared_neighbours = s1.intersection(s2)
					
					if len(shared_neighbours) > 2 and \
						((abs(floor(log(p1.length, 2)) - floor(log(p2.length, 2))) < 1) or \
						abs(p1.length - p2.length) < 2):
			

						_, new_path = p1.connect(p2)

						#print p1, ' and ', p2, ' -> ', new_path
						#add new node, remove old edges
						paths_graph_c.add_node(new_path, bipartite = 1)

						for p in shared_neighbours:
							paths_graph_c.add_edge(new_path, p)
							paths_graph_c.remove_edge(p1, p)
							paths_graph_c.remove_edge(p2, p)

						sub_paths.append(new_path)
						#print('------')
						#for n in [n for n,d in paths_graph_c.nodes(data=True) if d['bipartite'] == 1]:
						#	print n, paths_graph_c.degree(n)
						#print('#######')

						changed_flag = True
						break

				if changed_flag:
					break


	return paths_graph_c


def remove_low_degrees_sort(graph, node_list):
	result_temp = []
	
	for n in node_list:
		d = graph.degree(n)
		if d > 2:
			result_temp.append((n, d))

 	result_temp.sort(key = lambda tup: tup[1], reverse = True)

 	return [n[0] for n in result_temp]

def count_rules(graph):

 	count = len(graph.edges())

 	count += len([n for n,d in graph.nodes(data=True) if d['bipartite'] == 0])

 	visited = []
 	for n in [n for n,d in graph.nodes(data=True) if d['bipartite'] == 1]:
 		if not n in visited:
 			visited.append(n)
			count += len(n.ancestors) + 1

 	return count

def test(topology):
 	shortest_paths = find_all_Shortest_paths(topology)
	paths_graph = make_path_graph(shortest_paths)
	r1 = count_rules(paths_graph)
	print(len(paths_graph.edges()))
	print(r1)
	aggregated_paths_graph = aggregate(paths_graph)
	r2 = count_rules(aggregated_paths_graph)
	print(len(aggregated_paths_graph.edges()))
	print(r2)
	return ((r1 - r2) * 100.0) / r1

if __name__ == '__main__':
	# create a topology with 10 core switches, 20 edge switches and 10 hosts
	# per switch (i.e. 200 hosts in total)
	
	# repeat = 10
	# param = [50]#, 100, 200, 500, 1000]

	# result = []
	# for n in param:
	# 	improve = 0.0
	# 	for i in range(repeat):
	# 		topology = generate_topology([n])
	# 		improve += test(topology)
	# 	result.append((n, improve/repeat))

	# print(result)
	g1 = nx.read_graphml('/home/pooya/Bin/floodlight/python-scripts/Chinanet.graphml')
	#print(test(g1))
	g2 = nx.read_graphml('/home/pooya/Bin/floodlight/python-scripts/Bellcanada.graphml')
	print(test(g2))
	


	
	

	
	

	
	
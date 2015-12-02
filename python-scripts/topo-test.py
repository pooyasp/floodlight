"""Custom topology example

Two directly connected switches plus a host for each switch:

   host --- switch --- switch --- host

Adding the 'topos' dict with a key/value pair to generate our newly defined
topology enables one to pass in '--topo=mytopo' from the command line.
"""

from mininet.topo import Topo

class MyTopo( Topo ):
    "Simple topology example."

    def __init__( self ):
        "Create custom topo."

        # Initialize topology
        Topo.__init__( self )

        # Add hosts and switches
        h1 = self.addHost( 'h1', mac = '00:00:00:00:01:01' )
        h2 = self.addHost( 'h2', mac = '00:00:00:00:01:02' )
        h3 = self.addHost( 'h3', mac = '00:00:00:00:01:03' )
        h4 = self.addHost( 'h4', mac = '00:00:00:00:01:04' )
        h5 = self.addHost( 'h5', mac = '00:00:00:00:01:05' )
        h6 = self.addHost( 'h6', mac = '00:00:00:00:01:06' )
        h7 = self.addHost( 'h7', mac = '00:00:00:00:01:07' )
        h8 = self.addHost( 'h8', mac = '00:00:00:00:01:08' )
        h9 = self.addHost( 'h9', mac = '00:00:00:00:01:09' )
        h10 = self.addHost( 'h10', mac = '00:00:00:00:01:0a' )
        


        s1 = self.addSwitch( 's1' )
        s2 = self.addSwitch( 's2' )
        s3 = self.addSwitch( 's3' )
        s4 = self.addSwitch( 's4' )
        s5 = self.addSwitch( 's5' )
        s6 = self.addSwitch( 's6' )
        s7 = self.addSwitch( 's7' )
        s8 = self.addSwitch( 's8' )
        s9 = self.addSwitch( 's9' )
        s10 = self.addSwitch( 's10' )


        # Add links
        self.addLink( h1, s1 )
        #self.addLink( h2, s1 )
        self.addLink( h2, s3 )
        self.addLink( h3, s3 )
        #self.addLink( h4, s3 )
        self.addLink( h4, s8 )
        self.addLink( h5, s8 )
        self.addLink( s1, s3 )
        self.addLink( s8, s7 )
        self.addLink( s3, s7 )
        self.addLink( s3, s2 )
        self.addLink( s3, s4 )
        self.addLink( s2, s4 )
        self.addLink( s7, s6 )
        self.addLink( s4, s9 )
        self.addLink( s2, s5 )
        self.addLink( s9, s10 )
        self.addLink( s5, h6 )
        #self.addLink( s5, h8 )
        self.addLink( s6, h7 )
        self.addLink( s6, h9 )
        #self.addLink( s6, h10 )
        self.addLink( s10, h8 )
        self.addLink( s10, h10 )
        

topos = { 'mytopo': ( lambda: MyTopo() ) }

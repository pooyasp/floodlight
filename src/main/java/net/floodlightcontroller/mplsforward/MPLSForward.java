package net.floodlightcontroller.mplsforward;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionNicira;
import org.projectfloodlight.openflow.protocol.action.OFActionNiciraResubmit;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;
import net.floodlightcontroller.util.OFMessageDamper;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.jboss.netty.util.Timer;


public class MPLSForward implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<DatapathId> edgeSwitchesIDs;
	protected static Logger logger;
	protected IDeviceService deviceService;
	protected IRoutingService routingService;
	protected IOFSwitchService switchService;
	protected OFMessageDamper messageDamper;

	public static final int MPLS_FORWARD_INSTALLATION = 101;


	@Override
	public String getName() {
		return MPLSForward.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		this.edgeSwitchesIDs = new ConcurrentSkipListSet<DatapathId>();
		MPLSForward.logger = LoggerFactory.getLogger(MPLSForward.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);
		this.routingService = context.getServiceImpl(IRoutingService.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.messageDamper = new OFMessageDamper(10000, EnumSet.of(OFType.FLOW_MOD), 250);

		AppCookie.registerApp(MPLS_FORWARD_INSTALLATION, "MPLS FORWARD INSTALL");

	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				installRules();
			}
		}, 20000);

	}

	public void installRules(){
		logger.info("INSTALLING RULES!!!!!!!!!!!!!!!!!");

		for (IDevice i : this.deviceService.getAllDevices()){
			logger.info(i.getAttachmentPoints().length + " " + i.getMACAddressString());

			for (SwitchPort sp: i.getAttachmentPoints()){
				//SwitchPort sp = i.getAttachmentPoints()[0];
				logger.info(sp.getSwitchDPID().toString() + "    " + i.getMACAddressString());
				this.edgeSwitchesIDs.add(sp.getSwitchDPID());
			}
		}

		logger.info(this.edgeSwitchesIDs.size() + "");
		for (DatapathId id: this.edgeSwitchesIDs) {
			logger.info(id.toString());
		}

		int count = 0;
		for (DatapathId dp1 : this.edgeSwitchesIDs){
			for(DatapathId dp2 : this.edgeSwitchesIDs){
				if (!dp1.equals(dp2)) {

					Route route = routingService.getRoute(dp1, dp2, U64.of(0));
					logger.info(route.getPath().toString());
					count ++;
					pushRouteForEdgeSwitch(route);
				}
			}
		}

	}

	public void pushRouteForEdgeSwitch(Route route) {
		logger.info("HOSSEIN_POOYA_PushROute");
		List<NodePortTuple> switchPortList = route.getPath();
		int mplsId = (int)(switchPortList.get(switchPortList.size() -1 ).getNodeId()).getLong();
		U64 cookie = AppCookie.makeCookie(MPLS_FORWARD_INSTALLATION, 0);


		for (int indx = 0 ; indx < switchPortList.size() - 1; indx += 2) {

			DatapathId switchDPID = switchPortList.get(indx).getNodeId();
			IOFSwitch sw = switchService.getSwitch(switchDPID);

			Match.Builder mb = sw.getOFFactory().buildMatch();

			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();

			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();

			List<OFAction> actions = new ArrayList<OFAction>();
			
			mb.setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST);
			mb.setExact(MatchField.MPLS_LABEL, U32.of(mplsId));
			mb.setExact(MatchField.MPLS_TC, U8.of((short)4)); 
			logger.debug("matching mpls on switch: " + sw.toString() + " mpls label:" + mplsId);

			OFPort outPort = switchPortList.get(indx).getPortId();

			aob.setPort(outPort);
			

			actions.add(aob.build());
			aob.setMaxLen(Integer.MAX_VALUE);


			List<OFInstruction> instructions = new ArrayList<OFInstruction>();
			instructions.add(sw.getOFFactory().instructions().applyActions(actions));

			// compile
			fmb.setMatch(mb.build()) // was match w/o modifying input port
			.setActions(actions)
			.setIdleTimeout(0)
			.setHardTimeout(0)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort)
			.setPriority(10);

			try {
				if (logger.isTraceEnabled()) {
					logger.trace("Pushing Route flowmod routeIndx={} " +
							"sw={} inPort={} outPort={}",
							new Object[] {indx,
									sw,
									fmb.getMatch().get(MatchField.IN_PORT),
									outPort });
				}
				messageDamper.write(sw, fmb.build());
				sw.flush();


				// Push the packet out the source switch
				//if (sw.getId().equals(pinSwitch)) {
				// TODO: Instead of doing a packetOut here we could also
				// send a flowMod with bufferId set....
				//pushPacket(sw, pi, false, outPort, cntx);
				//srcSwitchIncluded = true;
				//}
			} catch (IOException e) {
				logger.error("Failure writing flow mod", e);
			}

		}

		//Last switch: pop MPLS, resubmit

		DatapathId switchDPID = switchPortList.get(switchPortList.size()-1).getNodeId();
		IOFSwitch sw = switchService.getSwitch(switchDPID);

		Match.Builder mb = sw.getOFFactory().buildMatch();
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		

		mb.setExact(MatchField.ETH_TYPE, EthType.MPLS_UNICAST);
		mb.setExact(MatchField.MPLS_LABEL, U32.of(mplsId));
		mb.setExact(MatchField.MPLS_TC, U8.of((short)4)); 
		logger.debug("matching mpls on switch: " + sw.toString() + " mpls label:" + mplsId);

		//OFPort outPort = switchPortList.get(switchPortList.size()-1).getPortId();

		//aob.setPort(outPort);

		actions.add(sw.getOFFactory().actions().popMpls(EthType.IPv4));

		//aob.setMaxLen(Integer.MAX_VALUE);
		OFActionNiciraResubmit.Builder ofnicr = OFFactories.getFactory(OFVersion.OF_13).actions().buildNiciraResubmit();
		ofnicr.setTable((short)(0));
		actions.add(ofnicr.build());
		
		List<OFInstruction> instructions = new ArrayList<OFInstruction>();

		//this is to goto table 5
		//OFInstructionGotoTable.Builder ib = sw.getOFFactory().instructions().buildGotoTable();
		//ib.setTableId(TableId.of(5)); //I have a bad feeling about this
		//instructions.add(ib.build());
		instructions.add(sw.getOFFactory().instructions().applyActions(actions));

		
		fmb.setMatch(mb.build()) // was match w/o modifying input port
		.setActions(actions)
		.setIdleTimeout(0)
		.setInstructions(instructions)
		.setHardTimeout(0)
		.setBufferId(OFBufferId.NO_BUFFER)
		.setCookie(cookie)
		.setPriority(10);
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Pushing Route flowmod routeIndx={} " +
						"sw={} inPort={} sendTo-tableId={}",
						new Object[] {switchPortList.size()-1,
								sw,
								fmb.getMatch().get(MatchField.IN_PORT),
								0 });
			}
			messageDamper.write(sw, fmb.build());
			sw.flush();

			// Push the packet out the source switch
			//if (sw.getId().equals(pinSwitch)) {
			// TODO: Instead of doing a packetOut here we could also
			// send a flowMod with bufferId set....
			//pushPacket(sw, pi, false, outPort, cntx);
			//srcSwitchIncluded = true;
			//}
		} catch (IOException e) {
			logger.error("Failure writing flow mod", e);
		}



	}

	protected void doForwardFlow(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx, boolean requestFlowRemovedNotifn) {
		OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		// Check if we have the location of the destination
		
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
		
		
		IDevice dstDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);

		//IDevice dstDevice = deviceService.Device(dstMac.getLong());
		
		logger.info("inja tu doForwardFlow "  +dstDevice) ;

		
		if (dstDevice != null) {
			IDevice srcDevice = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);
			//DatapathId srcIsland = topologyService.getL2DomainId(sw.getId());

			if (srcDevice == null) {
				logger.debug("No device entry found for source device");
				return;
			}

			// Validate that the source and destination are not on the same switchport
			boolean on_same_if = false;
			for (SwitchPort dstDap : dstDevice.getAttachmentPoints()) {
				DatapathId dstSwDpid = dstDap.getSwitchDPID();
				if (sw.getId().equals(dstSwDpid) && inPort.equals(dstDap.getPort())) {
					on_same_if = true;
					System.err.println("kar nabayad be inja miresid!! :( (same switch)");
				}
				break;
			}

			SwitchPort srcDap = srcDevice.getAttachmentPoints()[0];
			SwitchPort dstDap = dstDevice.getAttachmentPoints()[0];

			U64 cookie = AppCookie.makeCookie(MPLS_FORWARD_INSTALLATION, 0);
			Match m = createMatchFromPacket(sw, inPort, cntx);
			

			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			List<OFAction> actions = new ArrayList<OFAction>();

			int mplsTag = (int)(dstDap.getSwitchDPID()).getLong();
			
			Route route =
					routingService.getRoute(srcDap.getSwitchDPID(), 
							srcDap.getPort(),
							dstDap.getSwitchDPID(),
							dstDap.getPort(), U64.of(0));
			
			List<NodePortTuple> switchPortList = route.getPath();
			logger.info("switch list: " + switchPortList);
		
			actions.add(sw.getOFFactory().actions().pushMpls(EthType.MPLS_UNICAST));
	        actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().mplsLabel(U32.of(mplsTag))));
	        actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().mplsTc(U8.of((short)4))));
	        OFPort outPort = switchPortList.get(1).getPortId();
	        logger.info("output port:" + outPort );
			aob.setPort(outPort);
			actions.add(aob.build());
			aob.setMaxLen(Integer.MAX_VALUE);

			List<OFInstruction> instructions = new ArrayList<OFInstruction>();
			instructions.add(sw.getOFFactory().instructions().applyActions(actions));

//			List<OFInstruction> instructions = new ArrayList<OFInstruction>();
//			OFInstructionGotoTable.Builder ib = sw.getOFFactory().instructions().buildGotoTable();
//			ib.setTableId(TableId.of(1)); //I have a bad feeling about this
//			instructions.add(ib.build());
//			instructions.add(sw.getOFFactory().instructions().applyActions(actions));


			fmb.setMatch(m) // was match w/o modifying input port
			.setActions(actions)
			.setIdleTimeout(100)
			.setInstructions(instructions)
			.setHardTimeout(150)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setPriority(1);
			try {
				if (logger.isTraceEnabled()) {
					logger.trace("Pushing Route flowmod routeIndx={} " +
							"sw={} inPort={} sendTo-tableId={}",
							new Object[] {switchPortList.size()-1,
									sw,
									fmb.getMatch().get(MatchField.IN_PORT),
									0 });
				}
				messageDamper.write(sw, fmb.build());
				sw.flush();

				// Push the packet out the source switch
				//if (sw.getId().equals(pinSwitch)) {
				// TODO: Instead of doing a packetOut here we could also
				// send a flowMod with bufferId set....
				pushPacket(sw, pi, false, outPort, mplsTag,cntx);
				//srcSwitchIncluded = true;
				//}
			} catch (IOException e) {
				logger.error("Failure writing flow mod", e);
			}


		}
	}

	protected Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {


		boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
		boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
		boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
		boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;


		// The packet in match will only contain the port number.
		// We need to add in specifics for the hosts we're routing between.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
		

		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort);

		if (FLOWMOD_DEFAULT_MATCH_MAC) {
			mb.setExact(MatchField.ETH_SRC, srcMac)
			.setExact(MatchField.ETH_DST, dstMac);
		}

		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}

		// TODO Detect switch type and match to create hardware-implemented flow
		if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();

			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setExact(MatchField.IPV4_SRC, srcIp)
				.setExact(MatchField.IPV4_DST, dstIp);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				/*
				 * Take care of the ethertype if not included earlier,
				 * since it's a prerequisite for transport ports.
				 */
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}

				if (ip.getProtocol().equals(IpProtocol.TCP)) {
					TCP tcp = (TCP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
					.setExact(MatchField.TCP_SRC, tcp.getSourcePort())
					.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
				} else if (ip.getProtocol().equals(IpProtocol.UDP)) {
					UDP udp = (UDP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_SRC, udp.getSourcePort())
					.setExact(MatchField.UDP_DST, udp.getDestinationPort());
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { /* shallow check for equality is okay for EthType */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		} else if (eth.getEtherType() == EthType.IPv6) {
			IPv6 ip = (IPv6) eth.getPayload();
			IPv6Address srcIp = ip.getSourceAddress();
			IPv6Address dstIp = ip.getDestinationAddress();

			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv6)
				.setExact(MatchField.IPV6_SRC, srcIp)
				.setExact(MatchField.IPV6_DST, dstIp);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				/*
				 * Take care of the ethertype if not included earlier,
				 * since it's a prerequisite for transport ports.
				 */
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv6);
				}

				if (ip.getNextHeader().equals(IpProtocol.TCP)) {
					TCP tcp = (TCP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
					.setExact(MatchField.TCP_SRC, tcp.getSourcePort())
					.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
				} else if (ip.getNextHeader().equals(IpProtocol.UDP)) {
					UDP udp = (UDP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.UDP_SRC, udp.getSourcePort())
					.setExact(MatchField.UDP_DST, udp.getDestinationPort());
				}
			}
		}
		return mb.build();
	}

	private Command processPacketInMessage(IOFSwitch sw, OFPacketIn pi, FloodlightContext cntx) {
		//OFPort inPort = (pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT));
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
		//logger.info("received message src mac : " + srcMac + " destination mac:" +dstMac );
		if (eth.getEtherType() == EthType.IPv4) { /* shallow check for equality is okay for EthType */
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();
			logger.info("received message src IP : " + srcIp + " destination ip:" +dstIp);
		}
		
		
		if (eth.isBroadcast() || eth.isMulticast()) {
			//doFlood(sw, pi, cntx);
			//logger.info("INO NABAYAD BEBINIM", pi);
		}
		else
			doForwardFlow(sw, pi, cntx, false);


		
		/*
		// We found a routing decision (i.e. Firewall is enabled... it's the only thing that makes RoutingDecisions)
		if (decision != null) {
			switch(decision.getRoutingAction()) {
			case NONE:
				// don't do anythings
				return Command.CONTINUE;
			case FORWARD_OR_FLOOD:
			case FORWARD:
				doForwardFlow(sw, pi, cntx, false);
				return Command.CONTINUE;
			case MULTICAST:
				// treat as broadcast
				//doFlood(sw, pi, cntx);
				return Command.CONTINUE;
			case DROP:
				//doDropFlow(sw, pi, decision, cntx);
				return Command.CONTINUE;
			default:
				logger.error("Unexpected decision made for this packet-in={}", pi, decision.getRoutingAction());
				return Command.CONTINUE;
			}
		} else { // No routing decision was found. Forward to destination or flood if bcast or mcast.
			if (logger.isTraceEnabled()) {
				logger.trace("No decision was made for PacketIn={}, forwarding", pi);
			}

			if (eth.isBroadcast() || eth.isMulticast()) {
				//doFlood(sw, pi, cntx);
				logger.trace("INO NABAYAD BEBINIM", pi);

			} else {
				doForwardFlow(sw, pi, cntx, false);
			}
		}
		*/

		return Command.CONTINUE;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
		case PACKET_IN:
			return this.processPacketInMessage(sw, (OFPacketIn) msg, cntx);
		case FLOW_REMOVED:
			logger.info("received an removed flow {} from switch {}", msg, sw);

			//return this.processFlowRemovedMessage(sw, (OFFlowRemoved) msg);
		case ERROR:
			logger.info("received an error {} from switch {}", msg, sw);
			return Command.CONTINUE;
		default:
			logger.error("received an unexpected message {} from switch {}", msg, sw);
			return Command.CONTINUE;
		}
	}


	/**
	 * Pushes a packet-out to a switch.  The assumption here is that
	 * the packet-in was also generated from the same switch.  Thus, if the input
	 * port of the packet-in and the outport are the same, the function will not
	 * push the packet-out.
	 * @param sw        switch that generated the packet-in, and from which packet-out is sent
	 * @param pi        packet-in
	 * @param useBufferId  if true, use the bufferId from the packet in and
	 * do not add the packetIn's payload. If false set bufferId to
	 * BUFFER_ID_NONE and use the packetIn's payload
	 * @param outport   output port
	 * @param cntx      context of the packet
	 */
	protected void pushPacket(IOFSwitch sw, OFPacketIn pi, boolean useBufferId,
			OFPort outport, int MPLSId,FloodlightContext cntx) {

		if (pi == null) {
			return;
		}

		// The assumption here is (sw) is the switch that generated the
		// packet-in. If the input port is the same as output port, then
		// the packet-out should be ignored.
		if ((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)).equals(outport)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Attempting to do packet-out to the same " +
						"interface as packet-in. Dropping packet. " +
						" SrcSwitch={}, pi={}",
						new Object[]{sw, pi});
				return;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("PacketOut srcSwitch={} pi={}",
					new Object[] {sw, pi});
		}

		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();

		actions.add(sw.getOFFactory().actions().pushMpls(EthType.MPLS_UNICAST));
        actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().mplsLabel(U32.of(MPLSId))));
        actions.add(sw.getOFFactory().actions().setField(sw.getOFFactory().oxms().mplsTc(U8.of((short)4))));
        
		actions.add(sw.getOFFactory().actions().output(outport, Integer.MAX_VALUE));
		pob.setActions(actions);

		if (useBufferId) {
			pob.setBufferId(pi.getBufferId());
		} else {
			pob.setBufferId(OFBufferId.NO_BUFFER);
		}

		if (pob.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pi.getData();
			pob.setData(packetData);
		}

		pob.setInPort((pi.getVersion().compareTo(OFVersion.OF_12) < 0 ? pi.getInPort() : pi.getMatch().get(MatchField.IN_PORT)));

		try {
			messageDamper.write(sw, pob.build());
		} catch (IOException e) {
			logger.error("Failure writing packet out", e);
		}
	}

	
}
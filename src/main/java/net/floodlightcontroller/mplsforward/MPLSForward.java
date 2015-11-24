package net.floodlightcontroller.mplsforward;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;

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
		this.logger = LoggerFactory.getLogger(MPLSForward.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);
		this.routingService = context.getServiceImpl(IRoutingService.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.messageDamper = new OFMessageDamper(10000, EnumSet.of(OFType.FLOW_MOD), 250);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		//floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				installRules();
			}
		}, 25000);

	}

	public void installRules(){
		logger.info("POOYA_HOSSEIN_YOLO_SWAG_BOOTY_420");
		
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
					if (count == 2) {pushRoute(route);}
				}
			}
		}
		
	}
	
	public void pushRoute(Route route) {
		logger.info("HOSSEIN_POOYA_YOLO_SWAG_BOOTY420_PushROute");
		List<NodePortTuple> switchPortList = route.getPath();
		int mplsId = (int)(long)(switchPortList.get(switchPortList.size() -1 ).getNodeId()).getLong();
		U64 cookie = AppCookie.makeCookie(mplsId, 0);
		
	
		

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
			.setIdleTimeout(100)
			.setHardTimeout(150)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort)
			.setPriority(1);

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
			
	}
	

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		/*Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		Long sourceMACHash = eth.getSourceMACAddress().getLong();
		if (!macAddresses.contains(sourceMACHash)) {
			if(!edgeSwitchesIDs.contains(sw.getId()))
			{
				this.macAddresses.add(sourceMACHash);
				this.edgeSwitchesIDs.add(sw.getId());
				this.edgeSwitches.add(sw);
				logger.info("MAC Address: {} seen on switch: {}",
	                    eth.getSourceMACAddress().toString(),
	                    sw.getId().toString());
			}
		}
		return Command.CONTINUE;*/
		return null;
	}

}
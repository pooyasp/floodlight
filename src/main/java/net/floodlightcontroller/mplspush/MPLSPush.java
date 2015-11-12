package net.floodlightcontroller.mplspush;

import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.jboss.netty.util.Timer;


public class MPLSPush implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected ArrayList<IOFSwitch> edgeSwitches;
	protected Set<DatapathId> edgeSwitchesIDs;
	protected static Logger logger;
	//protected ITopologyService topologyService;
	protected IDeviceService deviceService;

	@Override
	public String getName() {
		return MPLSPush.class.getSimpleName();
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
		this.macAddresses = new ConcurrentSkipListSet<Long>();
		this.edgeSwitches = new ArrayList<IOFSwitch>();
		this.edgeSwitchesIDs = new ConcurrentSkipListSet<DatapathId>();
		this.logger = LoggerFactory.getLogger(MPLSPush.class);
		//this.topologyService = context.getServiceImpl(ITopologyService.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);

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
			for (SwitchPort sp: i.getAttachmentPoints()){
				logger.info(sp.getSwitchDPID().toString() + "    " + i.getMACAddressString());
				this.edgeSwitchesIDs.add(sp.getSwitchDPID());
			}
		}
		
		logger.info(this.edgeSwitchesIDs.size() + "");
		for (DatapathId id: this.edgeSwitchesIDs) {
			logger.info(id.toString());
		}
		
		//if you want switch obj by DPID: floodlightprovider.getSwitches().get(DPID)
		
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
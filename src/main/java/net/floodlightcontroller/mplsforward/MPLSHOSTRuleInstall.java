package net.floodlightcontroller.mplsforward;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.util.OFMessageDamper;

public class MPLSHOSTRuleInstall implements IFloodlightModule, IDeviceListener {
	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IOFSwitchService switchService;
	protected IDeviceService deviceService;
	protected OFMessageDamper messageDamper;
	
	public static final int MPLS_HOST_RULE_INSTALLATION = 100;
	
	//static {
	//	AppCookie.registerApp(MPLS_HOST_RULE_INSTALLATION, "EDGE RULE INSTALLATION");
	//}
	

	@Override
	public String getName() {
		return MPLSHOSTRuleInstall.class.getSimpleName();
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
		l.add(IDeviceService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
	    this.floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		this.deviceService = context.getServiceImpl(IDeviceService.class);
		this.logger = LoggerFactory.getLogger(MPLSHOSTRuleInstall.class);
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		this.messageDamper = new OFMessageDamper(10000, EnumSet.of(OFType.FLOW_MOD), 250);
		
		AppCookie.registerApp(MPLS_HOST_RULE_INSTALLATION, "MPLS EDGE INSTALL");
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
	    deviceService.addListener(this);
	}


	@Override
	public boolean isCallbackOrderingPrereq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean isCallbackOrderingPostreq(String type, String name) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void deviceAdded(IDevice device) {
		for (SwitchPort sp: device.getAttachmentPoints()){
			logger.info("HOSSEIN_POOYA_YOLO420_DEVICE_ADDED");
			logger.info(sp.getSwitchDPID().toString() + "    " + device.getMACAddressString());
			
			IOFSwitch sw = switchService.getSwitch(sp.getSwitchDPID());
			
			Match.Builder mb = sw.getOFFactory().buildMatch();
			OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			
			List<OFAction> actions = new ArrayList<OFAction>();
			
			mb.setExact(MatchField.ETH_DST, device.getMACAddress());
			
			OFPort outPort = sp.getPort();
			
			aob.setPort(outPort);
			
			actions.add(aob.build());
			aob.setMaxLen(Integer.MAX_VALUE);
			
			List<OFInstruction> instructions = new ArrayList<OFInstruction>();
			instructions.add(sw.getOFFactory().instructions().applyActions(actions));
			
			U64 cookie = AppCookie.makeCookie(MPLS_HOST_RULE_INSTALLATION, 0);
			//U64 cookie = AppCookie.makeCookie(10001, 0);
			
			// compile
			fmb.setMatch(mb.build()) // was match w/o modifying input port
			.setActions(actions)
			.setIdleTimeout(0)
			.setTableId(TableId.of(5))
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort)
			.setPriority(1);
			
			
			try {
				messageDamper.write(sw, fmb.build());
				sw.flush();
			} catch (IOException e) {
				logger.error("Failure writing flow mod", e);
			}
			
		}
		
		
	}


	@Override
	public void deviceRemoved(IDevice device) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deviceMoved(IDevice device) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deviceIPV4AddrChanged(IDevice device) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deviceIPV6AddrChanged(IDevice device) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void deviceVlanChanged(IDevice device) {
		// TODO Auto-generated method stub
		
	}

	
}
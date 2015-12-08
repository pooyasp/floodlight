package net.floodlightcontroller.hosseinTest;

import java.util.Collection;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;

import net.floodlightcontroller.packet.Ethernet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class test1 implements IOFMessageListener, IFloodlightModule {

	
	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	
	

	@Override
	public String getName() {
	    return test1.class.getSimpleName();
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
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
	    floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	    macAddresses = new ConcurrentSkipListSet<Long>();
	    logger = LoggerFactory.getLogger(test1.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
	    floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	   public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
	        Ethernet eth =
	                IFloodlightProviderService.bcStore.get(cntx,
	                                            IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
	 
	        Long sourceMACHash = eth.getSourceMACAddress().getLong();
//	        if (!macAddresses.contains(sourceMACHash)) {
//	            macAddresses.add(sourceMACHash);
//	            logger.info("MAC Address: {} seen on switch: {}",
//	                    eth.getSourceMACAddress().toString(),
//	                    sw.getId().toString());
//	        }
	        
/*
			Match.Builder mb = sw.getOFFactory().buildMatch();
	        mb.setExact(MatchField.MPLS_LABEL, U32.of(12345));
	        
			//OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
			
			OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
			List<OFAction> actions = new ArrayList<OFAction>();
			//Match.Builder mb = match.createBuilder();

			// set input and output ports on the switch
			OFPort outPort = switchPortList.get(indx).getPortId();
			//OFPort inPort = switchPortList.get(indx - 1).getPortId();
			//mb.setExact(MatchField.IN_PORT, inPort);
			aob.setPort(outPort);
			aob.setMaxLen(Integer.MAX_VALUE);

			if(pi.getVersion().compareTo(OFVersion.OF_13) >= 0){
				actions.add(sw.getOFFactory().actions().pushMpls(EthType.MPLS_UNICAST));
				//actions.add(sw.getOFFactory().actions().setQueue(1));
				/*actions.add(factory.actions().setField(factory.oxms().mplsLabel(U32.of(0x1111))));
		        actions.add(factory.actions().setField(factory.oxms().mplsLabel(U32.ZERO)));
		        actions.add(factory.actions().setField(factory.oxms().mplsTc(U8.ZERO)));** /
			}
			//actions.add(aob.build());
			actions.add(factory.actions().output(outPort, Integer.MAX_VALUE));

			List<OFInstruction> arg0 = new ArrayList<OFInstruction>();
			arg0.add(factory.instructions().applyActions(actions));

			// compile
			fmb.setMatch(match) //mb.build()
			//.setActions(actions)
			.setInstructions(arg0)
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setCookie(cookie)
			.setOutPort(outPort);

			// Set buffer_id, in_port, actions_len
			pob.setBufferId(msg.getBufferId());
			pob.setInPort(msg.getVersion().compareTo(OFVersion.OF_12) < 0 ? packetInMessage
					.getInPort() : msg.getMatch().get(
					MatchField.IN_PORT));

			// set actions
			List<OFAction> actions = new ArrayList<OFAction>(1);
			actions.add(sw.getOFFactory().actions().buildOutput()
					.setPort(egressPort).setMaxLen(0xffFFffFF).build());
			pob.setActions(actions);

			// set data - only if buffer_id == -1
			if (packetInMessage.getBufferId() == OFBufferId.NO_BUFFER) {
				byte[] packetData = packetInMessage.getData();
				pob.setData(packetData);
			}

			// and write it out
			sw.write(pob.build());
	        
	        mb.setExact(MatchField.ETH_TYPE, EthType.IPv4)
			.setExact(MatchField.IPV4_SRC, srcIp)
			.setExact(MatchField.IPV4_DST, dstIp);*/
	        
	        return Command.CONTINUE;
	  
	    }

}

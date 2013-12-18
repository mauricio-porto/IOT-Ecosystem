package com.hp.myidea.obdproxy;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

public class OBDRemoteResourceFilter implements PacketFilter {

	public final static String RESOURCE = "OBDRemote";

    /**
     * Default constructor
     *
     */
    public OBDRemoteResourceFilter() {
    	super();
    }

	public boolean accept(Packet packet) {
        if (packet.getFrom() == null) {
            return false;
        }
    	String lixo = StringUtils.parseResource(packet.getFrom());
    	return lixo.indexOf(RESOURCE) != -1;
	}
}

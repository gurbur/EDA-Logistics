package com.jihwan.logistics.ims.config;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;

public class SolaceSessionFactory {
    public static JCSMPSession createSession() throws JCSMPException {
        JCSMPProperties props = new JCSMPProperties();
        props.setProperty(JCSMPProperties.HOST , System.getenv("SOLACE_HOST"));
        props.setProperty(JCSMPProperties.VPN_NAME , System.getenv("SOLACE_VPN"));
        props.setProperty(JCSMPProperties.USERNAME , System.getenv("SOLACE_USER"));
        props.setProperty(JCSMPProperties.PASSWORD , System.getenv("SOLACE_PASS"));
        return JCSMPFactory.onlyInstance().createSession(props);
    }
}

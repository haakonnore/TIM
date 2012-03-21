/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.tools;

import java.io.Serializable;
import java.util.Vector;

/**
 *
 * @author prajwalan
 */
public class MessengerConfiguration implements Serializable
{
	// make compiler happy :)
	private static final long serialVersionUID = 666L;

	//General
    public String name;
    public String id;
    public String sipProxyIP;
    public int sipProxyPort;
    public int myPort;
    
    //Presence
    public boolean enablePresence;
    public boolean showMyPresence;
    public boolean sendLocationWithPresence;
    public boolean receiveLocationWithPresence;
    public int locationAdInterval; // in minutes
    
    //Location
    public String locationURL;
    
    //Extra
    public Vector<String> statusList;
    public Vector<Contact> contacts;
    public Vector<String> dialList;
    
    
    
    public MessengerConfiguration()
    {
        statusList = new Vector<String>();
        contacts = new Vector<Contact>();
        dialList = new Vector<String>();
        
        name = "";
        id = "";
        sipProxyIP = "";
        sipProxyPort = 0;
        myPort = 0;
        
        enablePresence = true;
        showMyPresence = true;
        sendLocationWithPresence = false;
        receiveLocationWithPresence = false;
        locationAdInterval = 5;

        locationURL = "";
    }
    
    public void loadFactorySettings()
    {
        name = "java1";
        id = "java1";
        sipProxyIP = "129.241.209.181";
        sipProxyPort = 5060;
        myPort = 5566;
        
        enablePresence = true;
        showMyPresence = true;
        sendLocationWithPresence = false;
        receiveLocationWithPresence = false;      
        locationAdInterval = 5;
        
        locationURL = "http://geoposen.item.ntnu.no:8080/geofinder/ws/getloc";
    }
    
    public void set(MessengerConfiguration config)
    {
        this.statusList = config.statusList;
        this.contacts = config.contacts;
        this.dialList = config.dialList;
        
        this.name = config.name;
        this.id = config.id;
        this.sipProxyIP = config.sipProxyIP;
        this.sipProxyPort = config.sipProxyPort;
        this.myPort = config.myPort;
        
        this.enablePresence = config.enablePresence;
        this.showMyPresence = config.showMyPresence;
        this.sendLocationWithPresence = config.sendLocationWithPresence;
        this.receiveLocationWithPresence = config.receiveLocationWithPresence;
        this.locationAdInterval = config.locationAdInterval;
        
        this.locationURL = config.locationURL;
    }
    
    public String getSIPProxy()
    {
        return sipProxyIP+":" +sipProxyPort;
    }

    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.tools;

import java.io.Serializable;

/**
 *
 * @author prajwalan
 */
public class Contact implements Serializable
{
	// make compiler happy :)
	private static final long serialVersionUID = 666L;

	public String contactName;
    public String contactID;
    public String contactSIPProxyIP;
    public int contactSIPProxyPort;
    public String contactPresenceStatus;
    
    public boolean enablePresence;
    
    public Contact(String contactName, String contactID, 
            String contactSIPProxyIP, int contactSIPProxyPort, boolean enablePresence)
    {
        this.contactID = contactID;
        this.contactName = contactName;
        this.contactPresenceStatus = "";
        this.contactSIPProxyIP = contactSIPProxyIP;
        this.contactSIPProxyPort = contactSIPProxyPort;
        
        this.enablePresence = enablePresence;
    }
    
    @Override
    public String toString()
    {
        if( this.contactPresenceStatus.length() > 0 && this.enablePresence )
        {
            if( this.contactPresenceStatus.equals("Invisible"))
                return this.contactName + " - Offline";
            else
                return this.contactName + " - " + this.contactPresenceStatus;
        }
        else
            return this.contactName;
    }
}

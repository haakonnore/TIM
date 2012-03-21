/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package orca.tools;

/**
 *
 * @author prajwalan
 */
public class Configuration 
{
    public int sipPort;
    public String name;
    public String userID;
    
    public int subscribeExpire;
    
    public Configuration() 
    {
        sipPort=5060;
        name="";
        userID="";
        subscribeExpire = 300;
    }
    
}

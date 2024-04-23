package server;/*
 * Protocol.java
 *
 * Created on 01 √»—Ì·, 2008, 09:38 „
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Mohamed Talaat Saad
 */
public class Protocol {
    
    private String message="";
    /** Creates a new instance of Protocol */
    public Protocol() {
    }
    
    public String IDPacket(int id)
    {
        message="ID"+id;
        return message;
    }
    public String IDPacket(int id,String username)
    {
        message="ID"+id+","+username;
        return message;
    }
    public String NewClientPacket(int x,int y,int dir,int id)
    {
        message="NewClient"+x+","+y+"-"+dir+"|"+id;
        return message;   
    }
    public String NewClientPacket(String username,int x,int y,int dir,int id)
    {
        message="NewClient"+username+","+x+"-"+y+"|"+dir+"!"+id;
        return message;
    }
    public String LoginPacket(String status,int id, String username,int x,int y)
    {
        message="Login"+status+","+id+"-"+username+"|"+x+"+"+y;
        return message;
    }
    
}

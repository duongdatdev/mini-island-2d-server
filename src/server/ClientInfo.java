package server;

import java.io.DataOutputStream;

public class ClientInfo
{
    DataOutputStream writer;
    int posX,posY,direction;
    String username;

    public ClientInfo(DataOutputStream writer,String username,int posX,int posY,int direction)
    {
        this.writer=writer;
        this.username=username;
        this.posX=posX;
        this.posY=posY;
        this.direction=direction;
    }
    public ClientInfo(DataOutputStream writer,int posX,int posY,int direction)
    {
        this.writer=writer;
        this.posX=posX;
        this.posY=posY;
        this.direction=direction;
    }

    //getters and setters
    public String getUsername()
    {
        return username;
    }
    public void setPosX(int x)
    {
        posX=x;
    }
    public void setPosY(int y)
    {
        posY=y;
    }
    public void setDirection(int dir)
    {
        direction=dir;
    }
    public DataOutputStream getWriterStream()
    {
        return writer;
    }
    public int getX()
    {
        return posX;
    }
    public int getY()
    {
        return posY;
    }
    public int getDir()
    {
        return direction;
    }
}

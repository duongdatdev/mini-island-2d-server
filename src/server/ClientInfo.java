package server;

import org.java_websocket.WebSocket;

public class ClientInfo
{
    WebSocket webSocket;
    int posX,posY,direction;
    String username;
    String map;
    boolean isAlive=true;

    public ClientInfo(WebSocket webSocket,String username,int posX,int posY,int direction,String map)
    {
        this.webSocket=webSocket;
        this.username=username;
        this.posX=posX;
        this.posY=posY;
        this.direction=direction;
        this.map=map;
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
    public WebSocket getWebSocket()
    {
        return webSocket;
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

    public String getMap() {
        return map;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setMap(String map) {
        this.map = map;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.bluetooth.RemoteDevice;
import lejos.nxt.LCD;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;

/**
 *
 * @author Unnati
 */
public class CommChannel extends Thread {

    private DataInputStream dis = null; 
    private DataOutputStream dos = null;    
    private String nodeName;
    private DataLogger dataLogger = new DataLogger();    
    protected BTConnection nodeConnection = null; 
    protected boolean threadRunning = false;    
    protected boolean connected = false;
    protected RemoteDevice node;
    
    
    
    public CommChannel(){        
        
    }
    
    public CommChannel(String n){
        super(n);        
        nodeName = n;        
    }
    
    public boolean isRunning(){
        return threadRunning;
    }
    
    public String getNodeName(){
        return nodeName;
    }
    
    public void startDataLog(String fileName){
        dataLogger.initialize(fileName, nodeName);        
    }
    
    public void stopDataLog(){
        dataLogger.close();      
    }
    
    protected int discoverDevices(){
        LCD.clear();
        LCD.drawString("Searching...",0,0);
        int cod = 0000;
        ArrayList <RemoteDevice> devList = Bluetooth.inquire(5,10,cod);
        if (devList == null){   //if none was found inform the user
            LCD.drawString("inquire returns null",0,1);
            LCD.drawString("Exiting!",0,2);            
            return 0;
        //if some were found, add them in the known device list
        }else if (devList.size()>0){       
            String[] names = new String[devList.size()];
            for (int j=0;j<devList.size();j++){
                RemoteDevice btrd = devList.get(j);
                names[j] = btrd.getFriendlyName(false);              
                Bluetooth.addDevice(btrd);
                LCD.drawString("added "+ names[j] + "\n",0,j+1);                   
            } 
            delay(500);
        }
        return 1;    
    }
    
    public int connect(){
        //check if BT is on. If not switch it on
        if (!Bluetooth.getPower()){          
            Bluetooth.setPower(true);
        }
        while(!Bluetooth.getPower()) {} //Wait until power is on
        if(Bluetooth.getVisibility() < 1) {// 1 if visible, 0 if invisible
            Bluetooth.setVisibility((byte)1); //Set visible
        }
        return 1;
    }
    
    public int connect(String name){        
        return (connect());
    }

    private void logWriteTime(){
        dataLogger.logWriteTime();
    }
    
    private void logReadTime(){
        dataLogger.logReadTime();
    }
    
    public int writeLongData(long n, boolean log){        
        try {
            if (log){
                logWriteTime();
            }
            dos.writeLong(n);
            dos.flush();
            return 1;
        } catch (IOException ioe) {
            LCD.drawString("Write Exception", 0, 0);
            delay(50);
            LCD.refresh();
            return 0;
        }
    }
    
    public int writeBoolData(boolean d, boolean log){        
        try {
            if (log){
                logWriteTime();
            }
            dos.writeBoolean(d);
            dos.flush();
            return 1;
        } catch (IOException ioe) {
            LCD.drawString("Write Exception", 0, 0);
            delay(50);
            LCD.refresh();
            return 0;
        }
    }
    
    public void saveData(){
        LCD.clear();
        LCD.drawString("Saving", 0, 0);
        try {
            dataLogger.saveData();
            LCD.clear();
            LCD.drawString("Success", 0, 1);
        } catch (IOException ex) {
            LCD.clear();
            LCD.drawString("Error!!", 0, 2);
            
        }
    }
    
    public long readLongData(boolean log){
        long temp;
        try {                    
            temp = dis.readLong();
            if (log){
                logReadTime();
            }
        } catch (IOException ioe) {
            LCD.drawString("Read Exception ", 0, 0);
            delay(50);            
            LCD.refresh();
            temp = -1;
        }    
        return temp;
    }
    
    public boolean readBoolData(boolean log){
        boolean temp;
        try {                    
            temp = dis.readBoolean();
            if (log){
                logReadTime();
            }
        } catch (IOException ioe) {
            LCD.drawString("Read Exception ", 0, 0);
            delay(50);            
            LCD.refresh();
            temp = false;
        }    
        return temp;
    }
    
    
    public void delay(int delayTime){
        try {
            Thread.sleep(delayTime);
        } catch (InterruptedException ex) {
            //error    
        }
    }
    
    public void disconnect(){
        nodeConnection.close();
        delay(100);
    }
    
    public void closeIOStreams() throws IOException{
        dis.close();
        dos.close();
        delay(100);
    }
    
    public void openIOStreams(){        
        dis = nodeConnection.openDataInputStream();
        dos = nodeConnection.openDataOutputStream();
    }
     
    
    public void run() {
        LCD.clear();
        LCD.drawString("In Channel Run", 0, 2);
        delay(1000);
    }
        
        
}


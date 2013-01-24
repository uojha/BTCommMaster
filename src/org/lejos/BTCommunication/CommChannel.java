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
    protected int id;
    private DataLogger dataLogger = new DataLogger();    
    protected BTConnection nodeConnection; 
    protected boolean threadRunning = false;    
    protected boolean connected = false;
    protected RemoteDevice node;
    protected int neighborID = 0;
    
    
    
    public CommChannel(){        
        
    }    
    public CommChannel(String n, int ch_id, int n_id){
        super(n);        
        nodeName = n;   
        id = ch_id;
        neighborID = n_id;
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
    
    public void setNeighborID(int n_id){
        neighborID = n_id;      
    }
    
    protected int discoverDevices(){
        if (Node.DEBUG) {
            Node.debugMessage("Searching...");
        }
        int cod = 0000;
        ArrayList <RemoteDevice> devList = Bluetooth.inquire(5,10,cod);
        if (devList == null){   //if none was found inform the user
            if (Node.DEBUG) {
                Node.debugMessage("Inquiry returns Null",0,0,0);
                Node.debugMessage("Exiting",0,1);
            }          
            return 0;
        //if some were found, add them in the known device list
        }else if (devList.size()>0){       
            String[] names = new String[devList.size()];
            for (int j=0;j<devList.size();j++){
                RemoteDevice btrd = devList.get(j);
                names[j] = btrd.getFriendlyName(false);              
                Bluetooth.addDevice(btrd);
                if (Node.DEBUG) {
                    Node.debugMessage("added "+ names[j] + "\n",0,j+1);
                }                
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
        dataLogger.logWriteTime(System.currentTimeMillis() - Node.clock.getDriftRoot());
    }    
    private void logReadTime(){
        dataLogger.logReadTime(System.currentTimeMillis() - Node.clock.getDriftRoot());
    }    
    public void logData(long n){
        dataLogger.logLongData(n);
    }  
    
    public void logState(double n){
        dataLogger.logState(n);
    }
    
    public void logNeighbor(double n){
        dataLogger.logNeighbor(n);
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
            if (Node.DEBUG) {
                Node.debugMessage("Write Exception");
            }
            return 0;
        }
    }    
    public int writeFloatData(Float n, boolean log){        
        try {
            if (log){
                logWriteTime();
            }
            dos.writeFloat(n);
            dos.flush();
            return 1;
        } catch (IOException ioe) {
            if (Node.DEBUG) {
                Node.debugMessage("Write Exception");
            }
            return 0;
        }
    }
    
    public int writeDoubleData(double n, boolean log){        
        try {
            if (log){
                logWriteTime();
            }
            dos.writeDouble(n);
            dos.flush();
            return 1;
        } catch (IOException ioe) {
            if (Node.DEBUG) {
                Node.debugMessage("Write Exception");
            }
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
            if (Node.DEBUG) {
                Node.debugMessage("Write Exception");
            }
            return 0;
        }
    }    
    public void saveData(){
        if (Node.DEBUG) {
            Node.debugMessage("Saving",100);
        }
        try {
            dataLogger.saveData();
            if (Node.DEBUG) {
                Node.debugMessage("Success");
            }
        } catch (IOException ex) {
            if (Node.DEBUG) {
                Node.debugMessage("Error in Saving");
            }
            
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
            if (Node.DEBUG) {
                Node.debugMessage("Read Exception");
            }
            temp = -1;
        }    
        return temp;
    }    
    public float readFloatData(boolean log){
        float temp;
        try {                    
            temp = dis.readFloat();
            if (log){
                logReadTime();
            }
        } catch (IOException ioe) {
            if (Node.DEBUG) {
                Node.debugMessage("Read Exception");
            }
            temp = -1.0f;
        }    
        return temp;
    }
    public double readDoubleData(boolean log){
        double temp;
        try {                    
            temp = dis.readDouble();
            if (log){
                logReadTime();
            }
        } catch (IOException ioe) {
            if (Node.DEBUG) {
                Node.debugMessage("Read Exception");
            }
            temp = -1.0f;
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
            if (Node.DEBUG) {
                Node.debugMessage("Read Exception");
            }
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
        if (Node.DEBUG) {
            Node.debugMessage("In Channel Run");
        }
    }   
}


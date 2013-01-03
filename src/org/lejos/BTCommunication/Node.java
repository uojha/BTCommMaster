package org.lejos.BTCommunication;


import java.io.IOException;
import lejos.nxt.*;
import lejos.nxt.comm.Bluetooth;

/**
 * LeJOS class to handle BT Communication related functions
 * such as search, add to device list, connect, communicate and disconnect
 *
 */
public class Node {

    final static int NUMCHANNELS = 4;
    final static int SLAVECHANNEL = 0;
    final static int MASTERCHANNEL1 = 1;
    final static int MASTERCHANNEL2 = 2;
    final static int MASTERCHANNEL3 = 3;
    
    final static long SYNCREQ = -99;
    final static long SYNCPARAMS = -100;
    final static boolean SYNCSTATUS_SYNCED = true;
    final static boolean SYNCSTATUS_NOTSYNCED = false;
    final static int SYNCWINDOW = 20;
            
    final static int COMMCOUNT = 50; 
    public static boolean isRootNode = false;
    final static boolean SYNCHRONIZE = true;
    static Clock clock;
    
    public static CommChannel setAsMaster(String slave, int priority){
        CommChannel m = new CommMaster(slave);
        m.connect(slave);
        m.openIOStreams();
        String fileName;
        fileName = new String(slave + ".txt");
        m.startDataLog(fileName);
        m.setPriority(priority); 
        return m;
    }
    public static CommChannel setAsSlave(String slave, int priority){
        CommChannel s = new CommSlave(slave);
        s.connect();
        s.openIOStreams();
        String fileName;
        fileName = new String(slave + ".txt");
        s.startDataLog(fileName);
        s.setPriority(priority); 
        return s;
    }
    
    public static void saveAndClose(CommChannel c) throws IOException{
        c.saveData();
        c.stopDataLog();
        c.closeIOStreams();
        c.disconnect();
    }
    
    public static void setRoot(){
        isRootNode = true;   
        clock.setSyncStatus(true);
    }
    
    public static boolean isRoot(){
        return(isRootNode);
    }
    
    public static boolean isCommunicating(CommChannel[] channels){
        
        boolean communicating = false;
        
        for (int i=0;i<NUMCHANNELS;i++){
            if (channels[i] != null){
                if (channels[i].isRunning()){
                    communicating = true;
                }
            }
        }
        /*
        if (ch2 != null){
            if (ch2.isRunning()){
                communicating = true;
            }            
        }
        if (ch3 != null){
            if (ch3.isRunning()){
                communicating = true;
            }           
        }
        if (ch4 != null){
            if (ch4.isRunning()){
                communicating = true;
            }           
        }*/
        
        return communicating;
    }
    
    
    public static void main(String[] args) throws InterruptedException {         
                
        CommChannel [] commChannels = null;
        boolean commStatus; 
        
        //construct the communication topology
        String myName = Bluetooth.getName();        
        if (myName.equalsIgnoreCase("Node0")){
            //do nothing
        }else if (myName.equalsIgnoreCase("Node1")){
            setRoot();
            commChannels[MASTERCHANNEL1] = setAsMaster("Node2", 2);            
        }else if (myName.equalsIgnoreCase("Node2")){
            commChannels[SLAVECHANNEL] = setAsSlave("Node1", 7);
            commChannels[MASTERCHANNEL1] = setAsMaster("Node3", 2);            
        }else if (myName.equalsIgnoreCase("Node3")){
            commChannels[SLAVECHANNEL] = setAsSlave("Node2", 7);            
        }else{
            //do Nothing
        }
         
         
         //before starting the threads, we will perform synchronization
         if (SYNCHRONIZE){
             boolean temp = false;
            //wait for synchronization request from its master
            if (clock.getSyncStatus()== SYNCSTATUS_NOTSYNCED){
                clock.waitForSync(commChannels[SLAVECHANNEL]);
            }
            //once the node is synchronized with root's clock
            //perform synchronization on its slave nodes.
            for (int i=0;i<NUMCHANNELS; i++){                
                if (commChannels[i]!=null){ 
                    temp = clock.initiateSync(commChannels[i],i);
                }
            }
        }
 
        for (int i=0;i<NUMCHANNELS;i++){
            if (commChannels[i]!=null){ 
                commChannels[i].start();           
                LCD.drawString("Ch" + i + " started",0,3);
                Thread.sleep(500);         
            }
        }

        
        
        /*
        if (chMaster1!=null){ 
            chMaster1.start();
            LCD.drawString("Ch2 started",0,4);
            Thread.sleep(500);
        }

        if (chMaster2!=null){ 
            chMaster2.start();
            LCD.drawString("Ch3 started",0,5);
            Thread.sleep(500);
        }

        if (chMaster3!=null){ 
            chMaster3.start();
            LCD.drawString("Ch4 started",0,6);
            Thread.sleep(500);
        }
        */

        Thread.sleep(1000);
        LCD.clear();
        LCD.drawString("Communicating",0,0);
        
        commStatus = isCommunicating(commChannels);
        
        while(commStatus){
            commStatus = isCommunicating(commChannels);
            Thread.sleep(100);
        }
        
        //save data and close connections
        
        for (int i=0;i<NUMCHANNELS;i++){
            if (commChannels[i]!=null){
                try{
                    saveAndClose(commChannels[i]);
                }catch(IOException ioe){
                }
            }
        }
        
        
        
        LCD.clear();
        LCD.drawString("Exiting ",0,0);
        Thread.sleep(1000);     
    }
}

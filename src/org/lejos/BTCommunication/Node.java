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
    
    final static boolean DEBUG = true;

    final static int NUMCHANNELS = 4;
    final static int SLAVECHANNEL = 0;
    final static int MASTERCHANNEL1 = 1;
    final static int MASTERCHANNEL2 = 2;
    final static int MASTERCHANNEL3 = 3;
    
    final static long SYNCREQ = -99;
    final static long SYNCPARAMS = -100;
    final static long SUBNET_SYNCED_CODE = -101;
    final static boolean SYNCSTATUS_SYNCED = true;
    final static boolean SYNCSTATUS_NOTSYNCED = false;
    final static int SYNCWINDOW = 20;
            
    final static int COMMCOUNT = 15; 
    public static boolean isRootNode = false;
    final static boolean SYNCHRONIZE = true;    
    static Clock clock = new Clock();
    
    public static CommChannel setAsMaster(String slave, int priority){
        CommChannel m = new CommMaster(slave);
        m.connect(slave);
        m.openIOStreams();
        String fileName;
        fileName = slave + ".txt";
        m.startDataLog(fileName);
        m.setPriority(priority); 
        return m;
    }
    public static CommChannel setAsSlave(String slave, int priority){
        CommChannel s = new CommSlave(slave);
        s.connect();
        s.openIOStreams();
        String fileName;
        fileName = slave + ".txt";
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
        return communicating;
    }
    
    public static void debugMessage(String s, int col, int row, int delay){        
        LCD.drawString(s,col,row);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {            
        }
    }    
    public static void debugMessage(String s, int delay){
        LCD.clear();
        debugMessage(s,0,0,delay);
    }    
    public static void debugMessage(String s, int col, int row){
        debugMessage(s,col,row,500);
    }
    public static void debugMessage(String s){
        LCD.clear();
        debugMessage(s,0,0,500);
    }
    
    public static void delay(int delayTime){
        try {
            Thread.sleep(delayTime);
        } catch (InterruptedException ex) {
            //error    
        }
    }
    
    public static void main(String[] args) throws InterruptedException {         
        
        CommChannel [] commChannels = {null, null,null,null};//new CommChannel[NUMCHANNELS];        
        boolean commStatus; 
        boolean sensorNetSynced = false;
        
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
            if (clock.getSyncStatus() == SYNCSTATUS_NOTSYNCED){
                clock.waitForSync(commChannels[SLAVECHANNEL]);
            }
            //once the node is synchronized with root's clock
            //perform synchronization on its slave nodes.
            for (int i=MASTERCHANNEL1;i<NUMCHANNELS; i++){                
                if (commChannels[i]!=null){ 
                    temp = clock.initiateSync(commChannels[i],i);
                }
            }
            
            //wait for this node's subnet to sync
            long  tempSyncCode;
            while (!sensorNetSynced){
                sensorNetSynced = true;
                debugMessage("Syncing...");
                for (int i=MASTERCHANNEL1;i<NUMCHANNELS; i++){
                    if (commChannels[i]!=null){
                        tempSyncCode = commChannels[MASTERCHANNEL1].readLongData(Clock.LOGSYNCDATA);  
                        if (tempSyncCode != SUBNET_SYNCED_CODE){
                            sensorNetSynced = false;
                        }
                    }
                    delay(100);
                }                
            }
            debugMessage("Synced",0,1);
            debugMessage(Long.toString(clock.getDriftRoot()),4000);
            
            //send data to its master to complete synchronization
            if (!isRootNode){
                commChannels[SLAVECHANNEL].writeLongData(SUBNET_SYNCED_CODE, Clock.LOGSYNCDATA);
            }
        }
        
        for (int i=0;i<NUMCHANNELS;i++){
            if (commChannels[i] != null){               
                commChannels[i].start();
                debugMessage("Ch" + i + " started",0,3,1000); 
            }
        }
        
        debugMessage("Communicating",1000);         
        commStatus = isCommunicating(commChannels);
        
        while(commStatus){
            commStatus = isCommunicating(commChannels);
            delay(100);            
        }
        
        debugMessage("Saving");
        //save data and close connections        
        for (int i=0;i<NUMCHANNELS;i++){
            if (commChannels[i] != null){
                try{
                    saveAndClose(commChannels[i]);
                }catch(IOException ioe){
                    debugMessage("Save Error",0,1,1000);
                }
            }
        }       
        debugMessage("Exiting");     
    }
}

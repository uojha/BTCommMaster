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
    final static int NUMNODES = 5;
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
    final static boolean DEBUG = true;    
    final static boolean SYNCHRONIZE = true;         
    final static int COMMCOUNT = 150; 
    final static double[] INITNODEVALS = {9,9,9,9,9};
    final static int DELTA_T = 250; 
    
    
    public static boolean[] activeNeighbor = {false, false, false, false};
    public static boolean isRootNode = false;
    public static boolean initStatus = false;
    public static long STARTTIME;  
    
    static Clock clock = new Clock();
    static DistAlgorithm distAlgo = new DistAlgorithm();
    static int nodeID = 0;

    
    public static void main(String[] args) throws InterruptedException {         
        
        CommChannel [] commChannels = {null, null,null,null};
        boolean commStatus; 
        boolean sensorNetSynced = false;
        
        //construct the communication topology
        String myName = Bluetooth.getName();    
        if (myName.equalsIgnoreCase("Node0")){
            setRoot();
            nodeID = 0;
            distAlgo.setNodeVal(INITNODEVALS[0]);
            commChannels[MASTERCHANNEL1] = setAsMaster("Node1", MASTERCHANNEL1, 1, 2); 
            commChannels[MASTERCHANNEL2] = setAsMaster("Node2", MASTERCHANNEL2, 2, 2);             
        }else if (myName.equalsIgnoreCase("Node1")){  
            nodeID = 1;
            distAlgo.setNodeVal(INITNODEVALS[1]);
            commChannels[SLAVECHANNEL] = setAsSlave("Node0", 0, 2);
            //commChannels[MASTERCHANNEL1] = setAsMaster("Node2", 2, MASTERCHANNEL1);
        }else if (myName.equalsIgnoreCase("Node2")){
            nodeID = 2;
            distAlgo.setNodeVal(INITNODEVALS[2]);
            commChannels[SLAVECHANNEL] = setAsSlave("Node0", 0, 2);   
            commChannels[MASTERCHANNEL1] = setAsMaster("Node3", MASTERCHANNEL1,3,2); 
            commChannels[MASTERCHANNEL2] = setAsMaster("Node4", MASTERCHANNEL2,4,2);
        }else if (myName.equalsIgnoreCase("Node3")){
            nodeID = 3;
            distAlgo.setNodeVal(INITNODEVALS[3]);
            commChannels[SLAVECHANNEL] = setAsSlave("Node2", 2,2);   
            //commChannels[MASTERCHANNEL1] = setAsMaster("Node1", 2, MASTERCHANNEL1); 
        }else if (myName.equalsIgnoreCase("Node4")){
            nodeID = 4;
            distAlgo.setNodeVal(INITNODEVALS[4]);
            commChannels[SLAVECHANNEL] = setAsSlave("Node2", 2,2);   
            //commChannels[MASTERCHANNEL1] = setAsMaster("Node1", 2, MASTERCHANNEL1); 
        }else{
            //do Nothing
        }
        
        if (isRootNode){
            STARTTIME = System.currentTimeMillis() + 100000;
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
                        tempSyncCode = commChannels[i].readLongData(Clock.LOGSYNCDATA);  
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
            if (commChannels[SLAVECHANNEL]!=null){
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
        
        //perform average consensus
        distAlgo.LFICC();
        
        //wait for all connections to close
        while(commStatus){
            commStatus = isCommunicating(commChannels);            
            delay(5);            
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
        
    public static CommChannel setAsMaster(String slave, int ch_id, int n_id, int priority){
        CommChannel m = new CommMaster(slave, ch_id);
        activeNeighbor[ch_id] = true;
        m.setNeighborID(n_id);
        m.connect(slave);
        debugMessage("Connected!!",2000);
        m.openIOStreams();
        debugMessage("IOS created");
        String fileName;
        fileName = slave + ".txt";
        m.startDataLog(fileName);
        debugMessage("file created");
        m.setPriority(priority); 
        debugMessage("priority set");
        return m;
    }
    public static CommChannel setAsSlave(String slave, int n_id, int priority){
        CommChannel s = new CommSlave(slave);
        activeNeighbor[0] = true;
        s.setNeighborID(n_id);
        s.connect();
        debugMessage("Connected!!");
        s.openIOStreams();
        debugMessage("IOS created");
        String fileName;
        fileName = slave + ".txt";
        s.startDataLog(fileName);
        debugMessage("file created");
        s.setPriority(priority);
        debugMessage("priority set");
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
        initStatus = true;
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
    
     public static boolean isInitialized(){
        return initStatus;
    }
}

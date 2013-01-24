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
    final static int MAXMASTERCHANNELS = 3;
    final static int MAXSLAVECHANNELS = 1;
    final static int SLAVECHANNEL = 0;
    final static int MASTERCHANNEL1 = 1;
    final static int MASTERCHANNEL2 = 2;
    final static int MASTERCHANNEL3 = 3;   
    final static long SYNCREQ = -99;
    final static long SYNCPARAMS = -100;
    final static long SUBNET_SYNCED_CODE = -101;
    final static boolean SYNCSTATUS_SYNCED = true;
    final static boolean SYNCSTATUS_NOTSYNCED = false;    
    final static boolean OK = true;
    final static boolean NOT_OK = false;  
    
    final static int SYNCWINDOW = 20;
    final static int SYNCWAIT = 100000;
    final static boolean DEBUG = true;    
    final static boolean SYNCHRONIZE = true;         
    final static int COMMCOUNT = 150;     
    final static int DELTA_T = 250;     
        
    public static boolean[] activeNeighbor = {false, false, false, false};
    public static boolean isRootNode = false;
    public static int nodeID = 0;
    public static long STARTTIME;  
    public static int numNodes;    
    
    public static CommChannel [] commChannels = {null, null,null,null};
    public static Clock clock = new Clock();
    public static DistAlgorithm distAlgo = new DistAlgorithm();
    public static int rootNode = 0;                           //specify the root node's id

    public static void main(String[] args) throws InterruptedException {               
        //Use topology matrix to set the network topology. 
        //Use 1 if the node is the master and -1 if it is the slave
        double [][] adjacencyMatrix =   {   { 0, 1, 0, 0, -1},
                                            {-1, 0, 1, 0, 0},
                                            {0, -1, 0, 1, 0},
                                            {0, 0, -1, 0, 1},
                                            {1, 0, 0, -1, 0}
                                        };        
        
        double [] initNodeValues = {9,9,9,9,9};         //specify the initial value of the nodes        
        double epsilon = 0.25;                      //specify the value of epsilon     
        double epsilon2 = 0.001;                    //set epsilon value for deltaP in ICC
        //using same values as paper
        //double [] alpha = {561, 310,78,561,78};
        double [] beta = {7.92, 7.85,7.8,7.92,7.8};                     //set beta value for ICC
        double [] gamma = {0.001562,0.00194,0.00482,0.001562,0.00482};  //set gamma value for ICC
        
        //check if the network topology contains rings or multiple slave connections
        if (checkTopology(adjacencyMatrix, rootNode)){        
            
            //initialize some of the matrices that will be used in 
            // the distributed Algorithm class
            distAlgo.initialize(numNodes);
            
            // create a perron matrix based on the adjacency matrix 
            // and the epsilon value
            distAlgo.createWeightMatrix(adjacencyMatrix, epsilon);
            
            //initialize the nodes
            initNodes(adjacencyMatrix, rootNode, initNodeValues);            
            
            //synchronize the network
            
            //boolean networkStatus = NOT_OK;
            debugMessage("BEFORE SYNC",1000);
            if (isRootNode){
                while(!commChannels[0].connected){
                    delay(200);
                }
            }
            
            debugMessage("START SYNC",3000); 
            clock.synchronizeNetwork();                 
            
            //start a thread for each active channel
            for (int i=0;i<NUMCHANNELS;i++){
                if (activeNeighbor[i]){
                    if (i == 0 && isRootNode){
                        delay(50);
                    }else{
                        commChannels[i].start();
                        delay(50);
                    }
                    debugMessage("Ch" + i + " started",0,3,1000); 
                }
            }        
            //perform average consensus
            distAlgo.LFICC(epsilon2, beta, gamma);        
            
            boolean commStatus = true;       
            //wait for all connections to stop communicating
            while(commStatus){
                commStatus = isCommunicating();            
                delay(5);            
            }        
            //save and close
            saveAndClose();
        }
        debugMessage("Exiting");     
    }
    
    
    /**
     * checkTopology checks for the properties of the network adjacency matrix
     * nt for the following: 1. if it is a square matrix, if it has correct
     * number of masters (<=3) and slaves (<=1), if root node is specified 
     * properly, and if the matrix is symmetric. returns true if all reqmts are
     * met and false otherwise
     */
    public static boolean checkTopology(double [][] nt, int r_id){        
          
        if (nt.length != nt[0].length){
            debugMessage("Top Matrix is not Square",1000);
            return false;
        }      
        numNodes = nt.length;      
        debugMessage("Network Size: "+numNodes,1000);
        if (r_id > numNodes){
            debugMessage("Root node does not exist",1000);
            return false;
        }       
        /*for (int i=0;i<numNodes;i++){
            if (nt[r_id][i] == -1){
                debugMessage("Root cannot be a slave",1000);
                return false;              
            }            
        }  
        * */
        for (int i=0;i<numNodes;i++){
            int numMasterCh = 0;
            int numSlaveCh = 0;
            for (int j=0;j<numNodes;j++){
                if ((nt[i][j] + nt[j][i]) != 0){
                    debugMessage("Master/Slave Channel mismatch",1000);
                    return false;              
                }
                if (nt[i][j] > 0){
                    numMasterCh += 1;             
                }
                if (nt[i][j] < 0){
                    numSlaveCh += 1;             
                }
            }
            if (numMasterCh > MAXMASTERCHANNELS){
                debugMessage("Too many master Channels",1000);
                return false;              
            }
            if (numSlaveCh > MAXSLAVECHANNELS){
                debugMessage("Too many slave Channels",1000);
                return false;              
            }
        }        
        return (true);        
    }
    
    /**
     * initNodes initializes the nodes based on the network topology set
     * gets the name of the node and creates master or slave connections
     * according to the topology. Note that the names of each Node should follow
     * the format "Node"+ID. ID should not be greater than the total number of 
     * nodes
     */
    public static void initNodes(double [][] nt, int r_id, double [] initNodeVals){
        //construct the communication topology
        String myName = Bluetooth.getName();  
        nodeID = Integer.valueOf(myName.substring(4));
        debugMessage("Node ID: "+ Integer.toString(nodeID),1000);
        if (nodeID > (numNodes - 1)){            
            debugMessage("Node ID should be ", 0,1,10);
            debugMessage("less than " + numNodes, 0,2,1000);
        }
        //initialize the node values
        distAlgo.setNodeVal(initNodeVals[nodeID]);       
        
        int currChannel  = 0;        
        //first check if this node is a slave node and create the slave connection
        for (int j=0;j<numNodes;j++){
            if (nt[nodeID][j] == -1.0){                
                StringBuilder temp = new StringBuilder();
                temp.append("Node");
                temp.append(j);
                String nodeName = temp.toString();  
                if (nodeID == r_id){
                    //NEEDS DOCUMENTATION
                    setRoot();
                    commChannels[currChannel] = new CommSlave(nodeName, j);
                    commChannels[currChannel].start();
                }else{
                    commChannels[currChannel] = setAsSlave(nodeName, j, 2);
                }
            }
        }     
        delay(3000);
        //then create the master channels
        currChannel +=1;
        for (int j=0;j<numNodes;j++){
            if (nt[nodeID][j] == 1){                
                StringBuilder temp = new StringBuilder();
                temp.append("Node");
                temp.append(j);
                String nodeName = temp.toString();
                commChannels[currChannel] =  setAsMaster(nodeName, currChannel, j, 2);
                currChannel += 1;
            }
        }
        //if this node is the root set it as the root
        if (nodeID == r_id){            
            //set time to start communication
            STARTTIME = System.currentTimeMillis() + SYNCWAIT;
        } 
    }
    
    /**
     * setAsMaster creates a channel where this node is the master node to the
     * slave node identified by the string slave.
     * Other functions of this method are also to
     * set this channel as active, openIOStreams to communicate data thrrough
     * this channel, create log file to write and save data and to set the 
     * priority of this channel's thread
     */
    
    public static CommChannel setAsMaster(String slave, int ch_id, int n_id, int priority){
        CommChannel m = new CommMaster(slave, ch_id, n_id);
        activeNeighbor[ch_id] = true;
        m.setNeighborID(n_id);
        m.connect(slave);        
        m.openIOStreams();
        String fileName;
        fileName = slave + ".txt";
        m.startDataLog(fileName);
        m.setPriority(priority); 
        debugMessage("Connected!!",2000);
        return m;
    }
    
    /*
     * setAsSlave creates a channel where this node is the slave node
     * Other functions of this method are also to
     * set this channel as active, openIOStreams to communicate data thrrough
     * this channel, create log file to write and save data and to set the 
     * priority of this channel's thread
     */
    public static CommChannel setAsSlave(String slave, int n_id, int priority){
        CommChannel s = new CommSlave(slave, n_id);
        activeNeighbor[0] = true;
        s.setNeighborID(n_id);
        s.connect();
        s.openIOStreams();
        String fileName;
        fileName = slave + ".txt";
        s.startDataLog(fileName);
        s.setPriority(priority);
        debugMessage("Connected!!",2000);
        return s;
    }
    
    /*
     * saveAndClose saves the Data, stops the data logger, closes IOStreams 
     * and disconnects the channel 
     */
    public static void saveAndClose(){
        
        debugMessage("Saving");
        //save data and close connections        
        for (int i=0;i<NUMCHANNELS;i++){
            if (activeNeighbor[i]){
                try{
                    commChannels[i].saveData();
                    commChannels[i].stopDataLog();
                    commChannels[i].closeIOStreams();
                    commChannels[i].disconnect();                    
                }catch(IOException ioe){
                    debugMessage("Save Error",0,1,1000);
                }
            }
        }
        
    }  
    
    /*
     * setRoot() sets this node as the root node. All the clocks are synchronized
     * to the root node's clock.
     */
    public static void setRoot(){
        isRootNode = true;   
        clock.setSyncStatus(true);
    } 
    
    public static boolean isRoot(){
        return(isRootNode);
    }
    
    /* isCommunicating() checks if the thread for each channel is still alive
     * the threadRunning variable in commchannels is cleared once the threads 
     * stop. This method checks only the active channels (whose value is not 
     * null.
     * */
    public static boolean isCommunicating(){
        
        boolean communicating = false;        
        for (int i=0;i<NUMCHANNELS;i++){
            if (activeNeighbor[i]){
                if (commChannels[i].isRunning()){
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
}
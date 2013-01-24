package org.lejos.BTCommunication;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

/**
 *
 * @author Unnati
 */
public class CommSlave extends CommChannel{
    
    public CommSlave(){        
        
    }
    
    public CommSlave(String n, int n_id){
        super(n,0, n_id);              
    }
    
    @Override
    public int connect(){
        super.connect();  
        int waitTime = 1;
        nodeConnection = null;
        boolean allMasterConnectionDone = false;
        while (nodeConnection == null){    
            Node.debugMessage("Waiting..." + waitTime);
            if (Node.isRootNode){
                allMasterConnectionDone = true;
                for (int ch = Node.MASTERCHANNEL1; ch < Node.MASTERCHANNEL3; ch++){
                    if (Node.activeNeighbor[ch]){
                        if (!Node.commChannels[ch].connected){
                            allMasterConnectionDone = false;
                        }
                    }
                }
                if (allMasterConnectionDone){
                    nodeConnection = Bluetooth.waitForConnection();
                }else{
                    nodeConnection = Bluetooth.waitForConnection(7000, NXTConnection.PACKET);
                }
            }else{
                nodeConnection = Bluetooth.waitForConnection();
            }            
            delay(100);       
            waitTime += 1;
        }   
        connected = true;
        Node.debugMessage("Connected...",0,1);
        return 1;
    }
    
    @Override
    public void run() {        
        long n, deltaT, startTime;
        threadRunning = true;
        double data_in;  
        connected = false;
        if (Node.isRootNode){
            Node.activeNeighbor[id] = true;
            connect();        
            openIOStreams();
            String fileName;
            fileName = getNodeName() + ".txt";
            startDataLog(fileName);
            setPriority(4); 
            Node.debugMessage("Im a Slave!!",1000);
        }
        
        while (Node.clock.getSyncStatus() !=  Node.SYNCSTATUS_SYNCED){
            delay(50);
        }
        
        deltaT = Node.DELTA_T;
        startTime = Node.STARTTIME;
        
        for(int i=0;i<Node.COMMCOUNT;i++){           
            
            //wait for exact time to send the data
            while (System.currentTimeMillis() - Node.clock.getDriftRoot() < startTime){
                delay(5);
            }
            //send the previous values
            writeDoubleData(Node.distAlgo.getNodeVal(),true);  
            logState(Node.distAlgo.getNodeVal()); 
            
            //receive values from neighbor and store them in the neighbor matrix
            data_in = readDoubleData(false);
//            while (data_in == -1.0f){
//                data_in = readFloatData(false);
//                delay(5);
//            }
//            logNeighbor(data_in);
            Node.distAlgo.setNeighborVal(neighborID,data_in);
            
            //update dataStep
            Node.distAlgo.setTimeStep(Node.distAlgo.getTimeStep(id)+1, id);
            
            startTime = startTime+deltaT;
            
            //wait for exact time to send the data
            while (Node.distAlgo.getTimeStep(id) == (Node.distAlgo.getCurrentStep())){
                delay(5);
            }
            if (Node.isRootNode){
                logNeighbor(Node.distAlgo.getPG());
            }
            Node.debugMessage(Integer.toString(Node.distAlgo.getCurrentStep()),0,id+1,20);
        }
        Node.debugMessage("Slave Done",1000);
        threadRunning = false;        
    }
} 

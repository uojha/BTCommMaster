package org.lejos.BTCommunication;
import lejos.nxt.comm.Bluetooth;

/**
 *
 * @author Unnati
 */
public class CommSlave extends CommChannel{
    
    public CommSlave(){        
        
    }
    
    public CommSlave(String n){
        super(n,0);              
    }
    
    @Override
    public int connect(){
        super.connect();        
        while (!connected){    
            Node.debugMessage("Waiting...");
            nodeConnection = Bluetooth.waitForConnection();
            delay(10);       
            connected = true;
        }         
        Node.debugMessage("Connected...",0,1);
        return 1;
    }
    
    @Override
    public void run() {        
        long n, deltaT, startTime;
        threadRunning = true;
        double data_in = 0.0f;
        
        Node.debugMessage("Starting Slave",1000);
        
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
            //logNeighbor(data_in);
            Node.distAlgo.setNeighborVal(neighborID,data_in);
            
            //update dataStep
            Node.distAlgo.setTimeStep(Node.distAlgo.getTimeStep(id)+1, id);
            
            startTime = startTime+deltaT;
            
            //wait for exact time to send the data
            while (Node.distAlgo.getTimeStep(id) == (Node.distAlgo.getCurrentStep())){
                delay(5);
            }
            Node.debugMessage(Integer.toString(Node.distAlgo.getCurrentStep()),0,id+1,20);
        }
        Node.debugMessage("Slave Done",1000);
        threadRunning = false;        
    }
} 

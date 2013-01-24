/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

/**
 *
 * @author Unnati
 */
public class CommMaster extends CommChannel{   
    
    public CommMaster(){          
    }
    
    public CommMaster(String n, int ch_id, int n_id){
        super(n,ch_id, n_id);            
    }
    
    public void synchronize(){
        
    }
    
    
    @Override
    public int connect(String nodeName){
        //establish connection with a specific device as a master
        node = Bluetooth.getKnownDevice(nodeName);
        if (node == null){
            if (Node.DEBUG) {
                Node.debugMessage("Cannot find " + nodeName);
            }
           
            //discover and add devices
            discoverDevices();
            node = Bluetooth.getKnownDevice(nodeName);
        }
        //try once again
        if (node == null){
            if (Node.DEBUG) {
                Node.debugMessage("Cannot find " + nodeName,0,0,0);
                Node.debugMessage("Exiting",0,1);
            }
            return 0;
        } else{
            int connectTime = 1;
            nodeConnection = null;
            while (nodeConnection == null){
                Node.debugMessage("Trying... " + Integer.toString(connectTime),0,3);
                nodeConnection = Bluetooth.connect(nodeName, NXTConnection.PACKET);
                connectTime += 1;
                
                /*byte[] status = Bluetooth.getConnectionStatus();
                if (status == null){
                    nodeConnection.close();
                    if (Node.DEBUG) {
                        Node.debugMessage("Not Connected ",0,0,0);
                        Node.debugMessage("Exiting Connect",0,1);
                    }
                    //return 0;          
                }else{                      
                }*/
                delay(100);
            }
            if (Node.DEBUG) {
                Node.debugMessage("Connected");
            }
            connected = true;
            return 1;
        }
    }

    
    @Override
    public void run() {
        threadRunning = true;       
         while (Node.clock.getSyncStatus() !=  Node.SYNCSTATUS_SYNCED){
            delay(50);
        }
        
        long deltaT = Node.DELTA_T;   //500 ms
        long startTime = Node.STARTTIME;
        double data_in;
        if (Node.DEBUG) {
            Node.debugMessage("Starting Master",1000);                    
        }             
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
            //logNeighbor(data_in);
//            while (data_in == -1.0f){
//                data_in = readFloatData(false);
//                delay(5);
//            }
            Node.distAlgo.setNeighborVal(neighborID, data_in);            
            
            //update dataStep
            Node.distAlgo.setTimeStep(Node.distAlgo.getTimeStep(id) + 1, id);
            
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
        Node.debugMessage("Master Done",1000);
        threadRunning = false;
    }    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

import lejos.nxt.comm.Bluetooth;

/**
 *
 * @author Unnati
 */
public class CommMaster extends CommChannel{
    
    
    
    public CommMaster(){        
        
    }
    
    public CommMaster(String n){
        super(n);              
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
            nodeConnection = Bluetooth.connect(node);
            byte[] status = Bluetooth.getConnectionStatus();
            if (status == null ){
                nodeConnection.close();
                if (Node.DEBUG) {
                    Node.debugMessage("Not Connected ",0,0,0);
                    Node.debugMessage("Exiting",0,1);
                }
                return 0;          
            }else{
                if (Node.DEBUG) {
                    Node.debugMessage("Connected ");
                }  
                connected = true;
                return 1;
            }
        }
    }

    
    @Override
    public void run() {
        threadRunning = true;
        if (Node.DEBUG) {
            Node.debugMessage("Starting Master",1000);                    
        }
        for(int i=0;i<Node.COMMCOUNT;i++) {
            writeLongData(System.currentTimeMillis(),true);           
            readLongData(true);
            delay(10);            
        }
        Node.debugMessage("Master Done",1000);
        threadRunning = false;
    }
    
}

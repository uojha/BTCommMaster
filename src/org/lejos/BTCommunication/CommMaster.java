/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lejos.BTCommunication;

import lejos.nxt.LCD;
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
            LCD.clear();
            LCD.drawString("Cannot find " + nodeName,0,0);
            //discover and add devices
            discoverDevices();
            node = Bluetooth.getKnownDevice(nodeName);
        }
        //try once again
        if (node == null){
            LCD.clear();
            LCD.drawString("Cannot find " + nodeName,0,0);
            LCD.drawString("Exiting!",0,1);
            return 0;
        } else{
            nodeConnection = Bluetooth.connect(node);
            byte[] status = Bluetooth.getConnectionStatus();
            if (status == null ){
                nodeConnection.close();
                LCD.clear();
                LCD.drawString("Not connected ",0,0);
                LCD.drawString("Exiting!",0,1);
                delay(1000);
                return 0;          
            }else{
                LCD.clear();
                LCD.drawString("Connected",0,0);  
                connected = true;
                delay(1000);
                return 1;
            }
        }
    }

    
    @Override
    public void run() {
        threadRunning = true;
        LCD.drawString("Starting Master", 0, 4);
        delay(50);
        for(int i=0;i<Node.COMMCOUNT;i++) {
            writeLongData(System.currentTimeMillis(),true);
            readLongData(true);
            delay(5);            
        }
        threadRunning = false;
        

    }
    
}

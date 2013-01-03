package org.lejos.BTCommunication;

import lejos.nxt.LCD;
import lejos.nxt.comm.Bluetooth;

/**
 *
 * @author Unnati
 */
public class CommSlave extends CommChannel{
    
    public CommSlave(){        
        
    }
    
    public CommSlave(String n){
        super(n);              
    }
    
    @Override
    public int connect(){
        super.connect();
        LCD.drawString("Waiting...",0,0);
        LCD.refresh();
        while (!connected){          
            nodeConnection = Bluetooth.waitForConnection();
            delay(100);
            connected = true;
        } 
        LCD.clear();
        LCD.drawString("Connected...",0,1);
        return 1;
    }

    
    @Override
    public void run() {        
        long n;
        threadRunning = true;
        LCD.drawString("Starting Slave", 0, 4);
        delay(50);
        for(int i=0;i<Node.COMMCOUNT;i++){				
            n = readLongData(false);
            writeLongData(System.currentTimeMillis(),false);
            delay(5);
        }
        threadRunning = false;
        
    }
} 

package org.lejos.BTCommunication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import lejos.nxt.LCD;

/**
 * 
 * Test of leJOS NXT File System.
 * 
 * The example creates a file into leJOS NXT File System. 
 * In this case the file, is a KML file used by Google Earth.
 * If you use the command nxjbrowse, you could
 * download that file and to use with Google Earth.
 * 
 * 2008/04/18
 * Current version has problems when increase the size of the file.
 * 
 * @author Juan Antonio Brenha Moral, JAB
 *
 */

public class DataLogger {
	
	private byte[] byteText;
	private FileOutputStream fos;
        private File f;
        ArrayList <Long> writeTime = new ArrayList<Long>();
        ArrayList <Long> readTime = new ArrayList<Long>();
        
        /* constructor
         * @author U. Ojha
         */
        public DataLogger(){
        }
        
	
	/**
	 * This method convert any String into an Array of bytes
	 * 
	 * @param text to convert
	 * @return An Array of bytes.
	 * @author JAB
	 */ 
      private byte[] getBytes(String inputText){
    	//Debug Point
        byte[] nameBytes = new byte[inputText.length()+1];
        
        for(int i=0;i<inputText.length();i++){
            nameBytes[i] = (byte) inputText.charAt(i);
        }
        nameBytes[inputText.length()] = 0;
 
        return nameBytes;
     }
    
	/**
	 * This method add data into a file
	 * 
	 * @param text to add
	 * @author JAB
	 */  
    public void appendToFile(String text) throws IOException{
        byteText = getBytes(text);

        //Critic to add a useless character into file
        //byteText.length-1
        for(int i=0;i<byteText.length-1;i++){
            fos.write((int) byteText[i]);
        }    	
    }
    
    public void initialize(String fileName, String nodeName){
        try{
            f = new File(fileName);
            fos = new  FileOutputStream(f); 
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                
            }
            appendToFile("DATA FOR ");
            appendToFile(nodeName.toUpperCase()+"\n\n");
            LCD.clear();
            LCD.drawString(fileName + " created",0,0);
        }catch(IOException e){
            LCD.drawString(e.getMessage(),0,0);
            LCD.clear();
            LCD.drawString("Cant create " + fileName,0,0);            
        }
    }
    
    public void logWriteTime(){
        writeTime.add(System.currentTimeMillis());
    }
    
    public void logReadTime(){
        readTime.add(System.currentTimeMillis());
    }    
      
    public void saveData() throws IOException{        
        for(int i=0;i<writeTime.size();i++) {
            appendToFile(Long.toString(writeTime.get(i)));
            appendToFile("\t");
            appendToFile(Long.toString(readTime.get(i)));
            appendToFile("\n");
        }                
    }
   
    
    public void close(){
        try {
            fos.close();
        } catch (IOException ex) {
            //Logger.getLogger(DataLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

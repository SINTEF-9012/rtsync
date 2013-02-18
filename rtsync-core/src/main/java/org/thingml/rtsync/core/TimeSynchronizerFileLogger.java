/**
 * Copyright (C) 2012 SINTEF <franck.fleurey@sintef.no>
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingml.rtsync.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;

/**
 *
 * @author ffl
 */
public class TimeSynchronizerFileLogger implements ITimeSynchronizerLogger {

     /**************************************************************************
     *  Logging of the Time Synchronization
     * ************************************************************************/
    //protected File folder;
    protected boolean logging = false;
    protected boolean udpLog = false;

    public boolean isLogging() {
        return logging;
    }
    protected PrintWriter log;
    protected PrintWriter logRaw;
    protected DatagramSocket udpSocket;
    protected InetAddress IPAddress;
    
    private SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private String SEPARATOR = "\t";
    
    public String createSessionName() {
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return "TS-" + timestampFormat.format( Calendar.getInstance().getTime());
    }
    
    public void start_logging(File folder) {
       String sName = createSessionName(); 
       File sFolder = new File(folder, sName);
       sFolder.mkdir();
        try {
           log = new PrintWriter(new FileWriter(new File(sFolder, "Time_Synch.txt")));
           log.println("Time" + SEPARATOR + "TS" + SEPARATOR + "TMT" + SEPARATOR + "TMR" + SEPARATOR + "delay" + SEPARATOR + "offs" + SEPARATOR + "error" + SEPARATOR + "errorSum" + SEPARATOR + "zeroOffset" + SEPARATOR + "regOffsMs"+ SEPARATOR + "phase"+ SEPARATOR + "tsOffset");
           logRaw = new PrintWriter(new FileWriter(new File(sFolder, "Time_Synch_raw.txt")));
           logRaw.println("Time" + SEPARATOR + "TS" + SEPARATOR + "TMT" + SEPARATOR + "TMR" + SEPARATOR + "SeqExp" + SEPARATOR + "SeqRcv");
           logging = true;
           udpSocket = new DatagramSocket();
           IPAddress = InetAddress.getByName("127.0.0.1");
           udpLog = true;        
       } catch (IOException ex) {
           ex.printStackTrace();
       }
    }
    
    public void stop_logging() {
        if (logging) {
            logging = false;
            log.close();
            log = null;
            logRaw.close();
            logRaw = null;
            udpSocket.close();
            udpLog = false;
        }
    }
    
    private void sendUdp( int ch, long valEpoc, float val) {
        
        long txEpoc = System.currentTimeMillis();
        ch--; 
        // System.out.println("txEpoc = " + txEpoc);
        // System.out.println("valEpoc = " + valEpoc);
        String sendStr = new String("#" + " " + ch + " " + txEpoc + " " + valEpoc + " " + val);
        byte[] sendData;
        sendData = sendStr.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 30000);
        try {
            udpSocket.send(sendPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void timeSyncLog(String time, long ts, long tmt, long tmr, long delay, long offs, long error, long errorSum, long zeroOffset, long regOffsMs, int skipped, long tsOffset) {
        if (logging) log.println(time + SEPARATOR + ts + SEPARATOR + tmt + SEPARATOR + tmr + SEPARATOR + delay + SEPARATOR + offs + SEPARATOR + error + SEPARATOR + errorSum + SEPARATOR + zeroOffset + SEPARATOR + regOffsMs + SEPARATOR + skipped + SEPARATOR + tsOffset);
        //if (udpLog) sendUdp(1, tmt, delay); // TODO remove before checkin
        //if (udpLog) sendUdp(2, tmt, error); // TODO remove before checkin

    }

    @Override
    public void timeSyncStart() {
        
    }


    @Override
    public void timeSyncWrongSequence(int pingSeqNum, int pongSeqNum) {
    }

    @Override
    public void timeSyncDtsFilter(int dts) {
    }
    
     @Override
    public void timeSyncErrorFilter(int error) {
    }

    @Override
    public void timeSyncPingTimeout(int pingSeqNum, long tmt) {

    }

    @Override
    public void timeSyncReady() {
        
    }

    @Override
    public void timeSyncStop() {
       
    }

    @Override
    public void timeSyncPong(int delay, int dtt, int dtr, int dts) {
    
    }

    @Override
    public void timeSyncPongRaw(String time, int rcvPingSeqNum, int expectedPingSeqNum, long tmt, long tmr, long ts) {
        if (logging) logRaw.println(time + SEPARATOR + ts + SEPARATOR + tmt + SEPARATOR + tmr + SEPARATOR + expectedPingSeqNum + SEPARATOR + rcvPingSeqNum);

    }
    
}

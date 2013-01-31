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

    public boolean isLogging() {
        return logging;
    }
    protected PrintWriter log;
    
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
           log.println("Time" + SEPARATOR + "TS" + SEPARATOR + "TMT" + SEPARATOR + "TMR" + SEPARATOR + "delay" + SEPARATOR + "offs" + SEPARATOR + "errorSum" + SEPARATOR + "zeroOffset" + SEPARATOR + "regOffsMs"+ SEPARATOR + "phase");
           logging = true;
       } catch (IOException ex) {
           ex.printStackTrace();
       }
    }
    
    public void stop_logging() {
        if (logging) {
            logging = false;
            log.close();
            log = null;
        }
    }
    
    @Override
    public void timeSyncLog(String time, long ts, long tmt, long tmr, long delay, long offs, long errorSum, long zeroOffset, long regOffsMs, int skipped) {
        if (logging) log.println(time + SEPARATOR + ts + SEPARATOR + tmt + SEPARATOR + tmr + SEPARATOR + delay + SEPARATOR + offs + SEPARATOR + errorSum + SEPARATOR + zeroOffset + SEPARATOR + regOffsMs + SEPARATOR + skipped);
    }

    @Override
    public void timeSyncStart() {
        
    }

    @Override
    public void timeSyncStop() {
        
    }

    @Override
    public void timeSyncPong(int delay, int dtt, int dtr, int dts) {
        
    }
    
    @Override
    public void timeSyncReady() {
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
    
}

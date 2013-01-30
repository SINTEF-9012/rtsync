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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimeSynchronizer implements Runnable {

    /**
     * ************************************************************************
     * Configuration and parameters of the Time Sync Algorithm
     * ***********************************************************************
     */
    private int pingRate = 250;  // Rate for ping in milliseconds
    
    private int pingSeqMax = 8;  // Maximum sequence number for the pings
    
    private int dTsDeltaMax = 25; // delta time for TsFilter
    private int tsErrorMax = 50; // Maximum deviation for a calculated offset compared to regOffset
    
    private int zeroOffsetAvgSize = 10; // Number of samples to calculate the initial offset
    
    private int ts_maxvalue = 0x3FFF;   // 14 bits timestamp wrap-around
    private int ts_phase_frame = 4096;
    
    private int kInt = 64; // 1/ki - Number of milliseconds to inc/dec

    public int getPingSeqMax() {
        return pingSeqMax;
    }

    public void setPingSeqMax(int pingSeqMax) {
        this.pingSeqMax = pingSeqMax;
    }
    
    public int getTs_maxvalue() {
        return ts_maxvalue;
    }

    public int getdTsDeltaMax() {
        return dTsDeltaMax;
    }

    public void setdTsDeltaMax(int dTsDeltaMax) {
        this.dTsDeltaMax = dTsDeltaMax;
    }

    public int getTsErrorMax() {
        return tsErrorMax;
    }

    public void setTsErrorMax(int tsErrorMax) {
        this.tsErrorMax = tsErrorMax;
    }

    public int getZeroOffsetAvgSize() {
        return zeroOffsetAvgSize;
    }

    public void setZeroOffsetAvgSize(int zeroOffsetAvgSize) {
        this.zeroOffsetAvgSize = zeroOffsetAvgSize;
    }

    public int getTs_phase_frame() {
        return ts_phase_frame;
    }

    public void setTs_phase_frame(int ts_phase_frame) {
        this.ts_phase_frame = ts_phase_frame;
    }

    public int getkInt() {
        return kInt;
    }

    public void setkInt(int kInt) {
        this.kInt = kInt;
    }
    
    public int getPingRate() {
        return pingRate;
    }

    public void setPingRate(int pingRate) {
        this.pingRate = pingRate;
    }
 
    /**
     * ************************************************************************
     * constructor and link to the device
     * ***********************************************************************
     */
    protected TimeSynchronizable device;

    public TimeSynchronizer(TimeSynchronizable device, int ts_maxvalue) {
        this.device = device;
        this.ts_maxvalue = ts_maxvalue;
        this.ts_phase_frame = ts_maxvalue / 4;
    }
    
    /**
     * ************************************************************************
     * Logging
     * ***********************************************************************
     */
    protected ArrayList<ITimeSynchronizerLogger> loggers = new ArrayList<ITimeSynchronizerLogger>();

    public void addLogger(ITimeSynchronizerLogger l) {
        loggers.add(l);
    }

    public void removeLogger(ITimeSynchronizerLogger l) {
        loggers.remove(l);
    }

    public void removeAllLoggers() {
        loggers.clear();
    }
    
     /**
     * ************************************************************************
     * Algorithm internal data structures
     * ***********************************************************************
     */
    private long tsPrev = 0; // Previous TS value used for TsFilter
    private long tmrPrev = 0; // Previous TS value used for TsFilter
    private long tmtPrev = 0; // Previous TS value used for TsFilter
    
    private int zeroOffsetAvgCount = zeroOffsetAvgSize;
    private long zeroOffset = 0; // Calculated offset as a zero value. 
    // Changed using cuWrapIncrement when CU clock wraps
    //final private long cuWrapIncrement = 0x100000000L; // Wraps at (2^30)*4
    
    private long regOffset = 0; // Regulator output offset
    private long errorSum = 0; // Sum of error - used by integrator
    
    private int pingSeqNum = 0;
    private long tmt = 0;
    
    //final private int tmtArrSize = 10;  // Max number used to dimasion array for TMT
    //private long[] tmtArr = new long[tmtArrSize]; // Time for transmitting the "RequestTimeInfo"
    
    private static final int UNKNOWN_WRAP = 0;
    private static final int NO_WRAP = 1;
    private static final int BEFORE_WRAP = 2;
    private static final int AFTER_WRAP = 3;
    private int ts_phase = UNKNOWN_WRAP;
    private long ts_offset = 0;
    
    /**
     * ************************************************************************
     * Utility functions
     * ***********************************************************************
     */
    private SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public String currentTimeStamp() {
        return timestampFormat.format(Calendar.getInstance().getTime());
    }

    /**
     * ************************************************************************
     * Time synchronization algorithm
     * ***********************************************************************
     */
    public void start_timesync() {
        //refClockPrev = 0;
        state = INIT;
        ts_phase = UNKNOWN_WRAP;
        ts_offset = 0;
        tsPrev = 0; // Previous TS value used for TsFilter
        tmrPrev = 0;
        tmtPrev = 0;
        tmt = 0;
        zeroOffsetAvgCount = zeroOffsetAvgSize;
        zeroOffset = 0; // Calculated offset as a zero value. 
        regOffset = 0; // Regulator output offset
        errorSum = 0; // Sum of error - used by integrator
        start_ping();
        for (ITimeSynchronizerLogger l : loggers) {
            l.timeSyncStart();
        }
    }

    public void stop_timesync() {
        stop_ping();
        for (ITimeSynchronizerLogger l : loggers) {
            l.timeSyncStop();
        }
    }

    private void send_ping() {
        pingSeqNum++;
        
        if (pingSeqNum > pingSeqMax) { 
            pingSeqNum = 0;
        }
        
        tmt = System.currentTimeMillis();
        device.sendTimeRequest(pingSeqNum);
    }

    public long getRegOffset() {
        return regOffset;
    }
    /**
     * ************************************************************************
     * Handling of the pong and calculation of the clock offset
     * ***********************************************************************
     */
    private static int INIT = 0;
    private static int READY = 1;
    private int state = INIT;

    public void receive_TimeResponse(int timeSyncSeqNum, long value) {

        //--------------------------------------
        // 1) Validate the ping sequence number
        //--------------------------------------
        if (timeSyncSeqNum > pingSeqMax) {
            timeSyncSeqNum = 0;  // Range check of sequence number. Should never occur
            for (ITimeSynchronizerLogger l : loggers) l.timeSyncWrongSequence(pingSeqNum, timeSyncSeqNum);
            return;
        }
        if (tmt == 0 || timeSyncSeqNum != pingSeqNum) {// The the ping sequence number is not correct
            System.out.println("Skip - Rep seqNum = " + timeSyncSeqNum + " Req seqNum = " + pingSeqNum);
            for (ITimeSynchronizerLogger l : loggers) l.timeSyncWrongSequence(pingSeqNum, timeSyncSeqNum);
            return;
        }
        
        //----------------------------------------------
        // 1b) Management of the slave clock wrap around 
        //----------------------------------------------
        
        if (ts_phase == UNKNOWN_WRAP) {
            ts_offset = 0; // initialize
            if (value > (ts_maxvalue - ts_phase_frame)) ts_phase = BEFORE_WRAP;
            else if (value < ts_phase_frame) ts_phase = UNKNOWN_WRAP; // wait until we are out of the AFTER_WRAP zone to avoid negative ts
            else ts_phase = NO_WRAP;
        }
        else if (ts_phase == NO_WRAP && value > (ts_maxvalue - ts_phase_frame)) {
            ts_phase = BEFORE_WRAP;
        }
        else if (ts_phase == BEFORE_WRAP && value < ts_phase_frame) {
            ts_offset += ts_maxvalue + 1; // increment the offset
            ts_phase = AFTER_WRAP;
        }
        else if (ts_phase == AFTER_WRAP && value > ts_phase_frame) {
            ts_phase = NO_WRAP;
        }
    
        long ts = ts_offset + value;  // Slave timestamp which does not wrap around.
        
        //---------------------------------------------
        // 2) Collect all timing data (TMT, TMR and TS)
        //---------------------------------------------
        long tmr = System.currentTimeMillis();
        
        int dTmr = (int) (tmr - tmrPrev); // time between the 2 last Pongs
        int dTmt = (int) (tmt - tmtPrev); // Time between the 2 last Pings
        int dTs = (int) (ts - tsPrev); // Time between last ts - Used by TsFilter

        for (ITimeSynchronizerLogger l : loggers) l.timeSyncPong((int) (tmr - tmt), (int) dTmt, (int) dTmr, (int) dTs);

        tmrPrev = tmr;
        tmtPrev = tmt;
        tsPrev = ts;

        //---------------------------------------------------------
        // 3) Filter abnormal delays - dTs too different from dTmr
        //---------------------------------------------------------
        if (dTs < dTmt - dTsDeltaMax || dTs > dTmt + dTsDeltaMax) {
            System.out.println("Skip by dTs Filter - dTS = " + dTs);
            for (ITimeSynchronizerLogger l : loggers) l.timeSyncDtsFilter(dTs);
            return;
        }

        //---------------------------------------------------------
        // 4) Init the Zero offset in initialization phase
        //---------------------------------------------------------

        int delay = (int)(tmr - tmt) / 2; // round trip delay
        long offset = -ts + tmt + delay; // instant offset between PC and sensor clocks

        if (state == INIT) {
            zeroOffset += offset;
            if (zeroOffsetAvgCount == 1) {
                // Got correct number of samples....calculate average
                zeroOffset /= zeroOffsetAvgSize;
                regOffset = zeroOffset; // Initialize regOffset to "zero"
                System.out.println("TimeSync=>zeroOffset calculated");
                state = READY;
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncReady();
            }
            zeroOffsetAvgCount--;
            return;
        }

        //---------------------------------------------------------
        // 5) Running the regulator
        //---------------------------------------------------------
        
        long error = offset - regOffset;
        
        if (state == READY) {
            if (error > tsErrorMax) {
                System.out.println("Limit - error(+) = " + error);
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncErrorFilter((int)error);
            }
            else if (error < -tsErrorMax) {
                System.out.println("Limit - error(-) = " + error);
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncErrorFilter((int)error);
            }
            else {
                // Running integrator
                errorSum += error;
                regOffset = zeroOffset + (errorSum / kInt);
            }
        }

        for (ITimeSynchronizerLogger l : loggers) {
            l.timeSyncLog(currentTimeStamp(), ts, tmt, tmr, delay, offset, errorSum, zeroOffset, regOffset, ts_phase);
        }

    }

    /**
     * ************************************************************************
     * Translation of device timestanp into synchronized timestamp
     * ***********************************************************************
     */

    public long getSynchronizedEpochTime(int timestamp) {

        if (regOffset == 0 || ts_phase == UNKNOWN_WRAP) {
            return 0; // We are not yet synchronized
        }
        if (ts_phase == BEFORE_WRAP && timestamp < ts_phase_frame) {
            return this.getRegOffset() + ts_maxvalue + 1 + timestamp;
        }
        else if (ts_phase == AFTER_WRAP && timestamp > (ts_maxvalue - ts_phase_frame)) {
            return this.getRegOffset() - ts_maxvalue - 1 + timestamp;
        }
        else {
            return this.getRegOffset() + timestamp;
        }
    }
    /**
     * ************************************************************************
     * Sending of the time requests at reqular interval
     * ***********************************************************************
     */
    protected boolean stop_request = false;
    protected boolean running = false;

    public boolean isRunning() {
        return running;
    }

    protected void start_ping() {
        if (!running) {
            stop_request = false;
            new Thread(this).start();
        }
    }

    protected void stop_ping() {
        if (running) {
            stop_request = true;
        }
    }

    @Override
    public void run() {
        running = true;
        try {
            do {
                // initiate a ping every "period" milliseconds
                send_ping();
                Thread.sleep(pingRate);
            } while (!stop_request);
        } catch (InterruptedException ex) {
            Logger.getLogger(TimeSynchronizer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            running = false;
        }
    }
}

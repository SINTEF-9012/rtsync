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
     * TimesyncV2 - Extension for loggable timesync looping offset out on the the sensor
     * ***********************************************************************
     */
//    private long lastSentOffset = 0;
    private long lastRegOffset = 0;
    private boolean timesyncV2Active = false;

    private int pingSeqV2Detect = 9;    // TimesyncV2 will change this sequence number to 1
    private boolean detectedV2 = false; // TimesyncV2 detected - Extension for loggable timesync looping offset out on the the sensor
    private boolean sentFullEpochV2 = false; // TimesyncV2 sent full Epoch after connect
    private long lastReceivedEpochV2 = 0; // TimesyncV2 lastReceived Epoch from slave ... should be the same as this.getOffset() except for wrap
    private boolean compareReceivedEpochV2 = false; // TimesyncV2 use compare lastReceived Epoch from slave (for test) ... this must be disabled during playback of stored data
    /**
     * ************************************************************************
     * Configuration and parameters of the Time Sync Algorithm
     * ***********************************************************************
     */
    private int pingRate = 250;  // Rate for ping in milliseconds
    
    private int pingSeqMax = 9;  // Maximum sequence number for the pings
    private int pingSeqMin = 0;  // Minimum sequence number for the pings
    private int pingSeqMod = 8;  // Modulo number for the pings. Higher numbers are used for version information

    private int dTsDeltaMax = 25; // delta time for TsFilter
    private int tsErrorMax = 75; // Maximum deviation for a calculated offset compared to regOffset
    private int tsErrorMaxCounter = 0; // Counts number of consequtive errors above tsErrorMax
    private static final int TS_ERROR_MAX_THRESHOLD = 40; // Threshold for restart of timeSync
    
    private int zeroOffsetAvgSize = 10; // Number of samples to calculate the initial offset
    
    private int ts_maxvalue = 0x3FFF;   // 14 bits timestamp wrap-around
    private int ts_phase_frame = 4096;
    
    private int kInt = 64; // 1/ki - Number of milliseconds to inc/dec
    
    private int ts_manual_offset = 1; // Manual offset added to the calculated error. Used for testing
    private long tsScatter0Offs = 100;
    private int tsScatter0Delay = 100;
    private long tsScatter1Offs = 100;
    private int tsScatter1Delay = 100;
    private int tsScatterFrameCount = 0; // PingPong counter
    private int tsScatterFrameMax = 50;  // Number of PingPong to scan before decide
    private long tsScatterOffsSum = 0;
    
    private void tsScatterInit() {
        tsScatter0Offs = 100;
        tsScatter0Delay = 100;
        tsScatter1Offs = 100;
        tsScatter1Delay = 100;
        tsScatterFrameCount = 0; // PingPong counter
        tsScatterFrameMax = 50;  // Number of PingPong to scan before decide
        tsScatterOffsSum = 0;
    }

    private void tsScatterNewPingPong(int delay, long offs) {
        if (tsScatterFrameCount < 0) {
            // Hold off to assure previous correction to take effect
            tsScatterFrameCount++;
            return;
        }
        if (tsScatterFrameCount < tsScatterFrameMax) {
            tsScatterFrameCount++;
            if (delay < tsScatter0Delay) {
                // New lowest delay....
                tsScatter1Offs = tsScatter0Offs;
                tsScatter1Delay = tsScatter0Delay;
                tsScatter0Offs = offs;
                tsScatter0Delay = delay;
                //System.out.println("tsScatter 0 [" + tsScatter0Offs + "," + tsScatter0Delay + "] tsScatter 1 [" + tsScatter1Offs + "," + tsScatter1Delay + "]");
            } else if (delay < tsScatter1Delay) {
                // New second lowest delay...
                tsScatter1Offs = offs;
                tsScatter1Delay = delay;
                //System.out.println("tsScatter 0 [" + tsScatter0Offs + "," + tsScatter0Delay + "] tsScatter 1 [" + tsScatter1Offs + "," + tsScatter1Delay + "]");
            }
        } else
        {
            tsScatterOffsSum += (tsScatter0Offs + tsScatter1Offs) / 2;
            tsScatterFrameCount = -kInt;
            tsScatter0Offs = 100;
            tsScatter0Delay = 100;
            tsScatter1Offs = 100;
            tsScatter1Delay = 100;
            System.out.println("tsScatterOffsSum " + tsScatterOffsSum);
        }
    }

    private long tsScatterOffsSum() {
        return tsScatterOffsSum;
    }
    
    public int getManualOffset() {
        return ts_manual_offset;
    }

    public void setManualOffset(int offset) {
        this.ts_manual_offset = offset;
    }

    public int getPingSeqMax() {
        return pingSeqMax;
    }

    public void setPingSeqMax(int pingSeqMax) {
        this.pingSeqMax = pingSeqMax;
    }
    
    public int getPingSeqMin() {
        return pingSeqMin;
    }

    public void setPingSeqMin(int pingSeqMin) {
        this.pingSeqMin = pingSeqMin;
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
    protected TimeSynchronizableV2 deviceV2;

    public TimeSynchronizer(TimeSynchronizable device, int ts_maxvalue) {
        this.device = device;
        if (device instanceof TimeSynchronizableV2) {
            this.deviceV2 = (TimeSynchronizableV2)device;
        } else {
            this.deviceV2 = null;
        }
        this.ts_maxvalue = ts_maxvalue;
        this.ts_phase_frame = ts_maxvalue / 4;
    }


//    public TimeSynchronizer(TimeSynchronizableV2 deviceV2, int ts_maxvalue) {
//        this.device = deviceV2;
//        this.deviceV2 = deviceV2;
//        this.ts_maxvalue = ts_maxvalue;
//        this.ts_phase_frame = ts_maxvalue / 4;
//    }
    
    
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
        tsErrorMaxCounter = 0;
//        lastSentOffset = 0;
        lastRegOffset = 0;
        timesyncV2Active = false;
        compareReceivedEpochV2 = false;
        detectedV2 = false;
        sentFullEpochV2 = false;
        start_ping();
        tsScatterInit();
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

    private boolean got_pong = false;
    private Object pongMonitor = "Thread synchronization monitor";
    
    private void send_ping() {
        
        synchronized(pongMonitor) {
            try {
                if (!got_pong) {
                    System.out.println("Pong given extra time");
                    pongMonitor.wait(pingRate*3);
                } // Give some time for the pong to arrive
            } catch (InterruptedException ex) {
                Logger.getLogger(TimeSynchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (!got_pong) { // Timeout, the pong is declared lost
                System.out.println("Pong timeout");
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncPingTimeout(pingSeqNum, tmt);
            }
            
        got_pong = false;
        // Send the next ping
        pingSeqNum++;
        if (pingSeqNum > pingSeqMax) { 
            pingSeqNum = pingSeqMin;
        }
        
        tmt = System.currentTimeMillis();
        device.sendTimeRequest(pingSeqNum);
        }
        
        
    }

    public long getOffset() {
        return regOffset + ts_offset;
    }
    /**
     * ************************************************************************
     * Handling of the pong and calculation of the clock offset
     * ***********************************************************************
     */
    private static int INIT = 0;
    private static int READY = 1;
    private int state = INIT;

    public synchronized void receive_TimeResponse(int timeSyncSeqNum, long value) {

        //--------------------------------------
        // 0) Log the raw pong data
        //--------------------------------------
        long tmr = System.currentTimeMillis();
        for (ITimeSynchronizerLogger l : loggers) l.timeSyncPongRaw( currentTimeStamp(), pingSeqNum, timeSyncSeqNum, tmt, tmr, value);
        
        //--------------------------------------
        // 1) Validate the ping sequence number
        //--------------------------------------
        synchronized(pongMonitor) {
            if (timeSyncSeqNum > pingSeqMax) {
                timeSyncSeqNum = pingSeqMin;  // Range check of sequence number. Should never occur
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncWrongSequence(pingSeqNum, timeSyncSeqNum);
                return;
            }
            if (tmt == 0 || (timeSyncSeqNum % pingSeqMod) != (pingSeqNum % pingSeqMod)) {// The the ping sequence number is not correct
                for (ITimeSynchronizerLogger l : loggers) l.timeSyncWrongSequence(pingSeqNum, timeSyncSeqNum);
                return;
            }
            if (timeSyncSeqNum != pingSeqNum) {  // The remote indicates version information
                if ( pingSeqNum == pingSeqV2Detect) {
                    if (detectedV2 == false)
                        System.out.println("receive_TimeResponse() TimesyncV2 slave detected");
                    detectedV2 = true;
                }
            }
            got_pong = true;
            pongMonitor.notify();
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
                //System.out.println("Goto BEFORE_WRAP");
            }
            else if (ts_phase == NO_WRAP && value < ts_phase_frame) {
                ts_phase = AFTER_WRAP; // Cleanup after pause ... not normal case
                System.out.println("Catch up error wrap - goes directly to AFTER_WRAP");
            }
            else if (ts_phase == BEFORE_WRAP && value < ts_phase_frame) {
                ts_offset += ts_maxvalue + 1; // increment the offset
                ts_phase = AFTER_WRAP;
                //System.out.println("Goto AFTER_WRAP");
                if ((detectedV2 == true) && (sentFullEpochV2 == false)) {
                    updateRemoteOffset(regOffset, true);  // Send full Epoch to slave - timesyncV2
                    sentFullEpochV2 = true;
                }
            }
            else if (ts_phase == BEFORE_WRAP && value < (ts_maxvalue - ts_phase_frame)) {
                ts_phase = NO_WRAP; // Cleanup after pause ... not normal case
                System.out.println("Catch up error wrap - goes directly to NO_WRAP");
            }
            else if (ts_phase == AFTER_WRAP && value > (ts_maxvalue - ts_phase_frame)) {
                ts_phase = BEFORE_WRAP; // Cleanup after pause ... not normal case
                System.out.println("Catch up error wrap - goes directly to BEFORE_WRAP");
            }
            else if (ts_phase == AFTER_WRAP && value > ts_phase_frame) {
                ts_phase = NO_WRAP;
                //System.out.println("Goto NO_WRAP");
            }

            long ts = ts_offset + value;  // Slave timestamp which does not wrap around.

            //----------------------------------------------
            // 1c) Send initial full EPOCH update to slave
            //----------------------------------------------

            if ((detectedV2 == true) && (sentFullEpochV2 == false)) {
                if (state == READY) {
                    if ((ts_phase != UNKNOWN_WRAP) && (ts_phase != BEFORE_WRAP)) {
                        updateRemoteOffset(regOffset, true);  // Send full Epoch to slave - timesyncV2
                        sentFullEpochV2 = true;
                    }
                }
            }
            
            
            //---------------------------------------------
            // 2) Collect all timing data (TMT, TMR and TS)
            //---------------------------------------------
            //long tmr = System.currentTimeMillis();

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

            tsScatterNewPingPong(delay, offset - regOffset);
            long scatterOffs = ts_manual_offset;
                if (ts_manual_offset == 0) {
                    scatterOffs = -tsScatterOffsSum();
                }  else {
                    tsScatterInit(); // Hold in reset to avoid OffsetSum buildup
                }
                
            long error = offset - regOffset - scatterOffs;
            
            final int MAX_NORMAL = 0;
            final int MAX_DETECTED = 1;
            int errorMaxDetected = MAX_NORMAL;
            
            if (state == READY) {
                //int high_limit = tsErrorMax;
                if (error > 15000) {
                    // There has been a pause in pingpong and device has wrapped ... catch up needed
                    System.out.println("Catch up error(+) = " + error);
                    while (error > 15000) {
                        //System.out.println("Catch up error(+) = " + error);
                        error -= ts_maxvalue + 1;
                        ts_offset += ts_maxvalue + 1; // increment the offset
                    }
                }
                if (error < -15000) {
                    // There has been a pause in pingpong and device has wrapped ... catch up needed
                    System.out.println("Catch up error(-) = " + error);
                    while (error < -15000) {
                        //System.out.println("Catch up error(+) = " + error);
                        error += ts_maxvalue + 1;
                        ts_offset -= ts_maxvalue + 1; // increment the offset
                    }
                }

                if (error > (tsErrorMax)) {
                    System.out.println("Limit - error(+) = " + error + " limit = " + tsErrorMax);
                    for (ITimeSynchronizerLogger l : loggers) l.timeSyncErrorFilter((int)error);
                    error = (tsErrorMax); // Limit to +max
                    errorMaxDetected = MAX_DETECTED;
                } 
                
                //int low_limit = -tsErrorMax;
                if (error < -tsErrorMax) {
                    System.out.println("Limit - error(-) = " + error + " limit = " + -tsErrorMax);
                    for (ITimeSynchronizerLogger l : loggers) l.timeSyncErrorFilter((int)error);
                    error = -tsErrorMax; // Limit to -max
                    errorMaxDetected = MAX_DETECTED;
                }
                //System.out.println("high_limit = " + high_limit + " error = " + error + " low_limit = " + low_limit);
                
                {
                    // Running integrator
                    errorSum += error;
                    regOffset = zeroOffset + (errorSum / kInt);
                    updateRemoteOffset(regOffset, false);
                }
            }

            for (ITimeSynchronizerLogger l : loggers) {
                l.timeSyncLog(currentTimeStamp(), ts, tmt, tmr, delay, offset, error+scatterOffs, errorSum, zeroOffset, regOffset, ts_phase, ts_offset);
            }

            if (errorMaxDetected == MAX_DETECTED) {
                tsErrorMaxCounter++;
                if (tsErrorMaxCounter > TS_ERROR_MAX_THRESHOLD) {
                    this.start_timesync();
                    System.out.println("tsErrorMaxCounter : " + tsErrorMaxCounter + " => force restart");
                }
            } else {
                tsErrorMaxCounter = 0;
            }
                
        }

    }

    /**
     * ************************************************************************
     * Translation of device timestamp into synchronized timestamp
     * ***********************************************************************
     */

    long testDiffLast = 0;
    long testDiffLastCount = 0;
    public long getSynchronizedEpochTime(int timestamp) {

        long v1Time = getSynchronizedEpochTimeV1(timestamp);
        long v2Time = lastReceivedEpochV2 + timestamp;        
        long ret    = v1Time;
        
        if ( timesyncV2Active == true )
            ret = v2Time;
        
        if ( compareReceivedEpochV2 == true) {
            if ( timesyncV2Active == true ) {
                long diff = v1Time - v2Time;
                if ( (diff > 1) || (diff < -1) ) {
                    System.out.println("getSynchronizedEpochTime() - timestamp: "+timestamp+" v1 : "+v1Time+" v2 : "+v2Time);
                    if ( testDiffLast == diff ) {
                        testDiffLastCount++;
                    } else {
                        if (testDiffLastCount != 0)
                            System.out.println("getSynchronizedEpochTime() - got diff : "+ testDiffLast + " count : "+ testDiffLastCount);
                        testDiffLast = diff;
                        testDiffLastCount = 0;
                    }
                } else {
                    if (testDiffLastCount != 0) {
                        System.out.println("getSynchronizedEpochTime() - timestamp: "+timestamp+" v1 : "+v1Time+" v2 : "+v2Time);
                        System.out.println("getSynchronizedEpochTime() - got diff : "+ testDiffLast + " count : "+ testDiffLastCount);
                    }
                    testDiffLast = 0;
                    testDiffLastCount = 0;
                }
            }
        }
        
        return (ret);
    }

    private long getSynchronizedEpochTimeV1(int timestamp) {

        if (regOffset == 0 || ts_phase == UNKNOWN_WRAP) {
            return 0; // We are not yet synchronized
        }
        if (ts_phase == BEFORE_WRAP && timestamp < ts_phase_frame) {
            return this.getOffset() + ts_maxvalue + 1 + timestamp;
        }
        else if (ts_phase == AFTER_WRAP && timestamp > (ts_maxvalue - ts_phase_frame)) {
            return this.getOffset() - ts_maxvalue - 1 + timestamp;
        }
        else {
            return this.getOffset() + timestamp;
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

    public void start_ping() {
        if (!running) {
            stop_request = false;
            new Thread(this).start();
        }
    }

    public void stop_ping() {
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

    /**
     * ************************************************************************
     * TimesyncV2 - Extension for loggable timesync looping offset out on the the sensor
     * ***********************************************************************
     */
    private void updateRemoteOffset( long currRegOffset, boolean fullUpdate) {
        long currentOffset = this.getOffset();
        if (deviceV2 == null) {
            //System.out.println("updateRemoteOffset() send failed due to no deviceV2 driver registered");
            return;
        }
        if (detectedV2 == false) {
            //System.out.println("updateRemoteOffset() send failed due to no deviceV2 detected");
            return;
        }
        
        if (fullUpdate == true) {
            if ( detectedV2 ) {
                deviceV2.sendEpochCorr(0x7fffffffffffffffl); // Force full update
                deviceV2.sendEpochCorr(currentOffset);
                //System.out.println("updateRemoteOffset() send full update : " + currentOffset + " currRegOffset : " + currRegOffset);
                lastRegOffset = currRegOffset;
//                lastSentOffset = currentOffset;
            } 
        } else {
            if (lastRegOffset != currRegOffset) {
                if ( deviceV2 != null ) {
                    long corr =  currRegOffset - lastRegOffset;
                    //System.out.println("updateRemoteOffset() send corr : " + currentOffset + " currRegOffset : " + currRegOffset + " corr: "+ corr);
                    if ( detectedV2 ) {
                        deviceV2.sendEpochCorr(corr);
                        lastRegOffset = currRegOffset;
//                        lastSentOffset = currentOffset;
                    }
                } else {
                    // System.out.println("updateRemoteOffset() Not supported for old drivers ");
                }
            }
            
        }
    }
    
    public void compareReceivedEpoch( boolean val ) {
        System.out.println("compareReceivedEpoch()       got val : "+ val);
        compareReceivedEpochV2 = val;
    }
    
    public void receiveEpoch( long epoch) {
        System.out.println("receiveEpoch()       got epoch : "+ epoch + " diff : "+ (epoch-lastReceivedEpochV2));
        lastReceivedEpochV2 = epoch;
        timesyncV2Active = true;
    }

}

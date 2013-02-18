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

/**
 *
 * @author ffl
 */
public class TimeSynchronizerPrintLogger implements ITimeSynchronizerLogger {

    private String SEPARATOR = "\t";
    
    @Override
    public void timeSyncLog(String time, long ts, long tmt, long tmr, long delay, long offs, long error, long errorSum, long zeroOffset, long regOffsMs, int skipped, long tsOffset) {
        System.out.println("TimeSync:" + time + SEPARATOR + ts + SEPARATOR + tmt + SEPARATOR + tmr + SEPARATOR + delay + SEPARATOR + offs + SEPARATOR + error + SEPARATOR + errorSum + SEPARATOR + zeroOffset + SEPARATOR + regOffsMs + SEPARATOR + skipped + SEPARATOR + tsOffset);
    }

    @Override
    public void timeSyncStart() {
        System.out.println("TimeSync: START.");
    }

    @Override
    public void timeSyncStop() {
        System.out.println("TimeSync: STOP.");
    }

    @Override
    public void timeSyncPong(int delay, int dtt, int dtr, int dts) {
        System.out.println("TimeSync: PONG delay=" + delay + SEPARATOR + dtt + SEPARATOR + dtr + SEPARATOR + dts);
    }
    
    @Override
    public void timeSyncReady() {
        System.out.println("TimeSync: READY!");
    }

    @Override
    public void timeSyncWrongSequence(int pingSeqNum, int pongSeqNum) {
        System.out.println("TimeSync: Got an unexpected pong (after another ping was sent) - seqNum = " + pongSeqNum);
    }

    @Override
    public void timeSyncDtsFilter(int dts) {
        System.out.println("TimeSync: Drop by Dts filter - dts = " + dts);
    }
    
     @Override
    public void timeSyncErrorFilter(int error) {
         System.out.println("TimeSync: Drop by Error filter - calculated error out of bounds: " + error);
    }

    @Override
    public void timeSyncPingTimeout(int pingSeqNum, long tmt) {
        System.out.println("TimeSync: Timout for ping " + pingSeqNum + " after " + (System.currentTimeMillis() - tmt) + "ms.");
    }

    @Override
    public void timeSyncPongRaw(String time, int rcvPingSeqNum, int expectedPingSeqNum, long tmt, long tmr, long value) {
    }
    
}

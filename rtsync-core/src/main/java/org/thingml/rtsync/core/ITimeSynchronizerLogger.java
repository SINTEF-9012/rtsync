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


public interface ITimeSynchronizerLogger {
    public void timeSyncStart();
    public void timeSyncReady();   
    public void timeSyncStop();   
    public void timeSyncPingTimeout(int pingSeqNum, long tmt);
    public void timeSyncWrongSequence(int pingSeqNum, int pongSeqNum);
    public void timeSyncPong(int delay, int dtt, int dtr, int dts);
    public void timeSyncDtsFilter(int dts);
    public void timeSyncErrorFilter(int error);
    public void timeSyncLog(String time, long ts, long tmt, long tmr, long delay, long offs, long errorSum, long zeroOffset, long regOffsMs, int skipped);
    
}

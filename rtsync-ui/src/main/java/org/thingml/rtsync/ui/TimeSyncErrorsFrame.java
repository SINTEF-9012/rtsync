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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thingml.rtsync.ui;

import java.awt.Color;
import org.thingml.rtcharts.swing.*;
import org.thingml.rtsync.core.*;

/**
 *
 * @author ffl
 */
public class TimeSyncErrorsFrame extends javax.swing.JFrame implements ITimeSynchronizerLogger {

    protected GraphBuffer btimeout = new GraphBuffer(100);
    protected GraphBuffer bwrongseq = new GraphBuffer(100);
    protected GraphBuffer bdtsdrop = new GraphBuffer(100);
    protected GraphBuffer berrdrop = new GraphBuffer(100);
    
   
    protected TimeSynchronizer ts = null;
    
    /**
     * Creates new form TimeSynchronizerFrame
     */
    public TimeSyncErrorsFrame(TimeSynchronizer ts) {
        initComponents();
        this.ts = ts;
        ts.addLogger(this);
        ((GraphPanel)jPanel2).start();
        ((GraphPanel)jPanel3).start();
        ((GraphPanel)jPanel4).start();
        ((GraphPanel)jPanel5).start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new BarGraphPanel(btimeout, "Timeout waiting for Pong", 0, 1, 1, Color.red);
        jPanel3 = new BarGraphPanel(bwrongseq, "Received wrong pong", 0, 1, 1, Color.red);
        jPanel4 = new BarGraphPanel(bdtsdrop, "Packets droped by Dts Filter", 0, 500, 150, Color.red);
        jPanel5 = new BarGraphPanel(berrdrop, "Packets droped by Error filter", -100, 100, 50, Color.red);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.PAGE_AXIS));
        jPanel1.add(jPanel2);
        jPanel1.add(jPanel3);
        jPanel1.add(jPanel4);
        jPanel1.add(jPanel5);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
          if (ts != null) {
              ts.removeLogger(this);
          }
          ((GraphPanel)jPanel2).stop();
          ((GraphPanel)jPanel3).stop();
          ((GraphPanel)jPanel4).stop();
          ((GraphPanel)jPanel5).stop();
    }//GEN-LAST:event_formWindowClosed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    // End of variables declaration//GEN-END:variables

    @Override
    public void timeSyncStart() {
        
    }

    @Override
    public void timeSyncStop() {
        
    }

    @Override
    public void timeSyncLog(String time, long ts, long tmt, long tmr, long delay, long offs, long errorSum, long zeroOffset, long regOffsMs, int skipped, long tsOffset) {
        /*
        int del = (int) delay;
        int err = (int) (offs - regOffsMs);
        bxyerr.appendDataRow(new int[] {del, err});
        berr.insertData(err);
        bdelay.insertData(del);
        bdrop.insertData(0);
        */
        berrdrop.insertData(0);
        bdtsdrop.insertData(0);
        bwrongseq.insertData(0);
    }

    @Override
    public void timeSyncPong(int delay, int dtt, int dtr, int dts) {
        btimeout.insertData(0);
    }

    @Override
    public void timeSyncReady() {
    }

    @Override
    public void timeSyncWrongSequence(int pingSeqNum, int pongSeqNum) {
        bwrongseq.insertData(1);
    }

    @Override
    public void timeSyncDtsFilter(int dts) {
        bdtsdrop.insertData(dts);
    }

    @Override
    public void timeSyncErrorFilter(int error) {
        berrdrop.insertData(error);
    }

    @Override
    public void timeSyncPingTimeout(int pingSeqNum, long tmt) {
        btimeout.insertData(1);
    }
}

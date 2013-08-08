/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft.client;

/**
 *
 * @author alex_000
 */

public interface IMonitor {
    void setMaximum(int max);
    void setNote(String note);
    void setProgress(int progress);
    void close();
}
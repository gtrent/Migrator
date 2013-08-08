/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft.client;

import java.io.File;
import javax.swing.JApplet;
import javax.swing.JOptionPane;

/**
 *
 * @author alex_000
 */
public class Minecraft {
    
    private static File techDummy = null;
    public static String params;
    
    public static void main(String[] args) {
        JApplet MineApplet = new MinecraftApplet();
        for (String arg : args) {
            params += arg + "\n";
        }
        JOptionPane.showMessageDialog(null, "Debug: " + params, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.minecraft.client;

import argo.format.PrettyJsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonField;
import argo.jdom.JsonNode;
import argo.jdom.JsonNodeFactories;
import argo.jdom.JsonRootNode;
import argo.jdom.JsonStringNode;
import argo.saj.InvalidSyntaxException;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author alex_000
 */
public class MinecraftApplet extends javax.swing.JApplet {

    /**
     * Initializes the applet MinecraftApplet
     */

    private static List<String> grabbed;
    
    public boolean isPathValid(File targetDir) {
        if (targetDir.exists()) {
            File launcherProfiles = new File(targetDir,"launcher_profiles.json");
            return launcherProfiles.exists();
        }
        return false;
    }


    public String getFileError(File targetDir) {
        if (targetDir.exists()) {
            return "The directory is missing a launcher profile. Please run the minecraft launcher first";
        } else {
            return "There is no minecraft directory set up. Either choose an alternative, or run the minecraft launcher to create one";
        }
    }

    public String getSuccessMessage() {
        return String.format("Successfully installed client profile %s into launcher and grabbed %d required libraries",VersionInfo.getModpackName(), grabbed.size());
    }
    
    public void main(){
        init();
    }
    
    @Override
    public void init() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MinecraftApplet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MinecraftApplet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MinecraftApplet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MinecraftApplet.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the applet */
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    initComponents();
                    String userHomeDir = System.getProperty("user.home", ".");
                    String osType = System.getProperty("os.name").toLowerCase();
                    File target;
                    String mcDir = ".minecraft";
                    if (osType.contains("win") && System.getenv("APPDATA") != null) {
                        target = new File(System.getenv("APPDATA"), mcDir);
                    } else if (osType.contains("mac")) {
                        target = new File(new File(new File(userHomeDir, "Library"),"Application Support"),"minecraft");
                    } else {
                        target = new File(userHomeDir, mcDir);
                    }

                    if (!target.exists()) {
                        JOptionPane.showMessageDialog(null, "There is no minecraft installation at this location!", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }
                    File launcherProfiles = new File(target,"launcher_profiles.json");
                    if (!launcherProfiles.exists()) {
                        JOptionPane.showMessageDialog(null, "There is no minecraft launcher profile at this location, you need to run the launcher first!", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }
                    
                    File versionRootDir = new File(target,"versions");
                    File versionTarget = new File(versionRootDir,VersionInfo.getModpackName());
                    if (!versionTarget.mkdirs() && !versionTarget.isDirectory()) {
                        if (!versionTarget.delete()) {
                            JOptionPane.showMessageDialog(null, "There was a problem with the launcher version data. You will need to clear "+versionTarget.getAbsolutePath()+" manually", "Error", JOptionPane.ERROR_MESSAGE);

                        } else {
                            versionTarget.mkdirs();
                        }
                    }

                    //javaw.exe -Xmx1G -Djava.library.path="%APPDATA%\.minecraft\versions\1.6.2\1.6.2-natives" -cp "%APPDATA%\.minecraft\libraries\*;%APPDATA%\.minecraft\versions\1.6.2\1.6.2.jar" net.minecraft.client.main.Main --username %1 --session ${auth_session} --version 1.6.2 --gameDir %2%3%4%5%6%7%8%9 --assetsDir %2%3%4%5%6%7%8%9assets
                    
                    File versionJsonFile = new File(versionTarget,VersionInfo.getModpackName()+".json");
                    File clientJarFile = new File(versionTarget, VersionInfo.getModpackName()+".jar");
                    File minecraftJarFile = VersionInfo.getMinecraftFile(versionRootDir);
                    try {
                        Files.copy(minecraftJarFile, clientJarFile);
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, "You need to run the version "+VersionInfo.getMinecraftVersion()+" manually at least once", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }
                    File librariesDir = new File(target,"libraries");
                    File targetLibraryFile = VersionInfo.getLibraryPath(librariesDir);
                    IMonitor monitor = DownloadUtils.buildMonitor();
                    List<JsonNode> libraries = VersionInfo.getVersionInfo().getArrayNode("libraries");
                    monitor.setMaximum(libraries.size() + 2);
                    int progress = 2;
                    grabbed = Lists.newArrayList();
                    List<String> bad = Lists.newArrayList();
                    progress = DownloadUtils.downloadInstalledLibraries("clientreq", librariesDir, monitor, libraries, progress, grabbed, bad);

                    monitor.close();
                    if (bad.size() > 0) {
                        String list = Joiner.on(", ").join(bad);
                        JOptionPane.showMessageDialog(null, "These libraries failed to download. Try again.\n"+list, "Error downloading", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }

                    if (!targetLibraryFile.getParentFile().mkdirs() && !targetLibraryFile.getParentFile().isDirectory()) {
                        if (!targetLibraryFile.getParentFile().delete()) {
                            JOptionPane.showMessageDialog(null, "There was a problem with the launcher version data. You will need to clear "+targetLibraryFile.getAbsolutePath()+" manually", "Error", JOptionPane.ERROR_MESSAGE);
                            //return false;
                        } else {
                            targetLibraryFile.getParentFile().mkdirs();
                        }
                    }


                    JsonRootNode versionJson = JsonNodeFactories.object(VersionInfo.getVersionInfo().getFields());

                    try {
                        BufferedWriter newWriter = Files.newWriter(versionJsonFile, Charsets.UTF_8);
                        PrettyJsonFormatter.fieldOrderPreservingPrettyJsonFormatter().format(versionJson,newWriter);
                        newWriter.close();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "There was a problem writing the launcher version data,  is it write protected?", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }

                    try {
                        VersionInfo.extractFile(targetLibraryFile);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "There was a problem writing the system library file:\n" + e, "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }

                    JdomParser parser = new JdomParser();
                    JsonRootNode jsonProfileData = null;

                    try {
                        jsonProfileData = parser.parse(Files.newReader(launcherProfiles, Charsets.UTF_8));
                    } catch (InvalidSyntaxException e) {
                        JOptionPane.showMessageDialog(null, "The launcher profile file is corrupted. Re-run the minecraft launcher to fix it!", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    } catch (Exception e) {
                        throw Throwables.propagate(e);
                    }


                    JsonField[] fields = new JsonField[] {
                        JsonNodeFactories.field("name", JsonNodeFactories.string(VersionInfo.getModpackName())),
                        JsonNodeFactories.field("lastVersionId", JsonNodeFactories.string(VersionInfo.getModpackName())),
                        JsonNodeFactories.field("useHopperCrashService", JsonNodeFactories.booleanNode(false)),
                        JsonNodeFactories.field("gameDir", JsonNodeFactories.string(target.getParentFile() + File.separator + ".technic" + File.separator + VersionInfo.getModpackName())),
                    };

                    HashMap<JsonStringNode, JsonNode> profileCopy = Maps.newHashMap(jsonProfileData.getNode("profiles").getFields());
                    HashMap<JsonStringNode, JsonNode> rootCopy = Maps.newHashMap(jsonProfileData.getFields());
                    profileCopy.put(JsonNodeFactories.string(VersionInfo.getModpackName()), JsonNodeFactories.object(fields));
                    JsonRootNode profileJsonCopy = JsonNodeFactories.object(profileCopy);

                    rootCopy.put(JsonNodeFactories.string("profiles"), profileJsonCopy);

                    jsonProfileData = JsonNodeFactories.object(rootCopy);

                    try {
                        BufferedWriter newWriter = Files.newWriter(launcherProfiles, Charsets.UTF_8);
                        PrettyJsonFormatter.fieldOrderPreservingPrettyJsonFormatter().format(jsonProfileData,newWriter);
                        newWriter.close();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "There was a problem writing the launch profile,  is it write protected?", "Error", JOptionPane.ERROR_MESSAGE);
                        //return false;
                    }
                    
                    String username = getParameter("username");
                    
                    //TODO: somehow turn getParameter("session") into a password and use it to run a json request at https://authserver.mojang.com/authenticate
                    
                    String mineCmd = 
                            "javaw.exe -Xmx1G -Djava.library.path=\"" + 
                            versionTarget.getPath() + File.separator + VersionInfo.getModpackName() + "-natives" + 
                            "\" -cp \"" + target.getPath() + File.separator + "libraries" + File.separator + "*;" + versionTarget.getPath() + File.separator + VersionInfo.getModpackName() + 
                            ".jar\" net.minecraft.client.main.Main --username " + username + " --session token:" + 
                            /*Get the value of "accessToken" from json response*/"" + ":" + 
                            /*Get the value of "availableProfiles -> id" from json response */"" + "--version " + VersionInfo.getModpackName() +
                            " --gameDir \"" + target.getParentFile() + File.separator + ".technic" + File.separator + VersionInfo.getModpackName() + File.separator +
                            "\" --assetsDir \"" + target.getParentFile() + File.separator + "assets\" " + 
                            "--tweakClass cpw.mods.fml.common.launcher.FMLTweaker";
                    Process mineExec;
                    try {
                        mineExec = Runtime.getRuntime().exec(mineCmd);
                        mineExec.waitFor();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    
                }
            }); 
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is called from within the init() method to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        jLabel2.setText("Your profile has been updated! Go into regular Minecraft and run the \"" + VersionInfo.getModpackName() + "\" profile.");

        jLabel1.setText("Remember to use this to check for update frequently!");
        jLabel1.setToolTipText("(Note) Remove this in the future");

        jLabel3.setText("In the future, going into regular minecraft wont be necessary and this text window will instead be " + VersionInfo.getModpackName() + ".");
        jLabel3.setToolTipText("(Note) Remove this too.");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addContainerGap(212, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addContainerGap(250, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}

class aStub implements AppletStub {
    HashMap<String,String> params = new HashMap<String,String>();

    @Override
    public void appletResize(int width, int height) { }
    @Override
    public AppletContext getAppletContext() {
        return null;
    }

    @Override
    public URL getDocumentBase() {
        return null;
    }

    @Override
    public URL getCodeBase() {
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getParameter(String name) {
        return params.get(name);
    }

    public void addParameter(String name, String value) {
        params.put(name, value);
    }
}
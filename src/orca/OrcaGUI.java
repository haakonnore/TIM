package orca;

import java.awt.Cursor;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;

import orca.tools.Configuration;
import orca.tools.Contact;
import orca.tools.MessengerConfiguration;
import orca.engine.MessengerEngine;



/**
 *
 * @author  prajwalan
 */
public class OrcaGUI extends javax.swing.JFrame 
        implements ActionListener
{
	// make compiler happy :)
	private static final long serialVersionUID = 1464542712457413018L;
   
	
    //Set Look & Feel
    static
    {
        try 
        {
            javax.swing.UIManager.setLookAndFeel(UIManager.getLookAndFeel());
        } 
        catch(Exception e) {
                e.printStackTrace();
        }
    }
    
    /** Creates new form OrcaGUI */
    public OrcaGUI() 
    {
        initComponents();
        contactsListModel = new DefaultListModel();
        
        messengerConfiguration = new MessengerConfiguration();
        loadMessengerConfiguration();
                
        showLog = false;
        logGUI = new LogView(this);
        
        showConfiguration = false;
        configurationGUI = new ConfigurationView(this);
        
        showChat = false;
        chatGUI = new ChatView(this);
        
        signedIn = false;
        
        addContactGUI = null;
        
        enableAll(false);
        
        mnuLstContactPopUp = new JPopupMenu();
        mnuCallContact = new JMenuItem("Call");
        mnuEditContact = new JMenuItem("Edit Details");
        mnuDeleteContact = new JMenuItem("Delete Contact");
        mnuLstContactPopUp.add(mnuCallContact);
        mnuLstContactPopUp.add(mnuEditContact);
        mnuLstContactPopUp.add(mnuDeleteContact);
        
        lstContact.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent me) 
            {
                // if right mouse button clicked (or me.isPopupTrigger())
                if (SwingUtilities.isRightMouseButton(me)
                        && !lstContact.isSelectionEmpty()
                        && lstContact.locationToIndex(me.getPoint())
                        == lstContact.getSelectedIndex()) 
                {
                    mnuLstContactPopUp.show(lstContact, me.getX(), me.getY());
                }
            }
        }
        );

        mnuCallContact.addActionListener(this);
        mnuEditContact.addActionListener(this);                
        mnuDeleteContact.addActionListener(this);    
        
        for(int ndx = 0; ndx < messengerConfiguration.statusList.size(); ndx++)
        {
            cmbMyStatus.addItem(messengerConfiguration.statusList.get(ndx));
        }
    }
    
    public void loadMessengerConfiguration()
    {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        File file = new File("orca.config");
        if(file.exists() == true) 
        {
            try
            {
                MessengerConfiguration tempConfig = new MessengerConfiguration();
                fis = new FileInputStream(file);
                ois = new ObjectInputStream(fis);
                tempConfig = (MessengerConfiguration)ois.readObject();
                
                int ndx;
                for(ndx = 0; ndx<tempConfig.contacts.size(); ndx++)                
                    tempConfig.contacts.get(ndx).contactPresenceStatus = "";
                
                messengerConfiguration.set(tempConfig);

                contactsListModel.clear();
                for( int index = 0; index < messengerConfiguration.contacts.size(); index++)
                    contactsListModel.addElement(messengerConfiguration.contacts.get(index));
                lstContact.setModel(contactsListModel);
                
                for(ndx = 0; ndx < contactsListModel.size(); ndx++)
                {
                    messengerConfiguration.contacts.add((Contact)contactsListModel.get(ndx));
                }
                
            }
            catch(Exception e)
            {
                showErrorMessage("Could not load the configuration from file. Starting with Factory Settings.");
                messengerConfiguration.loadFactorySettings();
                e.printStackTrace();
            }
            try
            {
                if( ois != null )
                    ois.close();
                if ( fis != null )
                    fis.close();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

        }
        else 
        {
            showErrorMessage("Could not load the configuration from file. Starting with Factory Settings.");
            messengerConfiguration.loadFactorySettings();
        }
    }
    
    public void saveMessengerConfiguration(boolean saveConfigAlso)
    {
        try
        {
            File file = new File("orca.config");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            messengerConfiguration.contacts.clear();
            for(int ndx = 0; ndx < contactsListModel.size(); ndx++)
            {
                messengerConfiguration.contacts.add((Contact)contactsListModel.get(ndx));
            }
            
            if( saveConfigAlso )
            {
                messengerConfiguration.name = configurationGUI.getMyName();
                messengerConfiguration.id = configurationGUI.getUserID();
                messengerConfiguration.sipProxyIP = configurationGUI.getSIPProxyIP();
                messengerConfiguration.sipProxyPort = configurationGUI.getSIPProxyPort();
                messengerConfiguration.myPort = configurationGUI.getMyPort();

                messengerConfiguration.enablePresence = configurationGUI.getEnablePresence();
                messengerConfiguration.showMyPresence = configurationGUI.getShowMyPresence();
                messengerConfiguration.sendLocationWithPresence = 
                        configurationGUI.getSendLocationWithPresence();
                messengerConfiguration.receiveLocationWithPresence = 
                        configurationGUI.getReceiveLocationWithPresence();
                messengerConfiguration.locationAdInterval = 
                        configurationGUI.getLocationAdInterval();

                messengerConfiguration.locationURL = configurationGUI.getLocationURL();
                
                if( signedIn )
                {
                    cmbMyStatus.setEnabled( messengerConfiguration.enablePresence && 
                            messengerConfiguration.showMyPresence);
                    btnAddNewStatus.setEnabled( messengerConfiguration.enablePresence && 
                            messengerConfiguration.showMyPresence);
                    btnDeleteStatus.setEnabled( messengerConfiguration.enablePresence && 
                            messengerConfiguration.showMyPresence);
                }

            }
            
            oos.writeObject(messengerConfiguration);
            oos.flush();
            oos.close();   
            
            updateEngine();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            
            java.awt.EventQueue.invokeLater(new Runnable() 
            {
                public void run() 
                {
                    JOptionPane.showMessageDialog(null, 
                            "Could not save file.\n\nPlease try saving into a different location or with different name.",
                            "Orca", JOptionPane.ERROR_MESSAGE);
                }
            });
            
        }
    }
    
    public void actionPerformed(ActionEvent ae) 
    {
        if (ae.getSource() == mnuCallContact) 
        {
            
            Contact selectedContact = (Contact)lstContact.getSelectedValue();
            if( selectedContact != null)
            {
                String uri = "";
                uri = "sip:" + selectedContact.contactID + 
                        "@" + selectedContact.contactSIPProxyIP +
                        ":" + selectedContact.contactSIPProxyPort;
                engine.userInput(0,uri,null);
                enableDisableWhileCalling(false);
            }
            
        }
        else if (ae.getSource() == mnuEditContact) 
        {
            if( addContactGUI != null )
           {
               closeContactView();
           }
           addContactGUI = new AddContactView(this, 1);
           addContactGUI.setContactData((Contact)lstContact.getSelectedValue(),
                   lstContact.getSelectedIndex());
           addContactGUI.setVisible(true);
        }
        else
        {
            if( JOptionPane.showConfirmDialog(this, 
                    "Are you sure, you want to delete this contact?", 
                    "Orca", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION )
            {
                contactsListModel.remove(lstContact.getSelectedIndex());
                lstContact.setModel(contactsListModel);
            }
        }
    }    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        toolbarStandard = new javax.swing.JToolBar();
        btnOnOff = new javax.swing.JToggleButton();
        btnLog = new javax.swing.JToggleButton();
        btnConfiguration = new javax.swing.JToggleButton();
        btnAddContact = new javax.swing.JButton();
        cmbInput = new javax.swing.JComboBox();
        btnDial = new javax.swing.JButton();
        btnHangUp = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstContact = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtStatus = new javax.swing.JTextArea();
        cmbMyStatus = new javax.swing.JComboBox();
        btnAddNewStatus = new javax.swing.JButton();
        lblStatusBar = new javax.swing.JLabel();
        btnTestLocation = new javax.swing.JButton();
        btnHospital = new javax.swing.JButton();
        btnGeneralSOS = new javax.swing.JButton();
        btnDeleteStatus = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuFileLoadContactList = new javax.swing.JMenuItem();
        menuFileSaveContactList = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        menuFileExit = new javax.swing.JMenuItem();
        menuView = new javax.swing.JMenu();
        menuViewToolbar = new javax.swing.JCheckBoxMenuItem();
        menuViewLogView = new javax.swing.JCheckBoxMenuItem();
        menuTools = new javax.swing.JMenu();
        menuToolsConfiguration = new javax.swing.JMenuItem();
        menuHelp = new javax.swing.JMenu();
        menuHelpContents = new javax.swing.JMenuItem();
        menuHelpAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Orca");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        toolbarStandard.setFloatable(false);
        toolbarStandard.setRollover(true);

        btnOnOff.setText("On/Off");
        btnOnOff.setToolTipText("Log");
        btnOnOff.setFocusable(false);
        btnOnOff.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOnOff.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOnOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOnOffActionPerformed(evt);
            }
        });
        toolbarStandard.add(btnOnOff);

        btnLog.setText("Log");
        btnLog.setToolTipText("Log");
        btnLog.setFocusable(false);
        btnLog.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnLog.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogActionPerformed(evt);
            }
        });
        toolbarStandard.add(btnLog);

        btnConfiguration.setText("Configuration");
        btnConfiguration.setToolTipText("Log");
        btnConfiguration.setFocusable(false);
        btnConfiguration.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnConfiguration.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfigurationActionPerformed(evt);
            }
        });
        toolbarStandard.add(btnConfiguration);

        btnAddContact.setText("Add Contact");
        btnAddContact.setToolTipText("Add Contact");
        btnAddContact.setFocusable(false);
        btnAddContact.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAddContact.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAddContact.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddContactActionPerformed(evt);
            }
        });
        toolbarStandard.add(btnAddContact);

        cmbInput.setEditable(true);

        btnDial.setText("   Dial  ");
        btnDial.setToolTipText("Dial");
        btnDial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDialActionPerformed(evt);
            }
        });

        btnHangUp.setText("HangUp");
        btnHangUp.setDefaultCapable(false);
        btnHangUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHangUpActionPerformed(evt);
            }
        });

        lstContact.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(lstContact);

        txtStatus.setColumns(20);
        txtStatus.setEditable(false);
        txtStatus.setFont(new java.awt.Font("Arial", 0, 10));
        txtStatus.setLineWrap(true);
        txtStatus.setRows(5);
        jScrollPane2.setViewportView(txtStatus);

        cmbMyStatus.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Online", "Away", "Busy", "Be Right Back", "On the Phone", "Invisible" }));
        cmbMyStatus.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                cmbMyStatusItemStateChanged(evt);
            }
        });
        cmbMyStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbMyStatusActionPerformed(evt);
            }
        });
        cmbMyStatus.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                cmbMyStatusPropertyChange(evt);
            }
        });

        btnAddNewStatus.setText("New");
        btnAddNewStatus.setToolTipText("Add New Status");
        btnAddNewStatus.setDefaultCapable(false);
        btnAddNewStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddNewStatusActionPerformed(evt);
            }
        });

        btnTestLocation.setText("Test Location");
        btnTestLocation.setDefaultCapable(false);
        btnTestLocation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTestLocationActionPerformed(evt);
            }
        });

        btnHospital.setText("Hospital");
        btnHospital.setDefaultCapable(false);
        btnHospital.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHospitalActionPerformed(evt);
            }
        });

        btnGeneralSOS.setText("General SOS");
        btnGeneralSOS.setDefaultCapable(false);
        btnGeneralSOS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeneralSOSActionPerformed(evt);
            }
        });

        btnDeleteStatus.setText("Delete");
        btnDeleteStatus.setToolTipText("Delete Existing Status");
        btnDeleteStatus.setDefaultCapable(false);
        btnDeleteStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteStatusActionPerformed(evt);
            }
        });

        menuFile.setText("File");

        menuFileLoadContactList.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        menuFileLoadContactList.setText("Load Contact List");
        menuFileLoadContactList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileLoadContactListActionPerformed(evt);
            }
        });
        menuFile.add(menuFileLoadContactList);

        menuFileSaveContactList.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        menuFileSaveContactList.setText("Save Contact List");
        menuFileSaveContactList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileSaveContactListActionPerformed(evt);
            }
        });
        menuFile.add(menuFileSaveContactList);
        menuFile.add(jSeparator1);

        menuFileExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        menuFileExit.setText("Exit");
        menuFileExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileExitActionPerformed(evt);
            }
        });
        menuFile.add(menuFileExit);

        jMenuBar1.add(menuFile);

        menuView.setText("View");

        menuViewToolbar.setSelected(true);
        menuViewToolbar.setText("Toolbar");
        menuViewToolbar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuViewToolbarActionPerformed(evt);
            }
        });
        menuView.add(menuViewToolbar);

        menuViewLogView.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        menuViewLogView.setSelected(true);
        menuViewLogView.setText("Log View");
        menuViewLogView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuViewLogViewActionPerformed(evt);
            }
        });
        menuView.add(menuViewLogView);

        jMenuBar1.add(menuView);

        menuTools.setText("Tools");

        menuToolsConfiguration.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        menuToolsConfiguration.setText("Configuration");
        menuToolsConfiguration.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuToolsConfigurationActionPerformed(evt);
            }
        });
        menuTools.add(menuToolsConfiguration);

        jMenuBar1.add(menuTools);

        menuHelp.setText("Help");

        menuHelpContents.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menuHelpContents.setText("Contents");
        menuHelpContents.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHelpContentsActionPerformed(evt);
            }
        });
        menuHelp.add(menuHelpContents);

        menuHelpAbout.setText("About");
        menuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHelpAboutActionPerformed(evt);
            }
        });
        menuHelp.add(menuHelpAbout);

        jMenuBar1.add(menuHelp);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(toolbarStandard, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnTestLocation)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnHospital)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnGeneralSOS)
                .addContainerGap(127, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(cmbMyStatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbInput, 0, 192, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnAddNewStatus, 0, 0, Short.MAX_VALUE)
                            .addComponent(btnDial, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnDeleteStatus)
                            .addComponent(btnHangUp))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnAddNewStatus, btnDeleteStatus, btnHangUp});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolbarStandard, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTestLocation)
                    .addComponent(btnHospital)
                    .addComponent(btnGeneralSOS))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbMyStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddNewStatus)
                    .addComponent(btnDeleteStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnDial)
                    .addComponent(btnHangUp))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE))
        );

        btnGeneralSOS.setVisible(false);
        btnHospital.setVisible(false);
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void menuFileLoadContactListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadContactListActionPerformed
        doLoadContactList();
    }//GEN-LAST:event_menuFileLoadContactListActionPerformed

    private void menuFileSaveContactListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSaveContactListActionPerformed
        doSaveContactList();
    }//GEN-LAST:event_menuFileSaveContactListActionPerformed

    private void menuViewToolbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuViewToolbarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuViewToolbarActionPerformed

    private void menuViewLogViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuViewLogViewActionPerformed
        toggleLog();
    }//GEN-LAST:event_menuViewLogViewActionPerformed

    private void menuToolsConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuToolsConfigurationActionPerformed
        toggleConfiguration();
    }//GEN-LAST:event_menuToolsConfigurationActionPerformed

    private void menuHelpContentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpContentsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuHelpContentsActionPerformed

    private void menuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpAboutActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_menuHelpAboutActionPerformed

    private void btnLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogActionPerformed
        toggleLog();
    }//GEN-LAST:event_btnLogActionPerformed

    private void btnAddContactActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddContactActionPerformed
        doAddContact();
    }//GEN-LAST:event_btnAddContactActionPerformed

    private void btnDialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDialActionPerformed
        doDial();
    }//GEN-LAST:event_btnDialActionPerformed

    private void btnHangUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHangUpActionPerformed
        doHangUp();
    }//GEN-LAST:event_btnHangUpActionPerformed

    private void btnAddNewStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddNewStatusActionPerformed
        doAddNewStatus();
}//GEN-LAST:event_btnAddNewStatusActionPerformed

    private void cmbMyStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbMyStatusActionPerformed
        statusChanged(null);//, null);
    }//GEN-LAST:event_cmbMyStatusActionPerformed

    private void btnOnOffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOnOffActionPerformed
        toggleOnOff();
}//GEN-LAST:event_btnOnOffActionPerformed

    private void btnConfigurationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfigurationActionPerformed
        toggleConfiguration();
}//GEN-LAST:event_btnConfigurationActionPerformed

    private void cmbMyStatusItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cmbMyStatusItemStateChanged
        
    }//GEN-LAST:event_cmbMyStatusItemStateChanged

    private void cmbMyStatusPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_cmbMyStatusPropertyChange
        
    }//GEN-LAST:event_cmbMyStatusPropertyChange

    private void btnTestLocationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFireActionPerformed
        
        engine.testLocation();
    }//GEN-LAST:event_btnFireActionPerformed

    private void btnHospitalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHospitalActionPerformed
        
    }//GEN-LAST:event_btnHospitalActionPerformed

    private void btnGeneralSOSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeneralSOSActionPerformed
        
    }//GEN-LAST:event_btnGeneralSOSActionPerformed

    private void menuFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileExitActionPerformed
        
        if( signedIn == true && btnDial.isEnabled()== false)
        {
            doHangUp();
        }
        System.exit(0);
    }//GEN-LAST:event_menuFileExitActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        saveMessengerConfiguration(false);
        if( signedIn == true && btnDial.isEnabled()== false)
        {
            doHangUp();
        }
        turnOff();
        System.exit(0);

    }//GEN-LAST:event_formWindowClosing

    private void btnDeleteStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteStatusActionPerformed
        // TODO add your handling code here:
        doDeleteStatus();
}//GEN-LAST:event_btnDeleteStatusActionPerformed
    
    public void statusChanged(String status)
    {
        if( messengerConfiguration == null )
            return;
        if( !messengerConfiguration.enablePresence )
            return;
        if( !messengerConfiguration.showMyPresence )
            return;
        
        if( status == null )
            status = (String)cmbMyStatus.getSelectedItem();
        
      
            System.out.println("My Status : " + status);
            if( engine != null )
            {
                engine.sendPublish(status, status);
            }
  
    }
    
    public void updateListWithStatus(String entity, String status)
    {
        String userid = "";
        String proxyip = "";
        
        int loc1, loc2;
        loc1 = entity.indexOf(":");
        loc2 = entity.indexOf("@");
        userid = entity.substring(loc1 + 1, loc2 );
        proxyip = entity.substring(loc2 + 1);
        
        for(int ndx = 0; ndx < contactsListModel.size(); ndx++)
        {
            Contact contact = (Contact)contactsListModel.get(ndx);
            if( contact.contactID.equals(userid)  && 
                    contact.contactSIPProxyIP.equals(proxyip))
            {
                if( status.contains("="))
                {
                    status = status.replace("=", " <");
                    status = status + ">";
                }
                
                contact.contactPresenceStatus = status;
                contactsListModel.setElementAt(contact, ndx);
                lstContact.setModel(contactsListModel);
                return;
            }
            
        }
    }
    
    public void doAddNewStatus()
    {
        strNewStatus = "";
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            public void run() 
            {
                strNewStatus = JOptionPane.showInputDialog(null, 
                        "Enter a new status: ", "Orca",
                        JOptionPane.PLAIN_MESSAGE);

                if( strNewStatus.contains("="))
                {
                    showErrorMessage("\"" + strNewStatus + "\"  contains an illegal character \'§\'.");
                    return;
                }
                
                if( strNewStatus.equalsIgnoreCase("Online") ||
                        strNewStatus.equalsIgnoreCase("Offline") ||
                        strNewStatus.equalsIgnoreCase("Away") ||
                        strNewStatus.equalsIgnoreCase("Busy") ||
                        strNewStatus.equalsIgnoreCase("Invisible") ||
                        strNewStatus.equalsIgnoreCase("Be Right Back") ||
                        strNewStatus.equalsIgnoreCase("On The Phone")
                        )
                {
                    showErrorMessage("\"" + strNewStatus + "\" is a keyword and hence invalid custom status.");
                    return;
                }
                
                for(int ndx=0; ndx < cmbMyStatus.getItemCount(); ndx++)
                {
                    if( ((String)cmbMyStatus.getItemAt(ndx)).equals(strNewStatus) )
                    {
                        showErrorMessage("\"" + strNewStatus + "\"  already exists.");
                        return;
                    }
                }
                
                if( strNewStatus.replaceAll(" ", "").length() > 0 )
                {
                    cmbMyStatus.addItem(strNewStatus);
                    messengerConfiguration.statusList.add(strNewStatus);
                }
            }
        });
       
    }
    
    public void doDeleteStatus()
    {
        strNewStatus = "";
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            public void run() 
            {
                strNewStatus = JOptionPane.showInputDialog(null, 
                        "Enter status to delete: ", "Orca",
                        JOptionPane.PLAIN_MESSAGE);
                
                if( strNewStatus.equalsIgnoreCase("Online") ||
                        strNewStatus.equalsIgnoreCase("Offline") ||
                        strNewStatus.equalsIgnoreCase("Away") ||
                        strNewStatus.equalsIgnoreCase("Busy") ||
                        strNewStatus.equalsIgnoreCase("Invisible") ||
                        strNewStatus.equalsIgnoreCase("Be Right Back") ||
                        strNewStatus.equalsIgnoreCase("On The Phone")
                        )
                {
                    showErrorMessage("\"" + strNewStatus + "\" is a keyword and hence cannot delete.");
                    return;
                }
                
                if( strNewStatus.equals( (String)cmbMyStatus.getSelectedItem()))
                {
                    showErrorMessage("Cannot delete current status.");
                    return;
                }
                
                for(int ndx=0; ndx < cmbMyStatus.getItemCount(); ndx++)
                {
                    if( ((String)cmbMyStatus.getItemAt(ndx)).equals(strNewStatus) )
                    {
                        cmbMyStatus.removeItem(strNewStatus);
                        messengerConfiguration.statusList.remove(strNewStatus);                        
                        return;
                    }
                }
                showErrorMessage(strNewStatus + " not found.");
            }
        });
    }
    
    public void sendSubscribe(Contact contact)
    {
        if( messengerConfiguration == null )
            return;
        if( !messengerConfiguration.enablePresence )
            return;
        if( engine == null)
            return;
        
        if( contact == null)
        {
            for(int ndx = 0; ndx < contactsListModel.size(); ndx++)
            {
                if( ((Contact)contactsListModel.get(ndx)).enablePresence )
                {
                    engine.sendSubscribe(
                    ((Contact)contactsListModel.get(ndx)).contactID + "@" +
                            ((Contact)contactsListModel.get(ndx)).contactSIPProxyIP
                            );
                }
            }
        }
        else
        {
            if( contact.enablePresence )
                engine.sendSubscribe(
                    contact.contactID + "@" +contact.contactSIPProxyIP);
        }
    }
    
    public void doLoadContactList()
    {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result= fileChooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) 
        {
            File file = new File(fileChooser.getSelectedFile().getPath());
            if(file.exists() == true) 
            {
                try
                {
                    FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    contactsListModel = null;  
                    contactsListModel = new DefaultListModel();
                    contactsListModel = (DefaultListModel)ois.readObject();
                    lstContact.setModel(contactsListModel);
                    
                    ois.close();
                    
                    sendSubscribe(null);
                }
                catch(Exception e)
                {
                    JOptionPane.showMessageDialog(this, 
                        "Oops! Could not open/read the file!",
                        "Orca", JOptionPane.ERROR_MESSAGE);
                }
            }
            else 
            {
                JOptionPane.showMessageDialog(this, 
                        "Oops! That file doesnt exists!",
                        "Orca", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    public void doSaveContactList()
    {
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if( contactsListModel != null )
        {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("mycontacts"));
            int returnVal = chooser.showSaveDialog(this);
            if(returnVal != JFileChooser.APPROVE_OPTION)
                return;
            
            try
            {
                FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile());
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(contactsListModel);
                oos.flush();
                oos.close();        
            }
            catch(Exception e)
            {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                        "Could not save file.\n\nPlease try saving into a different location or with different name.",
                        "Orca", JOptionPane.ERROR_MESSAGE);
            }
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    public void setDialCaption()
    {
        if( engine == null )
            return;
        
        if( engine.status == MessengerEngine.RINGING )
        {
            btnDial.setText("Accept");
            btnDial.setToolTipText("Accept the call");
        }
        else
        {
            btnDial.setText("   Dial  ");
            btnDial.setToolTipText("Dial");
        }
    }
    
   public void log(String logString)
   {
        logGUI.setLog(logString);
   }
   
   public void toggleLog()
   {
       showLog = !showLog;
       logGUI.setVisible(showLog);
       btnLog.setSelected(showLog);
   }
   
   public void toggleChat()
   {
       showChat = !showChat;
       chatGUI.setVisible(showChat);	   
   }

   public void setStatusTextArea(String strStatus)
   {
       txtStatus.append(strStatus);
       txtStatus.append("\n");
   }
   
   public void showStatus(String strStatus)
   {
       lblStatusBar.setText(strStatus);
   }
   
   public void toggleConfiguration()
   {
       showConfiguration = !showConfiguration;
       if( showConfiguration )
           configurationGUI.loadMessengerConfiguration();
       
       configurationGUI.setVisible(showConfiguration);
       btnConfiguration.setSelected(showConfiguration);
   }
   
   private Configuration readConfiguration()
   {
        Configuration config=new Configuration();

        config.sipPort=messengerConfiguration.myPort;
        config.name=messengerConfiguration.name;
        config.userID=messengerConfiguration.id;

        return config;
   }
   
   public void toggleOnOff()
   {
        if( signedIn == false ) //if it is Off then turn it On
        {
            //On action
            try
            {
                Configuration config = readConfiguration();
                engine = new MessengerEngine(config, this, messengerConfiguration.getSIPProxy());//configurationGUI.getSIPProxy());
                signedIn = true;
                this.setTitle("Orca - " + messengerConfiguration.name);//configurationGUI.getMyName());
                enableAll(true);
                
                statusChanged("Online");//, null);
                sendSubscribe(null);
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
                startErrorMessage();
            }
        }
        else
        {
            //Off action
            turnOff();
        }
   }
   
   public void turnOff()
   {
        if( engine != null )
        {
            statusChanged("Offline");//, null);
            try
            {
                Thread.sleep(500);
            }
            catch(InterruptedException ie)
            {}
            engine.setOff();
            engine=null;
        }
        showStatus("");        
        signedIn = false;
        enableAll(false);
        this.setTitle("Orca");
   }
   
   public void startErrorMessage()
   {
       turnOff();
        btnOnOff.setSelected(false);
        JOptionPane.showMessageDialog(this, "Cannot start the Messenger. Try changing \"My Port\"",
                "Orca", JOptionPane.ERROR_MESSAGE);
   }
   
   public void enableAll(boolean enable)
   {
       
        cmbMyStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnAddNewStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnDeleteStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);

       cmbInput.setEnabled(enable);
       btnDial.setEnabled(enable);
       btnHangUp.setEnabled(false);
       lstContact.setEnabled(enable);
       btnTestLocation.setEnabled(enable);
       btnHospital.setEnabled(enable);
       btnGeneralSOS.setEnabled(enable);
   }
   
   public void updateEngine()
   {
       if( !signedIn )
           return;
       if( engine == null )
           return;
       
       Configuration config = readConfiguration();
       engine.updateConfiguration(config);
       //engine.updateLocationTimer();
   }
   
   public void doDial()
   {
        try 
        {
            String uri="";
            if ( engine.status == MessengerEngine.RINGING )
            {
                engine.userInput(0,uri,null);
                enableDisableWhileCalling(false);
                return;
            }
            
            String selectedContact = (String)cmbInput.getSelectedItem();
            if( selectedContact.length() > 4 )
            {
                String tempstr = selectedContact.substring(0, 4);
                if( selectedContact.length()>0 &&
                        tempstr.equalsIgnoreCase("sip:")==false)
                {
                    uri = "sip:" + selectedContact + 
                            "@" + messengerConfiguration.getSIPProxy();//configurationGUI.getSIPProxy();
                    engine.userInput(0,uri,null);
                }
                else
                    engine.userInput(0,selectedContact,null);
            }
            else
            {
                    uri = "sip:" + selectedContact + 
                            "@" + messengerConfiguration.getSIPProxy();//configurationGUI.getSIPProxy();
                    engine.userInput(0,uri,null);
            }
            
            
            boolean found = false;
            for( int ndx=0; ndx < cmbInput.getItemCount(); ndx++ )
            {
                if( cmbInput.getItemAt(ndx).equals(cmbInput.getSelectedItem()))
                {
                    found = true;
                    break;
                }
            }
            if( !found )
                cmbInput.addItem((String)cmbInput.getSelectedItem());
            
            enableDisableWhileCalling(false);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            enableDisableWhileCalling(true);
            JOptionPane.showMessageDialog(this, "Could not make the call.",
                "Orca", JOptionPane.ERROR_MESSAGE);
            
        }
   }

   public void dialSOS(String destination)
   {
        
   }
   
   public void doHangUp()
   {
       engine.userInput(1,null,null);
       enableDisableWhileCalling(true);
   }
   
   public void enableDisableWhileCalling(boolean enable)
   {
        cmbMyStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnAddNewStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnDeleteStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);

       cmbInput.setEnabled(enable);
       btnDial.setEnabled(enable);
       btnHangUp.setEnabled(!enable);
       lstContact.setEnabled(enable);
       btnTestLocation.setEnabled(enable);
       btnHospital.setEnabled(enable);
       btnGeneralSOS.setEnabled(enable);
       
       btnAddContact.setEnabled(enable);
       btnConfiguration.setEnabled(enable);
       menuToolsConfiguration.setEnabled(enable);
   }
   
   public void enableDisableWhileRinging()
   {
       boolean enable = false;
        cmbMyStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnAddNewStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);
        btnDeleteStatus.setEnabled( messengerConfiguration.enablePresence && 
                messengerConfiguration.showMyPresence && enable);

       cmbInput.setEnabled(enable);
       btnDial.setEnabled(!enable);
       btnHangUp.setEnabled(!enable);
       lstContact.setEnabled(enable);
       btnTestLocation.setEnabled(enable);
       btnHospital.setEnabled(enable);
       btnGeneralSOS.setEnabled(enable);
       
       btnAddContact.setEnabled(enable);
       btnConfiguration.setEnabled(enable);
       menuToolsConfiguration.setEnabled(enable);
   }
   
   public void toggleChatView(boolean show)
   {
       if( show )
           chatGUI.clearOutputText();
       chatGUI.setVisible(show);
   }
   
   public void displayMessage(String message)
   {
       chatGUI.setOutputMessage(message);
   }
   
   public void sendMessage(String message)
   {
       engine.userInput(2,
               messengerConfiguration.name,//configurationGUI.getMyName(),
               message);
   }
   
   public ConfigurationView getConfigurationView()
   {
       return configurationGUI;
   }
   
   public void doAddContact()
   {
       if( addContactGUI != null )
       {
           closeContactView();
       }
       addContactGUI = new AddContactView(this, 0);
       addContactGUI.setSIPProxyIP(messengerConfiguration.sipProxyIP);//configurationGUI.getSIPProxyIP());
       addContactGUI.setSIPProxyPort(messengerConfiguration.sipProxyPort);//configurationGUI.getSIPProxyPort());
       addContactGUI.setVisible(true);
   }
   
   public void SaveContact(Contact contact)
   {
       contactsListModel.addElement(contact);
       lstContact.setModel(contactsListModel);
       
       
       sendSubscribe(contact);
   }
   
   public void UpdateContact(Contact contact, int index)
   {
       contactsListModel.set(index, contact);
       lstContact.setModel(contactsListModel);
       sendSubscribe(contact);
   }

   public void closeContactView()
   {
       if( addContactGUI != null)
       {
           addContactGUI.setVisible(false);
           addContactGUI.dispose();
           addContactGUI = null;
       }
   }
   
   public void showErrorMessage(String errMessage)
   {
       final String msg = errMessage;
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            public void run() 
            {
                JOptionPane.showMessageDialog(null, msg, "Orca",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
   }
   
   public void showCallRejectedMessage()
   {
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            public void run() 
            {
                JOptionPane.showMessageDialog(null, "Call Rejected or Destination too busy", "Orca",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
   }
   
   public String strNewStatus = "";//Temporary variable
   
   public MessengerConfiguration messengerConfiguration;
   private DefaultListModel contactsListModel;
   
   private boolean showLog;
   private LogView logGUI;
   private boolean showConfiguration;
   private ConfigurationView configurationGUI;
   private boolean showChat;
   private ChatView chatGUI;
   
   private boolean signedIn;
    
   private MessengerEngine engine;
   
   private AddContactView addContactGUI;
    private JPopupMenu mnuLstContactPopUp;
    private JMenuItem mnuCallContact, mnuEditContact, mnuDeleteContact;
   
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddContact;
    private javax.swing.JButton btnAddNewStatus;
    private javax.swing.JToggleButton btnConfiguration;
    private javax.swing.JButton btnDeleteStatus;
    private javax.swing.JButton btnDial;
    private javax.swing.JButton btnTestLocation;
    private javax.swing.JButton btnGeneralSOS;
    private javax.swing.JButton btnHangUp;
    private javax.swing.JButton btnHospital;
    private javax.swing.JToggleButton btnLog;
    private javax.swing.JToggleButton btnOnOff;
    private javax.swing.JComboBox cmbInput;
    private javax.swing.JComboBox cmbMyStatus;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblStatusBar;
    private javax.swing.JList lstContact;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenuItem menuFileExit;
    private javax.swing.JMenuItem menuFileLoadContactList;
    private javax.swing.JMenuItem menuFileSaveContactList;
    private javax.swing.JMenu menuHelp;
    private javax.swing.JMenuItem menuHelpAbout;
    private javax.swing.JMenuItem menuHelpContents;
    private javax.swing.JMenu menuTools;
    private javax.swing.JMenuItem menuToolsConfiguration;
    private javax.swing.JMenu menuView;
    private javax.swing.JCheckBoxMenuItem menuViewLogView;
    private javax.swing.JCheckBoxMenuItem menuViewToolbar;
    private javax.swing.JToolBar toolbarStandard;
    private javax.swing.JTextArea txtStatus;
    // End of variables declaration//GEN-END:variables
    
}

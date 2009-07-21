/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyClientFileQuad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.vis.otfvis.opengl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.OTFQueryControlBar;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.PreferencesDialog;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gui.OTFFileSettingsSaver;
import org.matsim.vis.otfvis.opengl.gui.OTFTimeLine;
import org.matsim.vis.otfvis.opengl.layer.ColoredStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

class OTFFrame extends JFrame {
	   public OTFFrame(String string) {
		super(string);
	}

	@Override
	protected void processWindowEvent(WindowEvent e) {

	        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	        	OnTheFlyClientFileQuad.endProgram(0);
	        } else {
		        super.processWindowEvent(e);
	        }
	    }
}

/**
 * This file starts OTFVis using a .mvi file.
 * @author dstrippgen
 */
public class OnTheFlyClientFileQuad extends Thread {
	private OTFQueryControlBar queryControl = null;

	
	public static void endProgram(int code) {
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if(config.isModified()) {
			final JDialog dialog = new JDialog((JFrame)null, "Preferences are unsaved and modified...", true);
			final JOptionPane optionPane = new JOptionPane(
				    "There are potenially unsaved changes in Preferences.\nQuit anyway?",
				    JOptionPane.QUESTION_MESSAGE,
				    JOptionPane.YES_NO_OPTION);
			dialog.setContentPane(optionPane);
			dialog.setDefaultCloseOperation(
				    JDialog.DO_NOTHING_ON_CLOSE);
				dialog.addWindowListener(new WindowAdapter() {
				    @Override
						public void windowClosing(WindowEvent we) {
				        
				    }
				});
				optionPane.addPropertyChangeListener(
				    new PropertyChangeListener() {
				        public void propertyChange(PropertyChangeEvent e) {
				            String prop = e.getPropertyName();

				            if (dialog.isVisible() 
				             && (e.getSource() == optionPane)
				             && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
				                //If you were going to check something
				                //before closing the window, you'd do
				                //it here.
				                dialog.setVisible(false);
				            }
				        }
				    });
			dialog.pack();
			dialog.setVisible(true);
			int value = ((Integer)optionPane.getValue()).intValue();
			if (value == JOptionPane.NO_OPTION) {
				return; // do not quit
			}

		}
		System.exit(code);
	}
	
	protected OTFHostControlBar hostControl = null;

	protected String filename;
	private boolean splitLayout = true;
	public OTFQueryControlBar getQueryControl() {
		return queryControl;
	}

	public OTFHostControlBar getHostControl() {
		return hostControl;
	}

	public boolean isSplitLayout() {
		return splitLayout;
	}

	public JSplitPane getPane() {
		return pane;
	}

	public OTFDrawer getLeftComp() {
		return leftComp;
	}

	public OTFDrawer getRightComp() {
		return rightComp;
	}

	protected JSplitPane pane = null;
	protected OTFDrawer leftComp = null;
	protected OTFDrawer rightComp = null;

	protected OTFConnectionManager connect = new OTFConnectionManager();

	//NetworkLayer network = new NetworkLayer();


	public OTFDrawer getLeftDrawerComponent(JFrame frame) throws RemoteException {
		OTFConnectionManager connectL = this.connect.clone();
		connectL.remove(OTFLinkAgentsHandler.class);

		connectL.add(OTFLinkAgentsHandler.class, ColoredStaticNetLayer.QuadDrawer.class);
		connectL.add(ColoredStaticNetLayer.QuadDrawer.class, ColoredStaticNetLayer.class);
		OTFClientQuad clientQ = this.hostControl.createNewView(null, connectL);

		OTFDrawer drawer = new OTFOGLDrawer(frame, clientQ);
		// DS TODO Repair painting of this colored net drawer.isActiveNet = true;
		return drawer;
	}

	public OTFDrawer getRightDrawerComponent(JFrame frame) throws RemoteException {
		OTFConnectionManager connectR = this.connect.clone();
		connectR.remove(OTFLinkAgentsHandler.class);

		connectR.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connectR.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connectR.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
		connectR.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		connectR.add(OGLAgentPointLayer.AgentPointDrawer.class, OGLAgentPointLayer.class);


		OTFClientQuad clientQ2 = this.hostControl.createNewView(null, connectR);

		OTFDrawer drawer2 = new OTFOGLDrawer(frame, clientQ2);

		return drawer2;
	}

	protected String getURL() {
		System.out.println("Loading file " + this.filename + " ....");
		return "file:" + this.filename;
	}
	
	protected OTFFileSettingsSaver getFileSaver() {
		return new OTFFileSettingsSaver(this.filename);
	}

	
	protected JFrame prepareRun() {
		System.setProperty("javax.net.ssl.keyStore", "input/keystore");
		System.setProperty("javax.net.ssl.keyStorePassword", "vspVSP");
		System.setProperty("javax.net.ssl.trustStore", "input/truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "vspVSP");
		
		boolean isMac = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
		if (isMac) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}

		JFrame frame = new OTFFrame("MATSim OTFVis");
	    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ); 

		if (isMac) {
			frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		pane.setContinuousLayout(true);
		pane.setOneTouchExpandable(true);
		frame.getContentPane().add(pane);
		this.pane = pane;

		//Make sure menus appear above JOGL Layer
		JPopupMenu.setDefaultLightWeightPopupEnabled(false); 
		
		if (Gbl.getConfig() == null) Gbl.createConfig(null);
		OTFFileSettingsSaver saver = getFileSaver();
		
		OTFVisConfig visconf = (OTFVisConfig) Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		if (visconf == null) {
			visconf = saver.openAndReadConfig();
		} else {
			System.out.println("OTFVisConfig already defined, cant read settings from file");
		}

		try {

			
			this.hostControl = new OTFHostControlBar(getURL());
			hostControl.frame = frame;

			frame.getContentPane().add(this.hostControl, BorderLayout.NORTH);
			PreferencesDialog.buildMenu(frame, visconf, this.hostControl, saver);
			
			
			if(!hostControl.isLiveHost()) {
				frame.getContentPane().add(new OTFTimeLine("time", hostControl), BorderLayout.SOUTH);
			} else  {
				queryControl = new OTFQueryControlBar("test", hostControl, visconf);
				frame.getContentPane().add(queryControl, BorderLayout.SOUTH);
			}

			// Maybe later: connect.add(QueueLink.class, OTFDefaultLinkHandler.Writer.class);
			// connect.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);

			if (this.splitLayout) {
				OTFDrawer drawer = getLeftDrawerComponent(frame);
				this.leftComp = drawer;
				drawer.invalidate((int)hostControl.getTime());
				this.hostControl.addHandler("test", drawer);
				pane.setLeftComponent(drawer.getComponent());
				if(queryControl != null) ((OTFOGLDrawer)drawer).setQueryHandler(queryControl);
			}

			OTFDrawer drawer2 = getRightDrawerComponent(frame);
			this.rightComp = drawer2;
			pane.setRightComponent(drawer2.getComponent());
			this.hostControl.addHandler("test2", drawer2);
			drawer2.invalidate((int)hostControl.getTime());
			if(queryControl != null) ((OTFOGLDrawer)drawer2).setQueryHandler(queryControl);

			System.out.println("Finished init");
			pane.setDividerLocation(0.5);
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setSize(screenSize.width/2,screenSize.height/2);
			frame.setVisible(true);

			//InfoText.showText("Loaded...");



		}catch (RemoteException e) {
			e.printStackTrace();
			throw new RuntimeException();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return frame;
	}
	
	@Override
	public void run() {
		prepareRun();
		hostControl.finishedInitialisition();
	}

	public static void main( String[] args) {

		String filename;

		if (args.length == 1) {
			filename = args[0];
		} else {
//			filename = "../MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi.gz";
			filename = "output/OTFQuadfile10p.mvi";
//			filename = "../../tmp/1000.events.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/output/OTFQuadfileNoParking10p_wip.mvi";
//			filename = "/TU Berlin/workspace/MatsimJ/otfvisSwitzerland10p.mvi";
			filename = "testCUDA10p.mvi";
		}

		
		OnTheFlyClientFileQuad client = new OnTheFlyClientFileQuad(filename);
		client.run();
	}

	public OnTheFlyClientFileQuad( String filename) {
		super();
		this.filename = filename;

		this.connect.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
		this.connect.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		this.connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		this.connect.add(OTFAgentsListHandler.class,  OGLAgentPointLayer.AgentPointDrawer.class);
		this.connect.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		this.connect.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		this.connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		splitLayout = false;
	}

	public OnTheFlyClientFileQuad( String filename2,  OTFConnectionManager connect) {
		this(filename2);
		this.connect = connect;
	}

	public OnTheFlyClientFileQuad( String filename2,  OTFConnectionManager connect,  boolean split) {
		this(filename2);
		this.connect = connect;
		this.splitLayout = split;
	}


	public OnTheFlyClientFileQuad( String filename2,  boolean split) {
		this(filename2);
		this.splitLayout = split;
	}



//	private static DisplayableNetI prepareNet() {
//	String fileName = "E:\\Development\\tmp\\studies\\berlin-wip\\network\\wip_net.xml";
//	NetReaderI netReader = new MatsimNetReader_PLAIN();
//	NetBuffer buffer = new NetBuffer();
//	netReader.readNetwork(buffer, fileName);

//	DisplayNet network = new DisplayNet();
//	NetComposerI netComposer = new TrafficNetComposer();
//	netComposer.compose(network, buffer);
//	return network;

//	}
}

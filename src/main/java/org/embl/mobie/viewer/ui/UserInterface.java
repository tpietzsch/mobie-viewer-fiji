package org.embl.mobie.viewer.ui;

import de.embl.cba.bdv.utils.popup.BdvPopupMenus;
import org.embl.mobie.viewer.MoBIE;
import org.embl.mobie.viewer.VisibilityListener;
import org.embl.mobie.viewer.display.RegionDisplay;
import org.embl.mobie.viewer.view.View;
import org.embl.mobie.viewer.display.ImageDisplay;
import org.embl.mobie.viewer.display.SegmentationDisplay;
import org.embl.mobie.viewer.display.SourceDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserInterface
{
	private final JPanel displaySettingsContainer;
	private final JScrollPane displaySettingsScrollPane;
	private final JFrame frame;
	private final JPanel selectionPanel;
	private final UserInterfaceHelper userInterfaceHelper;
	private Map< Object, JPanel > displayToPanel;
	private JSplitPane splitPane;

	public UserInterface( MoBIE moBIE )
	{
		MoBIELaf.MoBIELafOn();
		userInterfaceHelper = new UserInterfaceHelper( moBIE );
		selectionPanel = userInterfaceHelper.createSelectionPanel();
		displaySettingsContainer = userInterfaceHelper.createDisplaySettingsContainer();
		displaySettingsScrollPane = userInterfaceHelper.createDisplaySettingsScrollPane( displaySettingsContainer );
		JPanel displaySettingsPanel = userInterfaceHelper.createDisplaySettingsPanel( displaySettingsScrollPane );
		displayToPanel = new HashMap<>();
		frame = createAndShowFrame( selectionPanel, displaySettingsPanel, moBIE.getProjectName() + "-" + moBIE.getDatasetName() );
		MoBIELaf.MoBIELafOff();
		configureWindowClosing( moBIE );
	}

	private void configureWindowClosing( MoBIE moBIE )
	{
		frame.addWindowListener(
			new WindowAdapter() {
				public void windowClosing( WindowEvent ev )
				{
					frame.dispose();
					moBIE.close();
				}
			});
	}

	private JFrame createAndShowFrame( JPanel selectionPanel, JPanel displaySettingsPanel, String panelName )
	{
		JFrame frame = new JFrame( "MoBIE: " + panelName );

		splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();


		splitPane.setDividerLocation( actionPanelHeight );
		splitPane.setTopComponent( selectionPanel );
		splitPane.setBottomComponent( displaySettingsPanel );
		splitPane.setAutoscrolls( true );

		// show frame
		frame.setPreferredSize( new Dimension( 550, actionPanelHeight + 200 ) );
		frame.getContentPane().setLayout( new GridLayout() );
		frame.getContentPane().add( splitPane );

		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		frame.pack();
		frame.setVisible( true );

		return frame;
	}

	private void refreshDisplaySettings()
	{
		displaySettingsContainer.revalidate();
		displaySettingsContainer.repaint();
		frame.revalidate();
		frame.repaint();
	}

	private void refreshSelection()
	{
		selectionPanel.revalidate();
		selectionPanel.repaint();
		// update the location of the splitpane divider, so any new uiSelectionGroups are visible
		final int actionPanelHeight = userInterfaceHelper.getActionPanelHeight();
		splitPane.setDividerLocation( actionPanelHeight );
		frame.revalidate();
		frame.repaint();
	}

	public void addViews( Map<String, View> views )
	{
		MoBIELaf.MoBIELafOn();
		userInterfaceHelper.addViewsToSelectionPanel( views );
		refreshSelection();
		MoBIELaf.MoBIELafOff();
	}

	public Map< String, Map< String, View > > getGroupingsToViews()
	{
		return userInterfaceHelper.getGroupingsToViews();
	}

	public void addSourceDisplay( SourceDisplay sourceDisplay )
	{
		MoBIELaf.MoBIELafOn();
		final JPanel panel = createDisplaySettingPanel( sourceDisplay );
		showDisplaySettingsPanel( sourceDisplay, panel );
		MoBIELaf.MoBIELafOff();
	}

	private JPanel createDisplaySettingPanel( SourceDisplay sourceDisplay )
	{
		if ( sourceDisplay instanceof ImageDisplay )
		{
			return userInterfaceHelper.createImageDisplaySettingsPanel( ( ImageDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof SegmentationDisplay )
		{
			return userInterfaceHelper.createSegmentationDisplaySettingsPanel( ( SegmentationDisplay ) sourceDisplay );
		}
		else if ( sourceDisplay instanceof RegionDisplay )
		{
			return userInterfaceHelper.createAnnotatedMaskDisplaySettingsPanel( ( RegionDisplay ) sourceDisplay );
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}

	public void removeDisplaySettingsPanel( Object display )
	{
		SwingUtilities.invokeLater( () -> {
			final JPanel jPanel = displayToPanel.get( display );
			displaySettingsContainer.remove( jPanel );
			displayToPanel.remove( display );
			refreshDisplaySettings();
		} );
	}

	protected void showDisplaySettingsPanel( Object display, JPanel panel )
	{
		SwingUtilities.invokeLater( () -> {
			displayToPanel.put( display, panel );
			displaySettingsContainer.add( panel );

			// scroll to bottom, so any new panels are visible
			displaySettingsScrollPane.validate();
			JScrollBar vertical = displaySettingsScrollPane.getVerticalScrollBar();
			vertical.setValue( vertical.getMaximum() );

			refreshDisplaySettings();
		});
	}

	public Window getWindow()
	{
		return frame;
	}

	public String[] getUISelectionGroupNames() {

		Set<String> groupings = userInterfaceHelper.getGroupings();
		String[] groupNames = new String[groupings.size()];
		int i = 0;
		for ( String groupName: groupings ) {
			groupNames[i] = groupName;
			i++;
		}
		return groupNames;
	}

	public void close()
	{
		frame.dispose();
	}
}

package org.embl.mobie.viewer.command;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ij.gui.NonBlockingGenericDialog;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import javax.swing.*;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Registration - Manual")
public class RegisterManuallyCommand implements BdvPlaygroundActionCommand
{

	private AffineTransform3D originalFixedTransform;

	@Override
	public void run()
	{
		new Thread( () -> showDialog() ).start();
	}

	private void showDialog()
	{
		final NonBlockingGenericDialog dialog = new NonBlockingGenericDialog( "Registration - Manual" );
		dialog.hideCancelButton();
		dialog.addMessage( "Manual affine, similarity, rigid and translation transformations.\n\n" +
				"- Select the BDV window and press P and select the source to be transformed as the current source\n" +
				"- Press T to start the manual transform\n" +
				"- Transform\n" +
				"  - Press X,Y, or Z to change the axis of rotation\n" +
				"  - Use the LEFT and RIGHT arrow keys to rotate\n" +
				"  - Use the UP and DOWN to re-scale\n" +
				"" +
				"Press [ T ] again to fix the transformation\n" +
				"Press [ ESC ] to abort");

		dialog.showDialog();
		if ( dialog.wasCanceled() )
		{
			//resetMovingTransform();
		}
		else
		{
			// accept
		}
	}

	private void storeOriginalTransform(  )
	{
//		originalFixedTransform = new AffineTransform3D();
//		( ( TransformedSource ) .getSpimSource()).getFixedTransform( originalFixedTransform );
	}

	private void resetMovingTransform()
	{
//		( ( TransformedSource) movingSac.getSpimSource() ).setFixedTransform( sacToOriginalFixedTransform.get( movingSac ) );
//		}
//		bdvHandle.getViewerPanel().requestRepaint();
	}
}

/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.embl.cba.mobie.command;

import bdv.util.BdvHandle;
import de.embl.cba.mobie.bdv.ScreenShotMaker;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

/**
 * ScreenShotMakerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Screenshot",
        description = "Creates a screenshot of a BDV view, the resolution can be chosen to upscale or downscale" +
                " the image compared to the original window. A single RGB image resulting from the projection" +
                " of all sources is displayed. Raw image data can also be exported in grayscale.")
public class ScreenShotMakerCommand implements BdvPlaygroundActionCommand {

    @Parameter
    BdvHandle bdvh;

    @Parameter(label="Target Pixel Size (in XY)")
    public double targetPixelSizeInXY = 1;

    @Parameter(label="Pixel Size Unit")
    public String targetPixelUnit = "Pixels";

    @Parameter(label="Show Raw Data")
    public boolean showRawData = false;

    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvh);
        screenShotMaker.setPhysicalPixelSpacingInXY(targetPixelSizeInXY, targetPixelUnit);
        screenShotMaker.getRgbScreenShot().show();
        if(showRawData) screenShotMaker.getRawScreenShot().show();
    }
}

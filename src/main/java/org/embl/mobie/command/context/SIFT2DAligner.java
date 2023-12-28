package org.embl.mobie.command.context;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.*;
import net.imglib2.realtransform.AffineTransform3D;
import org.embl.mobie.lib.MoBIEHelper;
import org.embl.mobie.lib.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

/**
 * Extract landmark correspondences in two images as PointRoi.
 *
 * The plugin uses the Scale Invariant Feature Transform (SIFT) by David Lowe
 * \cite{Lowe04} and the Random Sample Consensus (RANSAC) by Fishler and Bolles
 * \citet{FischlerB81} with respect to a transformation model to identify
 * landmark correspondences.
 *
 * BibTeX:
 * <pre>
 * &#64;article{Lowe04,
 *   author    = {David G. Lowe},
 *   title     = {Distinctive Image Features from Scale-Invariant Keypoints},
 *   journal   = {International Journal of Computer Vision},
 *   year      = {2004},
 *   volume    = {60},
 *   number    = {2},
 *   pages     = {91--110},
 * }
 * &#64;article{FischlerB81,
 *	 author    = {Martin A. Fischler and Robert C. Bolles},
 *   title     = {Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography},
 *   journal   = {Communications of the ACM},
 *   volume    = {24},
 *   number    = {6},
 *   year      = {1981},
 *   pages     = {381--395},
 *   publisher = {ACM Press},
 *   address   = {New York, NY, USA},
 *   issn      = {0001-0782},
 *   doi       = {http://doi.acm.org/10.1145/358669.358692},
 * }
 * </pre>
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.4b
 *
 * Modified by Christian Tischer
 */
public class SIFT2DAligner
{
    final static private DecimalFormat decimalFormat = new DecimalFormat();
    final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    private final BdvHandle bdvHandle;

    private ImagePlus imp1;
    private ImagePlus imp2;

    final private List< Feature > fs1 = new ArrayList< Feature >();
    final private List< Feature > fs2 = new ArrayList< Feature >();;
    private AffineTransform3D affineTransform3D;
    private SourceAndConverter< ? > fixedSac;
    private SourceAndConverter< ? > movingSac;
    private ScreenShotMaker screenShotMaker;
    private static String fixedImageName;
    private static String movingImageName;

    static private class Param
    {
        final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();

        public Double pixelSize = null;

        /**
         * Closest/next closest neighbour distance ratio
         */
        public float rod = 0.92f;

        public boolean useGeometricConsensusFilter = true;

        /**
         * Maximal allowed alignment error in px
         */
        public float maxEpsilon = 25.0f;

        /**
         * Inlier/candidates ratio
         */
        public float minInlierRatio = 0.05f;

        /**
         * Minimal absolute number of inliers
         */
        public int minNumInliers = 7;

        /**
         * Whether to show the detected landmarks
         */
        public boolean showLandmarks = false;

        /**
         * Implemeted transformation models for choice
         */
        final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine", "Perspective" };
        public int modelIndex = 3;
    }

    final static private Param p = new Param();

    public SIFT2DAligner( BdvHandle bdvHandle )
    {
        this.bdvHandle = bdvHandle;

        decimalFormatSymbols.setGroupingSeparator( ',' );
        decimalFormatSymbols.setDecimalSeparator( '.' );
        decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
        decimalFormat.setMaximumFractionDigits( 3 );
        decimalFormat.setMinimumFractionDigits( 3 );
    }

    public boolean showUI()
    {
        double viewerVoxelSpacing = BdvHandleHelper.getViewerVoxelSpacing( bdvHandle );

        p.pixelSize = 2 * viewerVoxelSpacing;

        List< SourceAndConverter< ? > > sourceAndConverters = MoBIEHelper.getVisibleSacs( bdvHandle );
        if ( sourceAndConverters.size() < 2 )
        {
            IJ.showMessage( "There must be at least two images visible." );
            return false;
        }

        final String[] titles = sourceAndConverters.stream()
                .map( sac -> sac.getSpimSource().getName() )
                .toArray( String[]::new );

        final GenericDialog gd = new GenericDialog( "SIFT 2D Aligner" );

        String fixedDefault = Arrays.asList( titles ).contains( fixedImageName ) ? fixedImageName : titles[ 0 ];
        gd.addChoice( "fixed_image :", titles, fixedDefault );

        String movingDefault = Arrays.asList( titles ).contains( movingImageName ) ? movingImageName : titles[ 1 ];
        gd.addChoice( "moving_image :", titles, movingDefault );
        String voxelUnit = sourceAndConverters.get( 0 ).getSpimSource().getVoxelDimensions().unit();

        gd.addNumericField( "pixel_size :", p.pixelSize, 2, 6, voxelUnit );
        gd.addChoice( "expected_transformation :", Param.modelStrings, Param.modelStrings[ p.modelIndex ] );

        gd.addNumericField( "initial_gaussian_blur :", p.sift.initialSigma, 2, 6, "px" );
        gd.addNumericField( "steps_per_scale_octave :", p.sift.steps, 0 );
        gd.addNumericField( "minimum_image_size :", p.sift.minOctaveSize, 0, 6, "px" );
        gd.addNumericField( "maximum_image_size :", p.sift.maxOctaveSize, 0, 6, "px" );

        gd.addNumericField( "feature_descriptor_size :", p.sift.fdSize, 0 );
        gd.addNumericField( "feature_descriptor_orientation_bins :", p.sift.fdBins, 0 );
        gd.addNumericField( "closest/next_closest_ratio :", p.rod, 2 );

        gd.addCheckbox( "filter matches by geometric consensus", p.useGeometricConsensusFilter );
        gd.addNumericField( "maximal_alignment_error :", p.maxEpsilon, 2, 6, "px" );
        gd.addNumericField( "minimal_inlier_ratio :", p.minInlierRatio, 2 );
        gd.addNumericField( "minimal_number_of_inliers :", p.minNumInliers, 0 );

        gd.addCheckbox( "show images with detected landmarks", false );

        gd.showDialog();

        if (gd.wasCanceled()) return false;

        fixedImageName = gd.getNextChoice();
        movingImageName = gd.getNextChoice();

        p.pixelSize = gd.getNextNumber();
        p.modelIndex = gd.getNextChoiceIndex();

        p.sift.initialSigma = ( float )gd.getNextNumber();
        p.sift.steps = ( int )gd.getNextNumber();
        p.sift.minOctaveSize = ( int )gd.getNextNumber();
        p.sift.maxOctaveSize = ( int )gd.getNextNumber();

        p.sift.fdSize = ( int )gd.getNextNumber();
        p.sift.fdBins = ( int )gd.getNextNumber();
        p.rod = ( float )gd.getNextNumber();

        p.useGeometricConsensusFilter = gd.getNextBoolean();
        p.maxEpsilon = ( float )gd.getNextNumber();
        p.minInlierRatio = ( float )gd.getNextNumber();
        p.minNumInliers = ( int )gd.getNextNumber();

        p.showLandmarks = gd.getNextBoolean();

        return run( sourceAndConverters, fixedImageName, movingImageName );
    }

    public boolean run( List< SourceAndConverter< ? > > sourceAndConverters, String fixedImageName, String movingImageName )
    {
        extractImages( sourceAndConverters, fixedImageName, movingImageName );

        return run( imp1, imp2 );
    }

    private void extractImages( List< SourceAndConverter< ? > > sourceAndConverters, String fixedImageName, String movingImageName )
    {
        fixedSac = sourceAndConverters.stream()
                .filter( sac -> sac.getSpimSource().getName().equals( fixedImageName ) )
                .findFirst().get();

        movingSac = sourceAndConverters.stream()
                .filter( sac -> sac.getSpimSource().getName().equals( movingImageName ) )
                .findFirst().get();

        screenShotMaker = new ScreenShotMaker( bdvHandle, p.pixelSize, fixedSac.getSpimSource().getVoxelDimensions().unit() );
        screenShotMaker.run( Arrays.asList( fixedSac, movingSac ) );
        CompositeImage compositeImage = screenShotMaker.getCompositeImagePlus();

        ImageStack stack = compositeImage.getStack();
        imp1 = new ImagePlus( "fixed", stack.getProcessor( 1 ) );
        imp2 = new ImagePlus( "moving", stack.getProcessor( 2 ) );

        // Setting the display ranges is important
        // as those will be used by the SIFT for normalising the pixel values
        compositeImage.setPosition( 1 );
        imp1.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
        compositeImage.setPosition( 2 );
        imp2.getProcessor().setMinAndMax( compositeImage.getDisplayRangeMin(), compositeImage.getDisplayRangeMax() );
    }

    /**
     * Execute with current parameters
     *
     * @return
     *        boolean whether a model was found
     */
    private boolean run( final ImagePlus imp1, final ImagePlus imp2) {

        // cleanup
        fs1.clear();
        fs2.clear();

        final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
        final SIFT ijSIFT = new SIFT( sift );

        long start_time = System.currentTimeMillis();
        IJ.log( "Processing SIFT ..." );
        ijSIFT.extractFeatures( imp1.getProcessor(), fs1 );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
        IJ.log( fs1.size() + " features extracted." );

        start_time = System.currentTimeMillis();
        IJ.log( "Processing SIFT ..." );
        ijSIFT.extractFeatures( imp2.getProcessor(), fs2 );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );
        IJ.log( fs2.size() + " features extracted." );

        start_time = System.currentTimeMillis();
        IJ.log( "Identifying correspondence candidates using brute force ..." );
        final List< PointMatch > candidates = new ArrayList< PointMatch >();
        FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );
        IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );

        final ArrayList< Point > p1 = new ArrayList< Point >();
        final ArrayList< Point > p2 = new ArrayList< Point >();
        final List< PointMatch > inliers;

        boolean modelFound = false;

        if ( p.useGeometricConsensusFilter )
        {
            IJ.log( candidates.size() + " potentially corresponding features identified." );

            start_time = System.currentTimeMillis();
            IJ.log( "Filtering correspondence candidates by geometric consensus ..." );
            inliers = new ArrayList< PointMatch >();

            AbstractModel< ? > model;
            switch ( p.modelIndex )
            {
                case 0:
                    model = new TranslationModel2D();
                    break;
                case 1:
                    model = new RigidModel2D();
                    break;
                case 2:
                    model = new SimilarityModel2D();
                    break;
                case 3:
                    model = new AffineModel2D();
                    break;
                case 4:
                    // TODO: What is this?
                    model = new HomographyModel2D();
                    break;
                default:
                    return modelFound;
            }


            try
            {
                modelFound = model.filterRansac(
                        candidates,
                        inliers,
                        1000,
                        p.maxEpsilon,
                        p.minInlierRatio,
                        p.minNumInliers );

                if ( model instanceof AbstractAffineModel2D )
                {
                    affineTransform3D = new AffineTransform3D();

                    // global to target canvas
                    AffineTransform3D canvasToGlobalTransform = screenShotMaker.getCanvasToGlobalTransform();
                    affineTransform3D.preConcatenate( canvasToGlobalTransform.inverse() );

                    // sift within canvas
                    final double[] a = new double[6];
                    ( ( AbstractAffineModel2D< ? > ) model ).toArray( a );
                    AffineTransform3D canvasSiftTransform = new AffineTransform3D();
                    canvasSiftTransform.set(
                            a[0], a[2], 0, a[4],
                            a[1], a[3], 0, a[5],
                            0, 0, 1, 0);
                    affineTransform3D.preConcatenate( canvasSiftTransform.inverse() );

                    // canvas to global
                    affineTransform3D.preConcatenate( canvasToGlobalTransform );
                }
                else
                    IJ.showMessage( "Cannot apply " + model );

            }
            catch ( final NotEnoughDataPointsException e )
            {
                modelFound = false;
            }

            IJ.log( " took " + ( System.currentTimeMillis() - start_time ) + "ms." );

            if ( modelFound )
            {
                PointMatch.apply( inliers, model );

                IJ.log( inliers.size() + " corresponding features with an average displacement of " + decimalFormat.format( PointMatch.meanDistance( inliers ) ) + "px identified." );
                IJ.log( "Estimated transformation model: " + model );
            }
            else
                IJ.log( "No correspondences found." );
        }
        else
        {
            inliers = candidates;
            IJ.log( candidates.size() + " corresponding features identified." );
        }

        if ( ! inliers.isEmpty() && p.showLandmarks )
        {
            imp1.show();
            imp2.show();
            PointMatch.sourcePoints( inliers, p1 );
            PointMatch.targetPoints( inliers, p2 );
            imp1.setRoi( Util.pointsToPointRoi( p1 ) );
            imp2.setRoi( Util.pointsToPointRoi( p2 ) );
        }

        return modelFound;
    }

    public AffineTransform3D getSiftTransform3D()
    {
        return affineTransform3D;
    }

    public SourceAndConverter< ? > getMovingSac()
    {
        return movingSac;
    }
}
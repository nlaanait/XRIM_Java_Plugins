 //package ij.plugin.filter;
import  ij.*;
import  ij.gui.DialogListener;
import  ij.gui.GenericDialog;
import  ij.process.*;
import 	ij.plugin.filter.*;
import  ij.plugin.filter.GaussianBlur;
import  ij.plugin.filter.BackgroundSubtracter;
import  ij.plugin.*;

import java.awt.AWTEvent;
import java.util.regex.*;


public class FlatField_ implements ExtendedPlugInFilter, DialogListener {

	private ImagePlus imp;
	private ImagePlus impRaw;
	private ImagePlus impProcess;
	public int flags = DOES_ALL;
	private int nPasses = 1;
	private int pass;
	private boolean noProgress; 
	/** standard deviation of the Gaussian blur */
	private double sigma = 0.;
	/** radius of the rolling ball */
	private double radius = 0.;
	/** Processing flags */
	private boolean noprocess;
	/** Stats of the raw iamge */
	private double rawMean;
	private double rawMax;
	/** Stats of the illumination */
	private double flatMean;
	private double flatMax; 
	private double normfactor;
	
	/** Method to return types supported
    	 * @param arg unused
     	* @param imp The ImagePlus, used to get the spatial calibration
     	* @return Code describing supported formats etc.
     	* (see ij.plugin.filter.PlugInFilter & ExtendedPlugInFilter)
     	*/
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return flags;
	}

	/** Ask the user for the parameters */
	public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		GenericDialog gd = new GenericDialog(command);
		gd.addSlider("Rolling Ball Radius",0.0,500.,0.0);
		gd.addSlider("Gaussian Blur Sigma", 0.0,200.,0.0);
		gd.addPreviewCheckbox(pfr);
		gd.addDialogListener(this);
		gd.setOKLabel("process");
		gd.showDialog();
		if (gd.wasCanceled()) {
			noprocess = true;
			return DONE;
		}
	//	if (gd.wasOKed()) process = true;	
		IJ.register(this.getClass());	
		return IJ.setupDialog(imp, flags); 
	}

	/** Listener to modifications of the input fields of the dialog */
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e){
		radius = gd.getNextNumber();
		sigma  = gd.getNextNumber();
		if (radius < 0 || sigma < 0) return false;
		else return true;	
	}
	
	public void setNPasses(int nPasses){
		this.nPasses = nPasses; 
		pass = 0;
	}

	/** This method is invoked for each slice during execution
     	* @param ip The image subject to filtering.
     	*/
	public void run(ImageProcessor ip){
		if(radius == 0 & sigma == 0) {
			Duplicator dup = new Duplicator();
			ImagePlus impRaw = dup.run(imp);
			String RawTitle = imp.getTitle();
			impRaw.setTitle(RawTitle);
			impRaw.show();
			imp.setTitle("Illumination");
			/** Despeckle the raw image, run a median filter with a pixel radius of 5 */
			RankFilters rf = new RankFilters();
			rf.rank(ip,5,4);
			/** Getting stats of the raw image */
			rawMean = Math.round(ip.getStatistics().mean);
			rawMax = Math.round(ip.getStatistics().max);	
		}
		
		else {
			/** Subtract bacgkround and show it */
			BackgroundSubtracter bs = new BackgroundSubtracter();
			bs.rollingBallBackground(ip,radius,true,true,false,false,false);	
			/** Blur the subtracted background */	
			GaussianBlur gb = new GaussianBlur();
			gb.blurGaussian(ip,sigma,sigma,0.02);
			/** Getting stats of the illumination and logging... */
			flatMean = Math.round(ip.getStatistics().mean);
			flatMax = Math.round(ip.getStatistics().max);	
			IJ.log("\nRaw: mean = "+rawMean+", max = "+rawMax+"\nIllumination: mean = "+
			+flatMean+", max = "+flatMax);
			normfactor = 1. / flatMean;
			ip.multiply(normfactor);
			IJ.run(imp, "Enhance Contrast", "saturated=0.35");
			if (noprocess){
				imp.close();
			}
		}		
	}
		
				
	private void showProgress(double percent) {
    		if (noProgress) return;
        	percent = (double)(pass-1)/nPasses + percent/nPasses;
        	IJ.showProgress(percent);
    	}
    
    	public void showProgress(boolean showProgressBar) {
    		noProgress = !showProgressBar;
    	}

}

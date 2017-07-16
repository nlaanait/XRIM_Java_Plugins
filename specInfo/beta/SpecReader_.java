 //package ij.plugin.filter;
import  ij.*;
import  ij.gui.DialogListener;
import  ij.gui.GenericDialog;
import  ij.process.*;
import 	ij.plugin.filter.*;
import  ij.plugin.filter.GaussianBlur;
import  ij.plugin.filter.BackgroundSubtracter;
import  ij.plugin.*;
import  ij.plugin.filter.PlugInFilterRunner;
import  ij.plugin.filter.ImageProperties;

import java.awt.AWTEvent;
import java.util.regex.*;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Info;
import ij.text.TextWindow;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.io.FileInfo;
import ij.*;
import ij.gui.GenericDialog;

//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
import java.io.*;
import java.util.Scanner;
import java.util.regex.*;
import java.lang.reflect.Field;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

public class SpecReader_ implements ExtendedPlugInFilter {

	private ImagePlus imp;
	private ImagePlus impRaw;
	private ImagePlus impProcess;
	public int flags = DOES_ALL|NO_CHANGES;
	private int nPasses = 1;
	public int slicenumber;
	public int pass=0;
	private boolean showInfoFlag;
	private FileInfo fi;
	private String impTitle;
	private String specFilePath;
	private String[] infoArray;
	private Boolean labelFlag = false;
	private String info;
	private int call = 1;
	SpecFileInfo sfi = new SpecFileInfo();
	
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
		GenericDialog gd = new GenericDialog("Spec Info");
        	gd.addMessage("Label Image with Spec Info ? ");
        	gd.addCheckbox("Pull up Info",false);
        	gd.setOKLabel("Yes");
        	gd.setCancelLabel("No");
        	// need a text dialog to label by some user defined value
            	gd.showDialog();
            	if (gd.wasOKed()) labelFlag= true;
            	//specFilePath = IJ.getFilePath("Locate .hlog file");
            	showInfoFlag = gd.getNextBoolean();
            		
		IJ.register(this.getClass());	
		return IJ.setupDialog(imp, flags); 
	}

	/** Used by PlugInFilterRunner to figure out how many calls
	 *  it'll make to run()
	 */
	public void setNPasses(int nPasses){
		this.nPasses = nPasses; 
		pass = 0;
	}
	
	/** This method is invoked for each slice during execution
     	* @param ip The image subject to filtering.
     	*/
	public void run(ImageProcessor ip){
		
		// Get image title and dir
		String SliceLabel;
		String[] titles;
		String regexp;
		ImageStack stack = imp.getStack();
		if(stack.getSize() > 1){
			int slice = imp.getCurrentSlice();
			if(call <= 1){
				SliceLabel = stack.getSliceLabel(slice);	
				regexp = "[\\n]+";
				titles = SliceLabel.split(regexp);
				impTitle = titles[0];
				
			} else {
				SliceLabel = stack.getSliceLabel(call);	
				regexp = "[\\n]+";
				titles = SliceLabel.split(regexp);
				impTitle = titles[0];
			}
		} else {
			impTitle = imp.getTitle();
			}  
			
	  	fi = imp.getOriginalFileInfo();
	//   	IJ.log(""+fi.directory);
	//	IJ.log(impTitle);

	   	
	   	// parse to find metadata for the image
//	  	infoArray = parseSpecFile(fi,impTitle); 
		ArrayList infoList = parseSpecFile(fi,impTitle);
	  	
//		// log the metadata
//		for(int i=0; i<100;i++){
//			IJ.log(""+infoList.get(i).toString());
//		}		
		Object o = infoList.get(1);
		IJ.log(""+infoList.indexOf("#PROIE"));
//		// convert metadata to info
//		
//	  	setSpecFileInfo(sfi, infoArray,call); 
//   	
//	   	// set properties of the image for future access
//	 	setSpecProperties(sfi); 
//  	
// 
//
//		// creating a string info for image
//	  	String specInfo = makeSpecInfo(sfi, call); 
//	   //	String specInfo = "";
//	   	// pull up the info
//        	if (showInfoFlag) showInfo(specInfo,impTitle,500, 300);
//        	
//        	// tag the image
//                if (labelFlag){
//                	int drawSpot;
//                	int fontSize;
//               		if(imp.getWidth() > 512){
//               			fontSize = 26;
//               			drawSpot = imp.getWidth()-120;
//               		} else {
//               			fontSize = 13;
//               			drawSpot = imp.getWidth()-60;
//               		}
//            	ip.setFont(new Font("SansSerif", Font.BOLD, fontSize));
//				ip.setColor(Color.white);
//				String label = makeLabel(specInfo);
//				ip.drawString(label,30,drawSpot);  
//				imp.updateAndDraw();
//               	} 
       	
       		call++;    
       		
       			
	}		


		

	/* Methods */


   
	public ArrayList parseSpecFile(FileInfo fi, String impTitle){
		// Get logfile, by searching scan directory
		File file;
	
		File f = new File(fi.directory);
		File[] matchingFiles = f.listFiles(new FilenameFilter() {
    		public boolean accept(File dir, String name) {
        		return name.endsWith("log");
   		 	}
		});
		
			file = matchingFiles[0];
		
		// Scan the  logfile for the title of the image, when it matches take the following 70 lines
		ArrayList list = new ArrayList(50); // 50 is # of lines to read from log file for imp
		try {
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
   				String lineFromFile = scanner.nextLine();
   				if(lineFromFile.contains(impTitle)) {
   					list.add(0, lineFromFile);
   					for (int i=1;i<200;i++){
   						lineFromFile = scanner.nextLine();
   						if(lineFromFile.contains("#PF")){
   							break;
   						} else{
   							list.add(i,lineFromFile);
   						}
   					}
   				}
			}
			scanner.close();
		} catch (FileNotFoundException e){
			IJ.error("log file does not contain the image info");	
		} catch (IOException e){
			e.printStackTrace();
			IJ.error("I/O exception");
		}
	// Get rid of empty lines
		List infoList = list;
		do {
	   	 	infoList.remove("");	
	   	} while (infoList.indexOf("") != -1);
	// Cast the list into a string array and return it
	   	String[] infoArray = (String[])infoList.toArray(new String[0]);

	   	return list;
//		return infoArray;
	}


//   
//	public void setSpecFileInfo(SpecFileInfo sfi, String[] specInfoArray,int slicenumber){
//
//		// need a try and catch for Array out of bounds exceptions
//		
//		// Extract the info from the spec file
//		String regexp = "[ ]+";
//		String[] pfLine = specInfoArray[0].split(regexp);
//		String[] pdLine = specInfoArray[1].split(regexp);
//		String[] g1Line = specInfoArray[4].split(regexp);
//		String[] pqLine = specInfoArray[7].split(regexp);
//		String[] motorNames1 = specInfoArray[9].split(regexp);
//		String[] motorValues1 = specInfoArray[10].split(regexp);
//		String[] motorNames2 = specInfoArray[11].split(regexp);
//		String[] motorValues2 = specInfoArray[12].split(regexp);
//		String[] normNames = specInfoArray[23].split(regexp);
//		String[] normValues = specInfoArray[24].split(regexp);
//		String[] counters = specInfoArray[26].split(regexp);
//	
//		// Assign the fields of SpecFileInfo Class from the array above
//		sfi.title[slicenumber]=pfLine[4];
//		sfi.timeStamp[slicenumber] = pdLine[2]+" "+pdLine[3]+" "+pdLine[4]+" "+pdLine[5];
//		sfi.a = Double.parseDouble(g1Line[1]);
//		sfi.b = Double.parseDouble(g1Line[2]);
//		sfi.c = Double.parseDouble(g1Line[3]);
//		sfi.H[slicenumber] = Double.parseDouble(pqLine[1]);
//		sfi.K[slicenumber] = Double.parseDouble(pqLine[2]);
//		sfi.L[slicenumber] = Double.parseDouble(pqLine[3]);
//		sfi.theta[slicenumber] = Double.parseDouble(motorValues1[2]);
//		sfi.phi[slicenumber] = Double.parseDouble(motorValues1[1]);
//		sfi.twotheta[slicenumber] = Double.parseDouble(motorValues1[3]);
//		sfi.nu[slicenumber] = Double.parseDouble(motorValues1[4]);
//		sfi.sampleX[slicenumber] = Double.parseDouble(motorValues1[6]);
//		sfi.sampleY[slicenumber] = Double.parseDouble(motorValues1[7]);
//		sfi.sampleZ[slicenumber] = Double.parseDouble(motorValues1[8]);
//		sfi.sampleU[slicenumber] = Double.parseDouble(motorValues2[1]);
//		sfi.sampleV[slicenumber] = Double.parseDouble(motorValues2[2]);
//		sfi.sampleW[slicenumber] = Double.parseDouble(motorValues2[3]);
//		sfi.lensX[slicenumber] = Double.parseDouble(motorValues2[6]);
//		sfi.lensY[slicenumber] = Double.parseDouble(motorValues2[7]);
//		sfi.lensZ[slicenumber] = Double.parseDouble(motorValues2[8]);
//		sfi.exposure[slicenumber] = Double.parseDouble(normValues[1]);
//		sfi.energy[slicenumber] = Double.parseDouble(normValues[6]);
//		sfi.trans[slicenumber] = Double.parseDouble(normValues[8]);	
//		sfi.field[slicenumber] = Double.parseDouble(counters[7]);
//	}	
//		
//			
//  	public String makeSpecInfo(SpecFileInfo sfi, int slicenumber){
//  		int i = slicenumber;	
//  		// Build up the info string array
//  		String info = "Potential (V): " + sfi.field[i] + "\n";
//  		info += "Title: " + sfi.title[i] + "\n";
//		info += "Time stamp: " + sfi.timeStamp[i] + "\n";
//		info += "Exposure (sec) : " + sfi.exposure[i] + "\n";
//		info += "(H, K, L): " + sfi.H[i] + "\t \t " + sfi.K[i] + "\t \t " + sfi.L[i] + "\n";
//		info += "Lattice constants (a, b, c) "+ IJ.angstromSymbol +": " + sfi.a + "\t \t " + sfi.b + "\t \t " + sfi.c + "\n";
//		info += "Energy (keV) : " + sfi.energy[i] + "\n";
//		info += "Transmission: " + sfi.trans[i] + "\n";
//		info += "Sample Rotations (theta, phi) (deg.) : " + sfi.theta[i] + "\t \t " + sfi.phi[i] + "\n";
//		info += "Detector Rotations (2theta, nu) (deg.) : " + sfi.twotheta[i] + "\t \t " + sfi.nu[i] + "\n";
//		info += "Sample Translations (X, Y, Z) (mm) : " + sfi.sampleX[i] + "\t \t " + sfi.sampleY[i] + "\t \t " + sfi.sampleZ[i] + "\n";
//		info += "Sample pseudo-Rotations (U, V, W) (deg.) : " + sfi.sampleU[i] + "\t \t " + sfi.sampleV[i] + "\t \t " + sfi.sampleW[i] + "\n";
//		info += "Objective Lens Translations (X, Y, Z) (mm) : " + sfi.lensX[i] + "\t \t " + sfi.lensY[i] + "\t \t " + sfi.lensZ[i] + "\n";
//  
//		return info;
//	}
//	
//	public TextWindow showInfo(String text, String impTitle, int width, int height ){
//		imp.setProperty("Info",text);
//		TextWindow txtWin = new TextWindow("Info for "+impTitle, text, width, height);
//		return txtWin;
//	}
//
//	public void setSpecProperties(SpecFileInfo sfi){
//        	imp.setProperty("H",sfi.H);
//        	imp.setProperty("K",sfi.K);
//        	imp.setProperty("L",sfi.L);
//        	imp.setProperty("Energy",sfi.energy);
//        	imp.setProperty("Transmission",sfi.trans);
//        	imp.setProperty("Exposure",sfi.exposure);
//        	imp.setProperty("Theta",sfi.theta);
//        	imp.setProperty("2Theta",sfi.twotheta);
//			imp.setProperty("Phi",sfi.phi);
//			imp.setProperty("Nu",sfi.nu);
//			imp.setProperty("a",sfi.a);
//			imp.setProperty("b",sfi.b);
//			imp.setProperty("c",sfi.c);
//			imp.setProperty("Sample X",sfi.sampleX);
//			imp.setProperty("title",sfi.title);
//			imp.setProperty("Field",sfi.field);
//			imp.setProperty("Lens Z",sfi.lensZ);
//			imp.setProperty("Title",sfi.title);
//	}
//	
//	public String makeLabel(String specInfo) {
//		String regexp = "[\\n]+";
//		String[] sArray = specInfo.split(regexp);
//		String label = sArray[0];
//	//	label +="\n" +sArray[1]; 
//		label += "\n"+sArray[3]+"\t \t \t"+sArray[7];//+"\t \t \t \t"+sArray[5];
//		label += "\n"+sArray[4]+"\n"+sArray[10];
//		//label += "\n"+sArray[7]+"\n"+sArray[11];
//		//label += "\n"+sArray[8];
//		return label;
//	}	
//
///*
// // And this is how you access the image properties now or later
//	   	Object H = imp.getProperty("H");  
//		double[] h = (double[])H;
//		IJ.log(" "+h[1]);
//	*/	
	
}

import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Info;
import ij.text.TextWindow;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.io.FileInfo;
import ij.*;
import ij.gui.GenericDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.*;
import java.lang.reflect.Field;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

public class SpecReader_ implements PlugInFilter {
	ImagePlus imp;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
        return DOES_ALL|NO_CHANGES ;
	}

	@Override
	public void run(ImageProcessor ip) {
		
		// Generate dialog
		GenericDialog gd = new GenericDialog("Spec Info");
        	gd.addMessage("Label Image with Spec Info ? ");
        	gd.addCheckbox("Process all images in stack", false);
        	gd.setOKLabel("Yes");
        	gd.setCancelLabel("No");
            	gd.showDialog();
            	
            	// Get input from dialog and initialize some image fields
            	Boolean labelFlag = true;
            	FileInfo fi;
            	SpecFileInfo sfi = new SpecFileInfo();
            	String impTitle ;
            	String specFilePath;
            	String info ;
            	String[] infoArray;
        	int stackSize = imp.getStackSize();
        	int sliceNumber;
        	if(gd.wasOKed()) { 
        		labelFlag = true;
        	} else if (gd.wasCanceled()) { 
        		labelFlag = false;
        	}

	   	fi = imp.getOriginalFileInfo();
	   	
		// Processing a stack of images
		if(gd.getNextBoolean()){
			ImageStack stack = imp.getStack();
			String[] stackLabels = stack.getSliceLabels();
			impTitle = stackLabels[1];
			specFilePath = getSpecFilePath(fi,impTitle);	// Get path of spec file *.hlog
			int slice = 1;	 	
			for(String sliceLabel : stackLabels){		// Loop over images in stack
				if(sliceLabel != null){
					infoArray = parseSpecFile(specFilePath,sliceLabel); // parse to find metadata for the image
	   			//	setSpecFileInfo(sfi, infoArray,slice); // use metadata to fill the fields of SpecFileinfo	 		
	   				slice++;
				}
			}
			int nofSlices = slice-1;
			setSpecProperties(sfi);// set properties of the image for future access	 
			//	Object H = imp.getProperty("H"); how to access the properties 
			//	double[] h = (double[])H;
			//	IJ.log(" "+h[0]);
			IJ.log(""+sfi.title[2]);
			String[] specInfo = makeSpecInfo(sfi, nofSlices); // creating a string info for the stack
		
			// Tagging the images with some spec info and creating a new stack 
			if(labelFlag){
				ImageStack tagStack = new ImageStack(1024, 1250,nofSlices);
				for (int i=1; i <= nofSlices; i++){
					ImageProcessor ipSlice = stack.getProcessor(i);
					ShortProcessor ipTag = new ShortProcessor(1024,1250);
					ipTag.copyBits(ipSlice,0,0,0);
					ipTag.setFont(new Font("SansSerif", Font.BOLD, 25));
					ipTag.setColor(Color.white);
					String label = makeLabel(specInfo[i]);
					ipTag.drawString(label,30,1050);
					tagStack.setProcessor(ipTag,i);
				}
				ImagePlus impTag = new ImagePlus("tag_"+impTitle,tagStack);
				impTag.show();
				IJ.run(impTag, "Enhance Contrast","saturated=0.35");
				impTag.updateAndDraw();
			}
		//process a single image copy from v2
		} else { 
			impTitle = imp.getTitle();
	   		fi = imp.getOriginalFileInfo();
	   		specFilePath = getSpecFilePath(fi,impTitle);	// Get path of spec file *.hlog
	   		infoArray = parseSpecFile(specFilePath,impTitle); // parse to find metadata for the image	
	   		setSpecFileInfo(sfi, infoArray,1); // convert metadata to info
	   		setSpecProperties(sfi); // set properties of the image for future access
	   		String[] specInfo = makeSpecInfo(sfi, 1); // creating a string info for image
        		showInfo(specInfo[1],impTitle,500, 300); // pull up the info
                	if (labelFlag) setSpecLabel(info,impTitle); // tag the image     		
		}
		
		 	
//        	showInfo(info,impTitle,500, 300); // pull up the info
//                if (labelFlag) setSpecLabel(info,impTitle); // tag the image     	
	}




	/* Methods */



   	public String getSpecFilePath (FileInfo fi, String impTitle){
		String regexp = "[.]+";
		String[] parts = impTitle.split(regexp);
		String specFileName = parts[0]+".hlog";
		String specFilePath = fi.directory + specFileName;
		// Check if file exists, if not prompt for new path
		File file;
		file = new File(specFilePath);
		if (!file.exists()) {
			IJ.error(".hlog file not found in same folder as image.");
			specFilePath = IJ.getFilePath("Locate .hlog file");
		}
		return specFilePath;
	}


    
	public String[] parseSpecFile(String specFilePath, String impTitle){
		File file;
		//String[] specInfoArray = new String[27]; // 26 is # of lines to read from hlog file for imp
		ArrayList list = new ArrayList(26);
		try {
			file = new File(specFilePath);
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
   				String lineFromFile = scanner.nextLine();
   				if(lineFromFile.contains(impTitle)) {
   					//specInfoArray[0] = lineFromFile; 
   					for(int i=0; i<26; i++){ 
   					//	if(scanner.nextLine().())
   					//	String dummy = scanner.nextLine();
   					//	if(dummy != null) specInforArray[i+1];
   					//	if(scanner.nextLine() != null)
       					//	specInfoArray[i+1] = scanner.nextLine();
       					list.add(i, scanner.nextLine());
   					}
       					break;
   				}
			}
			scanner.close();
		} catch (FileNotFoundException e){
			IJ.error("file not found");	
		} catch (IOException e){
			e.printStackTrace();
			IJ.error("I/O exception");
		}
	//	return specInfoArray;
		List infoList = list;
		do {
	   	 	infoList.remove("");	
	   	} while (infoList.indexOf("") != -1);
	   	String[] infoArray = (String[])infoList.toArray(new String[0]);
		return infoArray;
	}


    
	public void setSpecFileInfo(SpecFileInfo sfi, String[] specInfoArray, int slicenumber){
		
		// Extract the info from the spec file
		String regexp = "[ ]+";
		String[] pfLine = specInfoArray[0].split(regexp);
		String[] pdLine = specInfoArray[1].split(regexp);
		String[] g1Line = specInfoArray[4].split(regexp);
		String[] pqLine = specInfoArray[7].split(regexp);
		String[] motorNames1 = specInfoArray[9].split(regexp);
		String[] motorValues1 = specInfoArray[10].split(regexp);
		String[] motorNames2 = specInfoArray[11].split(regexp);
		String[] motorValues2 = specInfoArray[12].split(regexp);
		String[] normNames = specInfoArray[23].split(regexp);
		String[] normValues = specInfoArray[24].split(regexp);
		
		// Assign the fields of SpecFileInfo Class
		sfi.title[slicenumber] = pfLine[4];
		sfi.timeStamp[slicenumber] = pdLine[2]+" "+pdLine[3]+" "+pdLine[4]+" "+pdLine[5];
		sfi.a = Double.parseDouble(g1Line[1]);
		sfi.b = Double.parseDouble(g1Line[2]);
		sfi.c = Double.parseDouble(g1Line[3]);
		sfi.H[slicenumber] = Double.parseDouble(pqLine[1]);
		sfi.K[slicenumber] = Double.parseDouble(pqLine[2]);
		sfi.L[slicenumber] = Double.parseDouble(pqLine[3]);
		sfi.theta[slicenumber] = Double.parseDouble(motorValues1[2]);
		sfi.phi[slicenumber] = Double.parseDouble(motorValues1[1]);
		sfi.twotheta[slicenumber] = Double.parseDouble(motorValues1[3]);
		sfi.nu[slicenumber] = Double.parseDouble(motorValues1[4]);
		sfi.sampleX[slicenumber] = Double.parseDouble(motorValues1[6]);
		sfi.sampleY[slicenumber] = Double.parseDouble(motorValues1[7]);
		sfi.sampleZ[slicenumber] = Double.parseDouble(motorValues1[8]);
		sfi.sampleU[slicenumber] = Double.parseDouble(motorValues2[1]);
		sfi.sampleV[slicenumber] = Double.parseDouble(motorValues2[2]);
		sfi.sampleW[slicenumber] = Double.parseDouble(motorValues2[3]);
		sfi.lensX[slicenumber] = Double.parseDouble(motorValues2[6]);
		sfi.lensY[slicenumber] = Double.parseDouble(motorValues2[7]);
		sfi.lensZ[slicenumber] = Double.parseDouble(motorValues2[8]);
		sfi.exposure[slicenumber] = Double.parseDouble(normValues[1]);
		sfi.energy[slicenumber] = Double.parseDouble(normValues[6]);
		sfi.trans[slicenumber] = Double.parseDouble(normValues[8]);	
	}	
		
	
 		
  	public String[] makeSpecInfo(SpecFileInfo sfi, int nofSlices ){	
  		// Build up the info string array
  		String[] info = new String[nofSlices+1]; // offset size so that slicenumber = array[slicenumber]
  		for (int i = 1; i<= nofSlices;i++ ){
			info[i] = "Title: " + sfi.title[i] + "\n";
			info[i] += "Time stamp: " + sfi.timeStamp[i] + "\n";
			info[i] += "Exposure (sec) : " + sfi.exposure[i] + "\n";
			info[i] += "(H, K, L): " + sfi.H[i] + "\t \t " + sfi.K[i] + "\t \t " + sfi.L[i] + "\n";
			info[i] += "Lattice constants (a, b, c) "+ IJ.angstromSymbol +": " + sfi.a + "\t \t " + sfi.b + "\t \t " + sfi.c + "\n";
			info[i] += "Energy (keV) : " + sfi.energy[i] + "\n";
			info[i] += "Transmission: " + sfi.trans[i] + "\n";
			info[i] += "Sample Rotations (theta, phi) (deg.) : " + sfi.theta[i] + "\t \t " + sfi.phi[i] + "\n";
			info[i] += "Detector Rotations (2theta, nu) (deg.) : " + sfi.twotheta[i] + "\t \t " + sfi.nu[i] + "\n";
			info[i] += "Sample Translations (X, Y, Z) (mm) : " + sfi.sampleX[i] + "\t \t " + sfi.sampleY[i] + "\t \t " + sfi.sampleZ[i] + "\n";
			info[i] += "Sample pseudo-Rotations (U, V, W) (deg.) : " + sfi.sampleU[i] + "\t \t " + sfi.sampleV[i] + "\t \t " + sfi.sampleW[i] + "\n";
			info[i] += "Objective Lens Translations (X, Y, Z) (mm) : " + sfi.lensX[i] + "\t \t " + sfi.lensY[i] + "\t \t " + sfi.lensZ[i] + "\n";
  		}
		return info;
	}
	
	public TextWindow showInfo(String text, String impTitle, int width, int height ){
		imp.setProperty("Info",text);
		TextWindow txtWin = new TextWindow("Info for "+impTitle, text, width, height);
		return txtWin;
	}

	public void setSpecProperties(SpecFileInfo sfi){
        	imp.setProperty("H",sfi.H);
        	imp.setProperty("K",sfi.K);
        	imp.setProperty("L",sfi.L);
        	imp.setProperty("Energy",sfi.energy);
        	imp.setProperty("Transmission",sfi.trans);
        	imp.setProperty("Exposure",sfi.exposure);
        	imp.setProperty("Theta",sfi.theta);
        	imp.setProperty("2Theta",sfi.twotheta);
		imp.setProperty("Phi",sfi.phi);
		imp.setProperty("a",sfi.a);
		imp.setProperty("b",sfi.b);
		imp.setProperty("c",sfi.c);
		imp.setProperty("Sample X",sfi.sampleX);
		imp.setProperty("title",sfi.title);
	}
	
	public String makeLabel(String specInfo) {
		String regexp = "[\\n]+";
		String[] sArray = specInfo.split(regexp);
		String label = sArray[0];
		label += "\n"+sArray[2]+"\t \t \t"+sArray[6]+"\t \t \t \t"+sArray[5];
		label += "\n"+sArray[3]+"\n"+sArray[9];
		label += "\n"+sArray[7]+"\n"+sArray[11];
		label += "\n"+sArray[8];
		return label;
	}
	
}
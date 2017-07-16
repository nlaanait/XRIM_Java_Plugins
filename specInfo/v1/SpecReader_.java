import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Info;
import ij.text.TextWindow;
import ij.process.ImageProcessor;
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

/**
 * This is a template for a plugin that requires one image to
 * be opened, and takes it as parameter.
 */
public class SpecReader_ implements PlugInFilter {
	ImagePlus imp;

	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
	 * @param image is the currently opened image
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
        return DOES_ALL| NO_CHANGES;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (typically, plugins use
	 * the ImagePlus set above instead).
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		
        // Get Image title and locate the spec file
        int sliceNumber = imp.getCurrentSlice();
        ImageStack stack = imp.getStack();
        String impTitle;
        if(imp.getImageStackSize() != 1 ){
            impTitle = stack.getSliceLabel(sliceNumber);
        } else {
            impTitle = imp.getTitle();
        }
        FileInfo fi = imp.getOriginalFileInfo();
        String specFilePath = getSpecFilePath(fi,impTitle);
        
        // Open spec file and scan to find metadata for the image
        String[] specInfoArray = parseSpecFile(specFilePath,impTitle);
        
        // Make the new info from the spec file
        SpecFileInfo sfi = new SpecFileInfo();
        String info = setSpecFileInfo(sfi,specInfoArray);
	
        // Generate fields of the image to be accessed later
        
        
        // generate dialog if stack of images is querried
        if(imp.getImageStackSize() != 1 ){
            GenericDialog gd = new GenericDialog("Spec Info");
            gd.addMessage("Show info for entire stack ?");
            gd.setOKLabel("Yes");
            gd.setCancelLabel("No");
            gd.addCheckbox("Label Image with Spec Info",false);
            gd.showDialog();
            Boolean labelFlag = gd.getNextBoolean();
            if (gd.wasOKed()){
                String stackInfo = "";
                String[] stackspecInfoArray;
                for(int i=1;i<imp.getStackSize()+1;i++){
                    	impTitle = stack.getSliceLabel(i);
                    	ImageProcessor ipStack = stack.getProcessor(i);
                    	stackspecInfoArray = parseSpecFile(specFilePath,impTitle);
                    	stackInfo += setSpecFileInfo(sfi,stackspecInfoArray);
                    	stackInfo +="\n";
                    	if(labelFlag) setSpecLabel(setSpecFileInfo(sfi,stackspecInfoArray), ipStack);
                    	imp.updateAndDraw();	 
                }
                setShowInfo(stackInfo,impTitle,500,500);
            } else if (gd.wasCanceled()) {
                setShowInfo(info,impTitle,500,500);
                setSpecProperties(sfi);
                if(labelFlag) setSpecLabel(setSpecFileInfo(sfi,specInfoArray),ip);
                imp.updateAndDraw();
            }
        } else {
            setShowInfo(info,impTitle,500,500);
            setSpecProperties(sfi);
            setSpecLabel(info,ip);
            imp.updateAndDraw();
        }
	}
	
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
		String[] specInfoArray = new String[27]; // 26 is # of lines to read from hlog file for imp
		try {
			file = new File(specFilePath);
			Scanner scanner = new Scanner(file);
			while (scanner.hasNextLine()) {
   				String lineFromFile = scanner.nextLine();
   				if(lineFromFile.contains(impTitle)) {
   					specInfoArray[0] = lineFromFile; 
   					for(int i=0; i<26; i++){ 
       						specInfoArray[i+1] = scanner.nextLine();
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
		return specInfoArray;
	}
    
	public String setSpecFileInfo(SpecFileInfo sfi, String[] specInfoArray){
		
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
		sfi.title = pfLine[4];
		sfi.timeStamp = pdLine[2]+" "+pdLine[3]+" "+pdLine[4]+" "+pdLine[5];
		sfi.a = Double.parseDouble(g1Line[1]);
		sfi.b = Double.parseDouble(g1Line[2]);
		sfi.c = Double.parseDouble(g1Line[3]);
		sfi.H = Double.parseDouble(pqLine[1]);
		sfi.K = Double.parseDouble(pqLine[2]);
		sfi.L = Double.parseDouble(pqLine[3]);
		sfi.theta = Double.parseDouble(motorValues1[2]);
		sfi.phi = Double.parseDouble(motorValues1[1]);
		sfi.twotheta = Double.parseDouble(motorValues1[3]);
		sfi.nu = Double.parseDouble(motorValues1[4]);
		sfi.sampleX = Double.parseDouble(motorValues1[6]);
		sfi.sampleY = Double.parseDouble(motorValues1[7]);
		sfi.sampleZ = Double.parseDouble(motorValues1[8]);
		sfi.sampleU = Double.parseDouble(motorValues2[1]);
		sfi.sampleV = Double.parseDouble(motorValues2[2]);
		sfi.sampleW = Double.parseDouble(motorValues2[3]);
		sfi.lensX = Double.parseDouble(motorValues2[6]);
		sfi.lensY = Double.parseDouble(motorValues2[7]);
		sfi.lensZ = Double.parseDouble(motorValues2[8]);
		sfi.exposure = Double.parseDouble(normValues[1]);
		sfi.energy = Double.parseDouble(normValues[6]);
		sfi.trans = Double.parseDouble(normValues[8]);
		
		// Build up the info string
		String info = "Title: " + sfi.title + "\n";
		info += "Time stamp: " + sfi.timeStamp + "\n";
		info += "Exposure (sec) : " + sfi.exposure + "\n";
		info += "(H, K, L): " + sfi.H + " " + sfi.K + " " + sfi.L + "\n";
		info += "Lattice constants (a, b, c) "+ IJ.angstromSymbol +": " + sfi.a + " " + sfi.b + " " + sfi.c + "\n";
		info += "Energy (keV) : " + sfi.energy + "\n";
		info += "Transmission: " + sfi.trans + "\n";
		info += "Sample Rotations (theta, phi) (deg.) : " + sfi.theta + " " + sfi.phi + "\n";
		info += "Detector Rotations (2theta, nu) (deg.) : " + sfi.twotheta + " " + sfi.nu + "\n";
		info += "Sample Translations (X, Y, Z) (mm) : " + sfi.sampleX + " " + sfi.sampleY + " " + sfi.sampleZ + "\n";
		info += "Sample pseudo-Rotations (U, V, W) (deg.) : " + sfi.sampleU + " " + sfi.sampleV + " " + sfi.sampleW + "\n";
		info += "Objective Lens Translations (X, Y, Z) (mm) : " + sfi.lensX + " " + sfi.lensY + " " + sfi.lensZ + "\n";
		
		return info;
	}
	
	public TextWindow setShowInfo(String text, String impTitle, int width, int height ){
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
		imp.setProperty("Sample X ",sfi.sampleX);
	}
	public void setSpecLabel(String specInfo, ImageProcessor ip) {
		String regexp = "[\\n]+";
		String[] sArray = specInfo.split(regexp);
		String label = sArray[2]+"\t \t"+sArray[3]+"\n"+sArray[6]+"\n"+sArray[9];
		ip.setFont(new Font("SansSerif", Font.BOLD, 25));
		ip.setColor(Color.white);
		ip.drawString(label,30,30);
	}
}

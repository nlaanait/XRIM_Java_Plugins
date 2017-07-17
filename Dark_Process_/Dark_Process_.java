import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;

 /** Written by N. Laanait, March 2012, nlaanait@gmail.com
 *
 *
 */

public class Dark_Process_ implements PlugInFilter {
	ImagePlus imp;
        
        private static String strNone="none";
        
        //Saturation threshold of a pixel
        private static double saturation = 65536;
        
        //READ image: Title, ID, and Exposure time (sec).
        private static int READiD;
        private static String READTitle = strNone;
        private double READTime = 1;
        
        
        //Dark images stack: Title & ID, and Exposure time (sec).
        private static int DarkiD;
        private static String DarkTitle = strNone;
        private double DarkTime = 100;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
                
                // Get the titles of all open windows
                int[] winList = WindowManager.getIDList();
                if (winList==null) {
			IJ.error("You gotta have some open images!");
			return DONE;
		}
                String[] titleList= new String[winList.length+1];
		titleList[0] = strNone;
		for (int i=0; i<winList.length; i++) {
			ImagePlus imp2 = WindowManager.getImage(winList[i]);
			if (imp2 != null) {
				titleList[i+1] = imp2.getTitle();
			} else {
				titleList[i+1] = "";
			}
		}
            
                //Fonts for the Dialog box
                Font font1 = new Font("Calibri", Font.ITALIC, 10);
                Font font2 = new Font("Calibri", Font.PLAIN, 15);
                
                //Help file extension
                String help = "file://"+ IJ.getDirectory("imagej")+"/XRIM_helpfiles/"
                        + "Dark_Process_help.txt";
               
                // Ask user to choose READ, Dark images and their time exposures.
		GenericDialog gd = new GenericDialog("Dark Signal Processing");
           //   gd.addMessage("Input: unsigned 16-bit Stack of images & READ I"
          //             + "mage");
                gd.addChoice("READ Image", titleList, READTitle);
                gd.addNumericField("READ Exposure tREAD (sec)",READTime,0);
                gd.addChoice("Dark Stack", titleList, DarkTitle);
                gd.addNumericField("Dark Exposure tDark (sec)",DarkTime,0);
                gd.addNumericField("Dark Brightness Threshold",saturation,0);
                
                gd.addMessage("DARK = ( avg(Dark) - READ ) / (tDark - tREAD),"
                        +"\n where tDark >> tREAD.",font2);
                gd.addMessage(" N.Laanait, nlaanait@gmail.com, June 2012",font1);
                gd.addHelp(help);
                gd.showDialog(); 
                if(gd.wasCanceled())
                    return DONE;
                
                // Get file names and exposures
                int index;
                index = gd.getNextChoiceIndex();
                READTitle = titleList[index];
                READiD = winList[index-1];
                READTime = gd.getNextNumber();
                index = gd.getNextChoiceIndex();
                DarkTitle = titleList[index];
                DarkiD = winList[index-1];        
                DarkTime = gd.getNextNumber();
                saturation = gd.getNextNumber();
                
		return NO_IMAGE_REQUIRED;
	}

	public void run(ImageProcessor ip) {
                
            // Get Dark Stack properties (dimensions, size, etc ...)
            ImagePlus impDark = WindowManager.getImage(DarkiD);
            ImageStack DarkStack = impDark.getStack();
            int width = DarkStack.getProcessor(1).getWidth();
            int height = DarkStack.getProcessor(1).getHeight();
            int dim = width * height;
            
            // Let's assign & declare all the local variables.     
            //The pix-by-pix sum of the stack
            double[] sum;
            sum = new double[dim];
            
            //The pix-by-pix average of the stack
            double[] average;
            average = new double[dim];
            
            //The DARK signal pixel array
            double[] DARKpixels;
            DARKpixels = new double[dim];
                     
            /* Scan the stack to weed out pics w/ pixel > saturation, then sum 
             pix-by-pix through the stack. */
            int k = 0;
            for(int i=1; i<=DarkStack.getSize(); i++){
                  double imgMax = DarkStack.getProcessor(i).getStatistics().max;
                  if (imgMax < saturation){
                      short[] pixels = (short[]) (DarkStack.getPixels(i));
                      for (int j=0; j<dim; j++){
                        sum[j] += (double)(pixels[j] & 0xffff);
                      }
                  } else 
                      k++;
            }    
            
            // Average pix-by-pix
            for(int i=0; i<dim; i++){
                average[i] = (double) (sum[i]/(DarkStack.getSize() - k ));
            }
            
            /* Subtract off READ signal and normalize per exposure time. If 
            a pixel has a value less than 0, then set it to zero.
            */
            ImagePlus impREAD = WindowManager.getImage(READiD);
            ImageProcessor READ_ip = impREAD.getProcessor();
            float[] READpixels = (float[]) ( READ_ip.getPixels() );           
            for(int i =0; i<dim; i++){
                DARKpixels[i] = (double)( (average[i] - READpixels[i])
                        /(DarkTime - READTime) );
                if (DARKpixels[i]< 0)
                    DARKpixels[i]=0;       
            }
            
            // Make DARK image from pixels then show it.  
            ImageProcessor DARK_ip = new FloatProcessor(width, 
                    height,DARKpixels); 
            ImagePlus impDARK = new ImagePlus("DARK",DARK_ip);
            impDARK.show();
            impDARK.draw();
            IJ.run(impDARK, "Enhance Contrast", "saturated=0.35");
   
            //Print out specs of the DARK image.
            ResultsTable rt = new ResultsTable();
            rt.incrementCounter();
            rt.addValue("Mean",DARK_ip.getStatistics().mean);
            rt.addValue("Max",DARK_ip.getStatistics().max);
            rt.addValue("Min",DARK_ip.getStatistics().min);
            rt.addValue("Std.Dev.",DARK_ip.getStatistics().stdDev);
            rt.showRowNumbers(false);
            rt.show("Results");
      
	}

}

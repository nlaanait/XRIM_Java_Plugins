import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;

/** Written by N. Laanait, June 2012, nlaanait@gmail.com
 *
 *
 */
public class Efficiency_Process_ implements PlugInFilter {
	ImagePlus imp;
        
        private static String strNone="none";
        
        //Saturation threshold of a pixel
        private static double saturation = 65536;
        
        //READ image: Title, ID.
        private static int READiD;
        private static String READTitle = strNone;
        
        //DARK image: Title & ID.
        private static int DARKiD;
        private static String DARKTitle = strNone;
        
        //Efficiency Stack: Title, ID, & Exposure Time
        private static int effiD;
        private static String effTitle = strNone;
        private static double effTime = 1;

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
                        + "Efficiency_Process_help.txt";
               
                // Ask user to choose READ, Dark images and their time exposures.
		GenericDialog gd = new GenericDialog("efficiency Signal Processing");
             //   gd.addMessage("Input: unsigned 16-bit Stack of images, DARK"
             //           + "image, and READ Image");
                gd.addChoice("READ Image", titleList, READTitle);
                gd.addChoice("DARK Image", titleList, DARKTitle);
                gd.addChoice("Raw eff. Stack", titleList, effTitle);
                gd.addNumericField("t-eff. Exposure t (sec)",effTime,0);
                gd.addNumericField("eff. Brightness Threshold",saturation,0);
                gd.addMessage("EFF = (avg(Raw_eff)- READ - DARK* t_eff)/ "
                        + "mean (EFF)",font2);
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
                index = gd.getNextChoiceIndex();
                DARKTitle = titleList[index];
                DARKiD = winList[index-1];  
                index = gd.getNextChoiceIndex();
                effTitle = titleList[index];
                effiD = winList[index-1];
                effTime = gd.getNextNumber();
                saturation = gd.getNextNumber();
                
		return NO_IMAGE_REQUIRED;
	}

	public void run(ImageProcessor ip) {
                
            // Get Dark Stack properties (dimensions, size, etc ...)
            ImagePlus impeff = WindowManager.getImage(effiD);
            ImageStack effStack = impeff.getStack();
            int width = effStack.getProcessor(1).getWidth();
            int height = effStack.getProcessor(1).getHeight();
            int dim = width * height;
            
            // Let's assign & declare all the local variables.     
            //The pix-by-pix sum of the stack
            double[] sum;
            sum = new double[dim];
            
            //The pix-by-pix average of the stack
            double[] average;
            average = new double[dim];
            
            //The EFF signal pixel array
            double[] EFFpixels;
            EFFpixels = new double[dim];
            
            /* Scan the raw efficiency stack to weed out pics w/ pix > saturation,
             then sum pix-by-pix through the stack. */
            int k = 0;
            for(int i=1; i<=effStack.getSize(); i++){
                  double imgMax = effStack.getProcessor(i).getStatistics().max;
                  if (imgMax < saturation){
                      short[] pixels = (short[]) (effStack.getPixels(i));
                      for (int j=0; j<dim; j++){
                        sum[j] += (double)(pixels[j] & 0xffff);
                      }
                  } else 
                      k++;
            }    
            
            // Average pix-by-pix
            for(int i=0; i<dim; i++){
                average[i] = (double) (sum[i]/(effStack.getSize() - k ));
            }
            
            /* Subtract off READ signal and DARK signal * exposure time. */ 
            ImagePlus impREAD = WindowManager.getImage(READiD);
            ImageProcessor READ_ip = impREAD.getProcessor();
            float[] READpixels = (float[]) ( READ_ip.getPixels() );
            ImagePlus impDARK = WindowManager.getImage(DARKiD);
            ImageProcessor DARK_ip = impDARK.getProcessor();
            ImageProcessor avg_ip = new FloatProcessor(width, 
                    height,average);
            float[] DARKpixels = (float[]) ( DARK_ip.getPixels() );
            for(int i =0; i<dim; i++){
                EFFpixels[i] = (double) (average[i] - READpixels[i] - 
                        DARKpixels[i] * effTime);
            }
            /* Make EFF image from pixels, divide it out by the mean pix value,
            then show it. */ 
            ImageProcessor EFF_ip = new FloatProcessor(width, 
                    height,EFFpixels); 
            double EFFmean = EFF_ip.getStatistics().mean;
            ImagePlus impEFF = new ImagePlus("EFF",EFF_ip);
            impEFF.show();
            impEFF.draw();
            IJ.run(impEFF, "Divide...","value="+EFFmean);
            IJ.run(impEFF, "Enhance Contrast", "saturated=0.35");
   
            //Print out specs of the DARK image.
            ResultsTable rt = new ResultsTable();
            rt.incrementCounter();
            rt.addValue("Mean",EFF_ip.getStatistics().mean);
            rt.addValue("Max",EFF_ip.getStatistics().max);
            rt.addValue("Min",EFF_ip.getStatistics().min);
            rt.addValue("Std.Dev.",EFF_ip.getStatistics().stdDev);
            rt.showRowNumbers(false);
            rt.show("Results");
      
	}

}

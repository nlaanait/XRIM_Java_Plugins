import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;


public class Read_Process_ implements PlugInFilter {
	ImagePlus imp;
        
        private static String strNone="none";
        
        //Saturation threshold of a pixel
        private static double saturation = 65536;
        
        //READ image: Title, ID.
        private static int ReadiD;
        private static String ReadTitle = strNone;
        
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
                Font font2 = new Font("Calibri", Font.PLAIN, 20);
                
                //Help file extension
                String help = "file://"+ IJ.getDirectory("imagej")+"/XRIM_helpfiles/"
                        + "Read_Process_help.txt";

                
                // Ask user to enter saturation value in Dialog Box.
                GenericDialog gd = new GenericDialog("READ Signal Processing");
            //  gd.addMessage("Input: unsigned 16-bit Stack of Read images.");
                gd.addChoice("Read Stack", titleList, ReadTitle);
                gd.addNumericField("Brightness Threshold",saturation,0);
                gd.addMessage("\t \t READ = Avg(Stack)\n", font2);
            //    gd.addMessage("Images with pixels brighter than threshold are"
            //        + " eliminated.");
                gd.addMessage(" N.Laanait, nlaanait@gmail.com, June 2012",font1);
               
                gd.addHelp(help);
                gd.showDialog();
                if(gd.wasCanceled()){
                    return DONE;  
                }
                
                //Get File name and saturation
                int index;
                index = gd.getNextChoiceIndex();
                ReadTitle = titleList[index];
                ReadiD = winList[index-1];
                saturation = (double) gd.getNextNumber();
                
		return NO_IMAGE_REQUIRED;
	}

	public void run(ImageProcessor ip) {
            
            // Get Stack properties (dimensions, size, etc ...)
            ImagePlus impread = WindowManager.getImage(ReadiD);
            ImageStack stack = impread.getStack();
            int width = stack.getProcessor(1).getWidth();
            int height = stack.getProcessor(1).getHeight();
            int dim = width * height;
            
            // Let's assign & declare all the local variables.     
            //The pix-by-pix sum of the stack
            double[] sum;
            sum = new double[dim];
            
            //The pix-by-pix average of the stack
            double[] average;
            average = new double[dim];
            
            // Scan the stack to weed out pics w/ pixel > saturation, then sum 
            // pix-by-pix through the stack.
            int k = 0;
            for(int i=1; i<=stack.getSize(); i++){
                  double imgMax = stack.getProcessor(i).getStatistics().max;
                  if (imgMax < saturation){
                      short[] pixels = (short[]) (stack.getPixels(i));
                      for (int j=0; j<dim; j++){
                        sum[j] += (double)(pixels[j] & 0xffff);
                      }
                    } else
                      k++;
            }    
            
            // Average pix-by-pix
            for(int i=0; i<dim; i++){
                average[i] = (double) (sum[i]/(stack.getSize() - k ));
            }
            
            // Make READ image from average pixels then show it.  
            ImageProcessor avg_ip = new FloatProcessor(width, 
                    height,average); 
            ImagePlus avgimp = new ImagePlus("READ",avg_ip);
            avgimp.show();
            avgimp.draw();
            IJ.run(avgimp, "Enhance Contrast", "saturated=0.35");
            
            //Print out specs of the READ image.
            ResultsTable rt = new ResultsTable();
            rt.incrementCounter();
            rt.addValue("Mean",avg_ip.getStatistics().mean);
            rt.addValue("Max",avg_ip.getStatistics().max);
            rt.addValue("Min",avg_ip.getStatistics().min);
            rt.addValue("Std.Dev.",avg_ip.getStatistics().stdDev);
            rt.showRowNumbers(false);
            rt.show("Results");
	}
       
}

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.ResultsTable;
import ij.plugin.filter.*;

public class Image_Process_ implements PlugInFilter {
	ImagePlus imp;
        
        private static String strNone="none";
        
        // operation to be performed
        private String operation;
        private int operationCase;
        
        // Conversion factor from ccd counts to photons /
	private static double Conv = 1.0;
        
        //READ image: Title, ID.
        private static int READiD;
        private static String READTitle = strNone;
        
        //DARK image: Title & ID.
        private static int DARKiD;
        private static String DARKTitle = strNone;
        
        //Efficiency Stack: Title, ID
        private static int EFFiD;
        private static String EFFTitle = strNone;
        
        //Raw Image Stack: Title, ID, & Exposure time
        private static int RAWiD;
        private static String RAWTitle = strNone;
        private static double RAWTime = 1.0;
        
        static boolean sumStack = false;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
                
                // Get the titles of all open windows
                int[] winList = WindowManager.getIDList();
                if(winList ==null){
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
                /*Operations to perform. Only java SE7 supports strings in a switch
                statements, hence let's enumerate the cases with numbers.
                */
                String[] operList = {"Sum Stack","Average Stack","Subtract READ","Normalize"};
                int[] operArray = {1,2,3,4};
                
                //Fonts for the Dialog box
                Font font1 = new Font("Calibri", Font.ITALIC, 10);
                Font font2 = new Font("Jokerman", Font.PLAIN, 12);
                
                
               //Help file extension
                String help = "file://"+ IJ.getDirectory("imagej")+"/XRIM_helpfiles/"
                        + "Image_Process_help.txt";
               
                // Ask user to choose what operation(s) to perform.
		GenericDialog gd = new GenericDialog("Raw Image Processing");
                gd.addChoice("Operation",operList,"Normalize");
                gd.addChoice("Raw Image", titleList, RAWTitle);
                gd.addChoice("READ Image", titleList, READTitle);
                gd.addChoice("DARK Image", titleList, DARKTitle);
                gd.addChoice("EFF Image", titleList, EFFTitle);
                gd.addNumericField("Raw image exposure T (sec)",RAWTime,0);
                gd.addNumericField("Conversion Factor Conv (photons/ccd_counts)",Conv,0);
                gd.addMessage("Normalized Image = Conv * (Raw Image- READ - DARK * T)/ "
                        + "EFF",font2);
                gd.addMessage("N.Laanait, nlaanait@gmail.com, June 2012",font1);
                gd.addHelp(help);
                gd.showDialog(); 
                if(gd.wasCanceled())
                    return DONE;
                
                // Get operation type, file names, etc ...
                int index;
                index = gd.getNextChoiceIndex();
                operation = operList[index];
                operationCase = operArray[index];
                index = gd.getNextChoiceIndex();
                RAWTitle = titleList[index];
                RAWiD = winList[index-1];
                index = gd.getNextChoiceIndex();
                if (index ==0) {
                    READTitle = strNone;
                    READiD = 0;
                } else {
                    READTitle = titleList[index];
                    READiD = winList[index-1];  
                }
                index = gd.getNextChoiceIndex();
                if (index ==0) {
                    DARKTitle = strNone;
                    DARKiD = 0;
                } else {
                    DARKTitle = titleList[index];
                    DARKiD = winList[index-1];  
                }
                index = gd.getNextChoiceIndex();
                if (index ==0) {
                    EFFTitle = strNone;
                    EFFiD = 0;
                } else {
                    EFFTitle = titleList[index];
                    EFFiD = winList[index-1];  
                } 
                
                RAWTime = gd.getNextNumber();
                Conv = gd.getNextNumber();
              
                return NO_IMAGE_REQUIRED;
	}

	public void run(ImageProcessor ip) {
            
            // Get Raw Image properties (dimensions, size, etc ...)
            ImagePlus impRAW = WindowManager.getImage(RAWiD);
            ImageStack RAWStack = impRAW.getStack();
            int width = RAWStack.getProcessor(1).getWidth();
            int height = RAWStack.getProcessor(1).getHeight();
            int dim = width * height;
            
            double[] dArray = new double[dim];
            switch (operationCase){
                case 1: 
                   // sumStack(width, height, dim, RAWStack);
                    dArray = sumStack(dim, RAWStack);
                    break;
                case 2:
                    dArray = averageStack(dim, RAWStack);
                    break;
                case 3:
                    dArray = subtractREAD(dim, READiD, RAWStack);
                    break;
                    case 4:
                    dArray = normalize(dim, READiD, DARKiD, EFFiD, RAWStack);
                    break;
            }
            ImageProcessor ipRes =  new FloatProcessor(width,height,dArray);
            ImagePlus impRes = new ImagePlus("Result of "+operation,ipRes);
            impRes.show();
                    
            
	}
        
        //Summing the images in the stack
        public double[] sumStack(int dim, ImageStack RAWStack){
            
            double[] sum;
            sum = new double[dim];
            for(int i=1; i<=RAWStack.getSize(); i++){
                  short[] pixels = (short[]) (RAWStack.getPixels(i));
                      for (int j=0; j<dim; j++){
                        sum[j] += (double)(pixels[j] & 0xffff);
                      }
            }
            return sum;
        }
        
        //Averaging the images in the stack
        public double[] averageStack(int dim, ImageStack RAWStack){
            
            double[] sum;
            sum = new double[dim];
            for(int i=1; i<=RAWStack.getSize(); i++){
                  short[] pixels = (short[]) (RAWStack.getPixels(i));
                      for (int j=0; j<dim; j++){
                        sum[j] += (double)(pixels[j] & 0xffff);
                      }
            }
            double[] average;
            average = new double[dim];
            for(int i=0; i<dim; i++){
                average[i] = (double) (sum[i]/(RAWStack.getSize()));
            }
            return average;
        }
        
        // Subtracting READ signal from every image in a stack the summing them.
        public double[] subtractREAD(int dim, int READiD, ImageStack RAWStack){
            
            ImagePlus impREAD = WindowManager.getImage(READiD);
            ImageProcessor READ_ip = impREAD.getProcessor();
            float[] READpixels = (float[]) READ_ip.getPixels();
            double[] sub,sumSubREAD;
            sub = new double[dim];
            sumSubREAD = new double[dim];
            for(int i=1; i<=RAWStack.getSize(); i++){
                short[] pixels = (short[]) (RAWStack.getPixels(i));
                for(int j=0; j<dim;j++){
                    sub[j] = (double) ((pixels[j] & 0xffff) - READpixels[j]);
                    sumSubREAD[j] += sub[j];
                }
            }
            return sumSubREAD;
        }
        
 

         /* Subtracting READ and DARK signals, dividing by efficiency
           for every image in a stack the summing them.
         */
        public double[] normalize(int dim, int READiD, int DARKiD, int EFFiD,
                                  ImageStack RAWStack){
            
            ImagePlus impREAD = WindowManager.getImage(READiD);
            ImageProcessor READ_ip = impREAD.getProcessor();
            float[] READpixels = (float[]) READ_ip.getPixels();
            ImagePlus impDARK = WindowManager.getImage(DARKiD);
            ImageProcessor DARK_ip = impDARK.getProcessor();
            float[] DARKpixels = (float[]) DARK_ip.getPixels();
            ImagePlus impEFF = WindowManager.getImage(EFFiD);
            ImageProcessor EFF_ip = impEFF.getProcessor();
            float[] EFFpixels = (float[]) EFF_ip.getPixels();
            
            double[] sub;
            double[] norm;
            sub = new double[dim];
            norm = new double[dim];
            for(int i=1; i<=RAWStack.getSize(); i++){
                short[] pixels = (short[]) (RAWStack.getPixels(i));
                for(int j=0; j<dim;j++){
                    sub[j] = (double) ( Conv*( (pixels[j] & 0xffff)
                        - READpixels[j] - DARKpixels[j] * RAWTime )/EFFpixels[j]
                        );
                        
                        
                  //  sub[j] = (double) DARKpixels[j];
                    norm[j] += sub[j];
                }
            }
            return norm;
        }
}

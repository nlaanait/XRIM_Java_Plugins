import ij.io.FileInfo;

public class SpecFileInfo extends FileInfo {

    int dim = 5000;
    public String[] title = new String[dim];
    public double a ;
    public double b ;
    public double c ;
    public double[] H = new double[dim] ;
    public double[] K = new double[dim] ;
    public double[] L = new double[dim] ;
    public double[] energy = new double[dim] ;
    public double[] theta = new double[dim] ;
    public double[] twotheta = new double[dim] ;
    public double[] phi = new double[dim] ;
    public double[] nu = new double[dim] ;
    public double[] sampleX = new double[dim] ;
    public double[] sampleY = new double[dim] ;
    public double[] sampleZ = new double[dim] ;
    public double[] sampleU = new double[dim] ;
    public double[] sampleV = new double[dim] ;
    public double[] sampleW = new double[dim] ;
    public double[] exposure = new double[dim] ;
    public String[] timeStamp = new String[dim] ;
    public double[] lensX = new double[dim] ;
    public double[] lensY = new double[dim] ;
    public double[] lensZ = new double[dim] ;
    public double[] trans = new double[dim] ;   
    public double[] field = new double[dim];
}
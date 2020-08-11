/**
 * Multi_OtsuThreshold_Stack.java
 * 
 * Algorithm: PS.Liao, TS.Chen, and PC. Chung,
 * Journal of Information Science and Engineering, vol 17, 713-727 (2001)
 * 
 * v1:
 * 
 * Coding   : Yasunari Tosa (ytosa@att.net)
 * Date     : Feb. 19th, 2005
 * Site:	: https://github.com/cytomine/Cytomine-core/blob/master/src/java/be/cytomine/processing/image/filters/Multi_OtsuThreshold.java
 * 
 * v2:
 * 
 * MODIFIED	: Steven J. West (stevenjonwest@gmail.com)
 * Date		: August 2019
 * Site		: [PUT GITHUB REPO HERE]
 * 
 * The code has been modified to provide SUPPORT FOR STACK PROCESSING in StereoMate, and to directly modify
 * the input image.  The output no longer generates a new output image or draws the histogram, and no longer shows each
 * region in a new image (although it does still log the segmentation points between populations).
 * 
 */

package io.github.stevenjwest;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Multi_OTSU_Segmentation implements PlugInFilter {

	ImagePlus imp;
  
	static final int NGRAY=256;

 
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + NO_UNDO;
	}

	
	
  public void run(ImageProcessor ip) {
    int MLEVEL = 3; // 3 level
    int bkgroundLevel = 0;
    boolean stack = true;

    GenericDialog gd = new GenericDialog("Select numLevels");
    String [] items= { "2", "3", "4", "5", };
    gd.addChoice("numLevels", items, "4");
    String [] itemsBkground= { "0", "1", "2", "3", };
    gd.addChoice("Background Selection", itemsBkground, "0");
    gd.addCheckbox("stack", true);
    gd.showDialog();
    if (gd.wasCanceled())
      return;

    MLEVEL = gd.getNextChoiceIndex() + 2;
    bkgroundLevel = gd.getNextChoiceIndex();
    stack = gd.getNextBoolean();
    //IJ.log("numLevels set to " + MLEVEL);

    int [] threshold = new int[MLEVEL]; // threshold
    int width = ip.getWidth();
    int height = ip.getHeight();
    
    ImageStack impStack = imp.getStack();
    
    //IJ.showMessage("Stack: "+impStack);
    //IJ.showMessage("impStack size: "+impStack.getSize() );
    
    ////////////////////////////////////////////
    // Build Histogram - stack
    ////////////////////////////////////////////
    float [] histogram = new float[NGRAY];
    
    byte[] pixels = null;
    
    for(int a=1; a<=impStack.getSize(); a++) {
    	//byte[] pixels = (byte[]) ip.getPixels();
    	pixels = (byte[]) impStack.getProcessor(a).getPixels();
    	buildHistogram(histogram, pixels, width, height);
    }
    
    computeProbabilityHistogram(histogram, width, height, impStack.getSize() );

    /////////////////////////////////////////////
    // Build lookup tables from h
    ////////////////////////////////////////////
    float [][] P = new float[NGRAY][NGRAY];
    float [][] S = new float[NGRAY][NGRAY];
    float [][] H = new float[NGRAY][NGRAY];
    buildLookupTables(P, S, H, histogram);

    ////////////////////////////////////////////////////////
    // now M level loop   MLEVEL dependent term
    ////////////////////////////////////////////////////////
    float maxSig = findMaxSigma(MLEVEL, H, threshold);
    String msg = "thresholds: ";;
    for (int i=0; i < MLEVEL; ++i)
      msg += i + "=" + threshold[i] + ", ";
    msg += " maxSig = " + maxSig;
    IJ.log(msg);
    IJ.log("histogram 0: "+histogram[0] + " histogram 10: "+histogram[10]);

    ///////////////////////////////////////////////////////////////
    // show regions works for any MLEVEL
    ///////////////////////////////////////////////////////////////
    showRegions(impStack, MLEVEL, bkgroundLevel, threshold, width, height);
  }

  
  
  /**
   * Build the histogram from the byte array 'pixels', scanning over the image 'width' and 'height'.
   * This method builds a histogram plot window.  It does not include BLACK [0] or WHITE [255] pixels.
   * @param histogram
   * @param pixels
   * @param width
   * @param height
   */
  public void buildHistogram(float [] histogram, byte [] pixels, int width, int height) {
    // note Java byte is signed. in order to make it 0 to 255 you have to
    // do int pix = 0xff & pixels[i];
    for (int i=0; i < width*height; ++i) {
    	if( ((int) (pixels[i]&0xff) != 0) && ((int) (pixels[i]&0xff) != 255) ) {
      histogram[(int) (pixels[i]&0xff)]++;
    	}
    }
    // can ELIMINATE black [0] and white [255] pixels by logically testing the value of:
    	// (int)(pixels[i]&0xff) -> if this is 0 or 255 the pixel is black or white
    	// I think it makes sense for this algorithm to IGNORE these pixels!
    		// AVOID OVER AND UNDER EXPOSURE ISSUES
    
    // This needs to act on the WHOLE HISTOGRAM once completely built of every Z slice
    	// Moved to new method -> computeProbabilityHistogram()
   // note the probability of grey i is h[i]/(width*height)
    //float [] bin = new float[NGRAY];
    //float hmax = 0.f;
    //for (int i=0; i < NGRAY; ++i)
    //{
      //bin[i] = (float) i;
      //histogram[i] /= ((float) (width*height));
      //if (hmax < histogram[i])
	//hmax = histogram[i];
    //}
    //PlotWindow histogramPlot = new PlotWindow("Histogram", "grey", "hist", bin, histogram);
    //histogramPlot.setLimits(0.f, (float) NGRAY, 0.f, hmax);
    //histogramPlot.draw();
  }
  
  public void computeProbabilityHistogram(float[] histogram, int width, int height, int slices) {
	  
	   // note the probability of grey i is h[i]/(width*height)
	// note the probability of grey i is h[i]/(width*height*slices) ??
	    float [] bin = new float[NGRAY];
	    float hmax = 0.f;
	    for (int i=0; i < NGRAY; ++i) {
	      bin[i] = (float) i;
	      histogram[i] /= ((float) (width*height*slices));
	      if (hmax < histogram[i]) {
	    	  hmax = histogram[i];
	      }
	    }
	    //PlotWindow histogramPlot = new PlotWindow("Histogram", "grey", "hist", bin, histogram);
	    //histogramPlot.setLimits(0.f, (float) NGRAY, 0.f, hmax);
	    //histogramPlot.draw();
	  
  }

  
  
  public void buildLookupTables(float [][] P, float [][] S, float [][] H, float [] h) {
    // initialize
    for (int j=0; j < NGRAY; j++)
      for (int i=0; i < NGRAY; ++i)
      {
	P[i][j] = 0.f;
	S[i][j] = 0.f;
	H[i][j] = 0.f;
      }
    // diagonal 
    for (int i=1; i < NGRAY; ++i)
    {
      P[i][i] = h[i];
      S[i][i] = ((float) i)*h[i];
    }
    // calculate first row (row 0 is all zero)
    for (int i=1; i < NGRAY-1; ++i)
    {
      P[1][i+1] = P[1][i] + h[i+1];
      S[1][i+1] = S[1][i] + ((float) (i+1))*h[i+1];
    }
    // using row 1 to calculate others
    for (int i=2; i < NGRAY; i++)
      for (int j=i+1; j < NGRAY; j++)
      {
	P[i][j] = P[1][j] - P[1][i-1];
	S[i][j] = S[1][j] - S[1][i-1];
      }
    // now calculate H[i][j]
    for (int i=1; i < NGRAY; ++i)
      for (int j=i+1; j < NGRAY; j++)
      {
	if (P[i][j] != 0)
	  H[i][j] = (S[i][j]*S[i][j])/P[i][j];
	else
	  H[i][j] = 0.f;
      }

  }

  
  
  public float findMaxSigma(int mlevel, float [][] H, int [] t) {
    t[0] = 0;
    float maxSig= 0.f;
    switch(mlevel)
    {
    case 2:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
      {
	float Sq = H[1][i] + H[i+1][255];
	if (maxSig < Sq)
	{
	  t[1] = i;
	  maxSig = Sq;
	}
      } 
      break;
    case 3:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	{
	  float Sq = H[1][i] + H[i+1][j] + H[j+1][255];
	  if (maxSig < Sq)
	  {
	    t[1] = i;
	    t[2] = j;
	    maxSig = Sq;
	  }
	} 
      break;
    case 4:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	  for (int k = j+1; k < NGRAY-mlevel + 2; k++) // t3
	  {
	    float Sq = H[1][i] + H[i+1][j] + H[j+1][k] + H[k+1][255];
	    if (maxSig < Sq)
	    {
	      t[1] = i;
	      t[2] = j;
	      t[3] = k;
	      maxSig = Sq;
	    }
	  } 
      break;
    case 5:
      for (int i= 1; i < NGRAY-mlevel; i++) // t1
	for (int j = i+1; j < NGRAY-mlevel +1; j++) // t2
	  for (int k = j+1; k < NGRAY-mlevel + 2; k++) // t3
	    for (int m = k+1; m < NGRAY-mlevel + 3; m++) // t4
	  {
	    float Sq = H[1][i] + H[i+1][j] + H[j+1][k] + H[k+1][m] + H[m+1][255];
	    if (maxSig < Sq)
	    {
	      t[1] = i;
	      t[2] = j;
	      t[3] = k;
	      t[4] = m;
	      maxSig = Sq;
	    }
	  } 
      break;
    }
    return maxSig; 
  }

  
  
  public void showRegions(ImageStack impStack, int mlevel, int bkgroundLevel, int [] threshold, int width, int height) {
    //ImagePlus [] region = new ImagePlus[mlevel]; // do not use new imps - set the ips to the current imp!
    // ImageProcessor rip = new ByteProcessor(width, height);
    //for (int i=0; i < mlevel; ++i)
    //{
      // region[i] = NewImage.createByteImage("Region "+i, width, height,1, NewImage.FILL_BLACK);
      // rip[i] = region[i].getProcessor();
    	// rip[i] = new ByteProcessor(width, height);
    //}
	
	  ImageProcessor rip = null;
	  
	  byte [] pixels = null;
	  
	  for(int a=1; a<=impStack.getSize(); a++) {

		  rip = impStack.getProcessor(a);
		  
		  pixels = (byte[]) impStack.getProcessor(a).getPixels();

		  for (int i = 0; i < width*height; ++i)
		  {
			  int pixelValue = 0xff & pixels[i];
			  for (int k = 0; k < mlevel; k++)
			  {
				  if (k <= bkgroundLevel) 
				  {
					  if (pixelValue > 0 ) // k-0 region - BACKGROUND
						  rip.putPixel(i%width, i/width, 0);
				  }
				  else if (k < mlevel-1)
				  {
					  if (pixelValue <= threshold[k+1] && pixelValue > threshold[k]) // k-1 region
						  // rip[k].putPixel(i%width, i/width, pixelValue);
						  rip.putPixel(i%width, i/width, 255); // set pixel value to max val - 255
				  }
				  else // k= mlevel-1 last region
				  {
					  if (pixelValue > threshold[k])
						  // rip[k].putPixel(i%width, i/width, pixelValue);
						  rip.putPixel(i%width, i/width, 255); // set pixel value to max val - 255
				  }
			  }
		  }

	  }
    //for (int i=0; i < mlevel; i++)
      //region[i].show();
    
    //imp.setProcessor(rip);
    
    imp.updateAndDraw();

  }
  
  
  public void showRegions2(int mlevel, int [] threshold, byte [] pixels, int width, int height) {
	    //ImagePlus [] region = new ImagePlus[mlevel]; // do not use new imps - set the ips to the current imp!
	    ImageProcessor [] rip = new ImageProcessor[mlevel];
	    for (int i=0; i < mlevel; ++i)
	    {
	      // region[i] = NewImage.createByteImage("Region "+i, width, height,1, NewImage.FILL_BLACK);
	      // rip[i] = region[i].getProcessor();
	    	rip[i] = new ByteProcessor(width, height);
	    }
	    for (int i = 0; i < width*height; ++i)
	    {
	      int pixelValue = 0xff & pixels[i];
	      for (int k = 0; k < mlevel; k++)
	      {
		if (k < mlevel-1)
		{
		  if (pixelValue <= threshold[k+1] && pixelValue > threshold[k]) // k-1 region
		    // rip[k].putPixel(i%width, i/width, pixelValue);
		  	rip[k].putPixel(i%width, i/width, 255); // set pixel value to max val - 255
		}
		else // k= mlevel-1 last region
		{
		  if (pixelValue > threshold[k])
		    // rip[k].putPixel(i%width, i/width, pixelValue);
		  	rip[k].putPixel(i%width, i/width, 255); // set pixel value to max val - 255
		}
	      }
	    }
	    //for (int i=0; i < mlevel; i++)
	      //region[i].show();

	  }
  
  
}
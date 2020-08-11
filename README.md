# Multi-OTSU Segmentation


**A pre-compiled JAR file of this plugin can be downloaded from the [wiki](https://github.com/stevenjwest/Multi_OTSU_Segmentation/wiki).**


This is an updated version of the Multi-OTSU Threshold plugin by *Yasunari Tosa*.  It provides an implementation of multi-OTSU segmentation in ImageJ:


https://imagej.net/Multi_Otsu_Threshold


Implements an algorithm described in the following paper: https://www.iis.sinica.edu.tw/page/jise/2001/200109_01.pdf


However, this plugin generates as output:


* An image of the historgram

* A separate output stack for each segmentation level 

    + Each image shows the raw pixel values at each segmentation level
    
* A log of the numLevels selected and computed threshold values in the greyscale image


This plugin is desirable for high quality segmentation of fluorescent images:


* Multi-OTSU should allow for multiple peaks in the image histogram to be included in the segmentation:

    + Multiple peaks may represent different intensities of object signals - and standard 2-level OTSU would not capture the multiple peaks in the segmentation.
    
        - For example, if the objects are composed of some high intensity fluorescent objects, and a proportion of lower intensity, multi-OTSU can capture the two intensity levels in the segmented portion of the thresholded image, whereas 2-level OTSU would fail.


This plugins output is not suitable for use with StereoMate:


* SM Threshold Manager requires that the output of a plugin modifies DIRECTLY the input image data:

    + This is because the image is open in an ImageWindowWithPanel, and direct modification is built into the workflow of the Threshold Manager (to save opening new IWPs all the time!).
    
    
Therefore this plugin has **Re-Formed the Multi-OTSU Threshold ImageJ Plugin to directly modify the input image, for use with StereoMate Threshold Manager Plugin:**



This implementation has re-formaulated the original Multi-OTSU plugin to:


* Select the number of levels, and the Background Level
    
    - num of levels will decide how many segmentation levels will be computed
        
    - Background Level will decide from what level the Background Signal is considered up to
        
* Directly modify the input image to generate a segmented image
    
* Not output the histogram, but will still output a log of the data
    
* Work on Image Stacks



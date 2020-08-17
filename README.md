# Multi-OTSU Segmentation


This is an updated version of the Multi-OTSU Threshold plugin by *Yasunari Tosa*.  It provides an implementation of multi-OTSU segmentation in ImageJ:


https://imagej.net/Multi_Otsu_Threshold


Implements an algorithm described in the following paper: https://www.iis.sinica.edu.tw/page/jise/2001/200109_01.pdf


## INSTALLATION


* At time of writing, the ImageJ Updater is down, so the easiest way to use this plugin, please download the pre-compiled JAR from the wiki, and place the JAR into your plugins folder in ImageJ.

    + **A pre-compiled JAR file of this plugin can be downloaded from the [wiki](https://github.com/stevenjwest/Multi_OTSU_Segmentation/wiki).**


* Alternatively, clone this repo and build from source using Maven:


```bash

git clone https://github.com/stevenjwest/Multi_OTSU_Segmentation.git
cd Multi_OTSU_Segmentation
mvn clean package # cleans any target/ directory, then moves through all maven goals upto package

# NB: need Java Version: 1.8.0_101+ for SciJava Maven repository HTTPS support.


# compiled JAR will be available in the target/ directory -> Multi_OTSU_Segmentation-0.1.0.jar

# NB for successful build need:

#$ java -version
#java version "1.8.0_221"
#Java(TM) SE Runtime Environment (build 1.8.0_221-b11)
#Java HotSpot(TM) 64-Bit Server VM (build 25.221-b11, mixed mode)

#$ git --version
#git version 2.27.0

#$ mvn --version
#Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)
#Maven home: /usr/local/Cellar/maven/3.6.3_1/libexec
#Java version: 1.8.0_221, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/jre
#Default locale: en_GB, platform encoding: UTF-8
#OS name: "mac os x", version: "10.13.6", arch: "x86_64", family: "mac"

# NOTE CHECK THE MAVEN JAVA VERSION - if using a version HIGHER than 1.8, should use export JAVA_HOME below to allow maven to see an appropriate
# version of Java:
# export JAVA_HOME var for mvn to use - example code here exports 1.8.0_221:
#export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_221.jdk/Contents/Home/

# you can download Java 1.8 for your machine at this or related link: https://docs.oracle.com/javase/10/install/installation-jdk-and-jre-macos.htm

```



## Why re-implement an existing plugin?


Although Multi-OTSU is an excellent plugin, unfortunately it generates a messy output, including:


* An image of the histogram

* A separate output stack for each segmentation level 

    + Each image shows the raw pixel values at each segmentation level
    
* A log of the numLevels selected and computed threshold values in the greyscale image


However, this algorithm is desirable for high quality segmentation of fluorescent images:


* Multi-OTSU should allow for multiple peaks in the image histogram to be included in the segmentation:

    + Multiple peaks may represent different intensities of object signals - and standard 2-level OTSU would not capture the multiple peaks in the segmentation.
    
        - For example, if the objects are composed of some high intensity fluorescent objects, and a proportion of lower intensity, multi-OTSU can capture the two intensity levels in the segmented portion of the thresholded image, whereas 2-level OTSU would fail.


This plugins output is not suitable for use with [StereoMate](https://github.com/stevenjwest/StereoMate):


* The SM Threshold Manager requires that the output of a plugin modifies DIRECTLY the input image data:

    + This is because the image is open in an ImageWindowWithPanel, and direct modification is necessary to simply update the image without re-opening the large image window.
    
    
Therefore this plugin has **Re-Formed the Multi-OTSU Threshold ImageJ Plugin to directly modify the input image, for use with StereoMate Threshold Manager Plugin:**



This implementation has re-formaulated the original Multi-OTSU plugin to:


* Select the number of levels, and the Background Level
    
    - num of levels will decide how many segmentation levels will be computed
        
    - Background Level will decide from what level the Background Signal is considered up to
        
* Directly modify the input image to generate a segmented image
    
* Not output the histogram, but will still output a log of the data
    
* Work on Image Stacks



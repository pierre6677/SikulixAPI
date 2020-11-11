/*
 * Copyright (c) 2010-2020, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.script;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.sikuli.script.support.Commons;
import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.Finder.Finder2;
import org.sikuli.script.support.RunTime;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

// start of P PETIT
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;
import org.opencv.core.MatOfPoint;
import java.io.FileWriter;   // Import the FileWriter class
import java.io.IOException;  // Import the IOException class to handle errors
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

// end of P PETIT

/**
 * Intended to be used only internally - still public for being backward compatible
 * <p></p>
 * <b>New projects should use class OCR</b>
 * <p></p>
 * Implementation of the Tess4J/Tesseract API
 */
public class TextRecognizer {

  private TextRecognizer() {
  }

  private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");

  private static boolean isValid = false;

  private static int lvl = 3;

  private static final String versionTess4J = "4.5.3";
  private static final String versionTesseract = "4.1.x";

  private OCR.Options options;

  //<editor-fold desc="00 instance, reset">

  /**
   * New TextRecognizer instance using the global options.
   *
   * @return instance
   * @deprecated no longer needed at all
   */
  @Deprecated
  public static TextRecognizer start() {
    return TextRecognizer.get(OCR.globalOptions());
  }

  /**
   * INTERNAL
   *
   * @param options an Options set
   * @return a new TextRecognizer instance
   */
  protected static TextRecognizer get(OCR.Options options) {
    if (!isValid) {
      if (Commons.runningMac()) {
        String libPath = "/usr/local/lib";
        File libTess = new File(libPath, "libtesseract.dylib");
        if (libTess.exists()) {
          String jnaLibPath = System.getProperty("jna.library.path");
          if (jnaLibPath == null) {
            System.setProperty("jna.library.path", libPath);
          } else {
            System.setProperty("jna.library.path", libPath + File.pathSeparator + jnaLibPath);
          }
        } else {
          throw new SikuliXception(String.format("OCR: validate: Tesseract library not in /usr/local/lib"));
        }
      }
      RunTime.loadLibrary(RunTime.libOpenCV);
      isValid = true;
    }

    initDefaultDataPath();

    Debug.log(lvl, "OCR: start: Tess4J %s using Tesseract %s", versionTess4J, versionTesseract);
    if (options == null) {
      options = OCR.globalOptions();
    }
    options.validate();

    TextRecognizer textRecognizer = new TextRecognizer();
    textRecognizer.options = options;

    return textRecognizer;
  }

  public static void validate() {
  }

  private ITesseract getTesseractAPI() {
    try {
      ITesseract tesseract = new Tesseract1();
      tesseract.setOcrEngineMode(options.oem());
      tesseract.setPageSegMode(options.psm());
      tesseract.setLanguage(options.language());
      tesseract.setDatapath(options.dataPath());
      for (Map.Entry<String, String> entry : options.variables().entrySet()) {
        tesseract.setTessVariable(entry.getKey(), entry.getValue());
      }
      if (!options.configs().isEmpty()) {
        tesseract.setConfigs(new ArrayList<>(options.configs()));
      }
      return tesseract;
    } catch (UnsatisfiedLinkError e) {
      String helpURL;
      if (RunTime.get().runningWindows) {
        helpURL = "https://github.com/RaiMan/SikuliX1/wiki/Windows:-Problems-with-libraries-OpenCV-or-Tesseract";
      } else {
        helpURL = "https://github.com/RaiMan/SikuliX1/wiki/macOS-Linux:-Support-libraries-for-Tess4J-Tesseract-4-OCR";
      }
      Debug.error("see: " + helpURL);
      if (RunTime.isIDE()) {
        Debug.error("Save your work, correct the problem and restart the IDE!");
        try {
          Desktop.getDesktop().browse(new URI(helpURL));
        } catch (IOException ex) {
        } catch (URISyntaxException ex) {
        }
      }
      throw new SikuliXception(String.format("OCR: start: Tesseract library problems: %s", e.getMessage()));
    }
  }

  /**
   * @see OCR#reset()
   * @deprecated use OCR.reset() instead
   */
  @Deprecated
  public static void reset() {
    OCR.globalOptions().reset();
  }

  /**
   * @see OCR#status()
   * @deprecated use OCR.status() instead
   */
  @Deprecated
  public static void status() {
    Debug.logp("Global settings " + OCR.globalOptions().toString());
  }
  //</editor-fold>

  //<editor-fold desc="02 set OEM, PSM">

  /**
   * @param oem
   * @return instance
   * @see OCR.Options#oem(OCR.OEM)
   * @deprecated Use options().oem()
   */
  @Deprecated
  public TextRecognizer setOEM(OCR.OEM oem) {
    return setOEM(oem.ordinal());
  }

  /**
   * @param oem
   * @return instance
   * @see OCR.Options#oem(int)
   * @deprecated use OCR.globalOptions().oem()
   */
  @Deprecated
  public TextRecognizer setOEM(int oem) {
    options.oem(oem);
    return this;
  }


  /**
   * @param psm
   * @return instance
   * @see OCR.Options#psm(OCR.PSM)
   * @deprecated use OCR.globalOptions().psm()
   */
  @Deprecated
  public TextRecognizer setPSM(OCR.PSM psm) {
    return setPSM(psm.ordinal());
  }

  /**
   * @param psm
   * @return instance
   * @see OCR.Options#psm(int)
   * @deprecated use OCR.globalOptions().psm()
   */
  @Deprecated
  public TextRecognizer setPSM(int psm) {
    options.psm(psm);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="03 set datapath, language, variable, configs">

  /**
   * @param dataPath
   * @return instance
   * @see OCR.Options#dataPath()
   * @deprecated use OCR.globalOptions().datapath()
   */
  @Deprecated
  public TextRecognizer setDataPath(String dataPath) {
    options.dataPath(dataPath);
    return this;
  }

  /**
   * @param language
   * @return instance
   * @see OCR.Options#language(String)
   * @deprecated use OCR.globalOptions().language()
   */
  @Deprecated
  public TextRecognizer setLanguage(String language) {
    options.language(language);
    return this;
  }

  /**
   * @param key
   * @param value
   * @return instance
   * @see OCR.Options#variable(String, String)
   * @deprecated use OCR.globalOptions().variable(String key, String value)
   */
  @Deprecated
  public TextRecognizer setVariable(String key, String value) {
    options.variable(key, value);
    return this;
  }

  /**
   * @param configs
   * @return instance
   * @see OCR.Options#configs(String...)
   * @deprecated Use OCR.globalOptions.configs(String... configs)
   */
  @Deprecated
  public TextRecognizer setConfigs(String... configs) {
    setConfigs(Arrays.asList(configs));
    return this;
  }

  /**
   * @param configs
   * @return
   * @see OCR.Options#configs(List)
   * @deprecated Use options.configs
   */
  @Deprecated
  public TextRecognizer setConfigs(List<String> configs) {
    options.configs(configs);
    return this;
  }
  //</editor-fold>

  //<editor-fold desc="10 image optimization">

  /**
   * @param size expected font size in pt
   * @see OCR.Options#fontSize(int)
   * @deprecated use OCR.globalOptions().fontSize(int size)
   */
  @Deprecated
  public TextRecognizer setFontSize(int size) {
    options.fontSize(size);
    return this;
  }

  /**
   * @param height of an uppercase X in px
   * @see OCR.Options#textHeight(float)
   * @deprecated use OCR.globalOptions().textHeight(int height)
   */
  @Deprecated
  public TextRecognizer setTextHeight(int height) {
    options.textHeight(height);
    return this;
  }
  


  private BufferedImage optimize(BufferedImage bimg) {
    Mat mimg = Finder2.makeMat(bimg);
	
    Imgproc.cvtColor(mimg, mimg, Imgproc.COLOR_BGR2GRAY);

    // sharpen original image to primarily get rid of sub pixel rendering artifacts
    mimg = unsharpMask(mimg, 3);

    float rFactor = options.factor();

    if (rFactor > 0 && rFactor != 1) {
      Image.resize(mimg, rFactor, options.resizeInterpolation());
    }

    // sharpen the enlarged image again
    mimg = unsharpMask(mimg, 5);

    // invert if font color is said to be light
    if (options.isLightFont()) {
      Core.bitwise_not(mimg, mimg);
    }
    //TODO does it really make sense? invert in case of mainly dark background
//    else if (Core.mean(mimg).val[0] < 127) {
//      Core.bitwise_not(mimg, mimg);
//    }

    BufferedImage optImg = Finder2.getBufferedImage(mimg);
    return optImg;
  }

  /*
   * sharpens the image using an unsharp mask
   */
  private Mat unsharpMask(Mat img, double sigma) {
    Mat blurred = new Mat();
    Imgproc.GaussianBlur(img, blurred, new Size(), sigma, sigma);
    Core.addWeighted(img, 1.5, blurred, -0.5, 0, img);
    return img;
  }
  //</editor-fold>

  //<editor-fold desc="20 text, lines, words - internal use">
  protected <SFIRBS> String readText(SFIRBS from) {
    return doRead(from);
  }

  protected <SFIRBS> List<Match> readLines(SFIRBS from) {
    BufferedImage bimg = Element.getBufferedImage(from);
    return readTextItems(bimg, OCR.PAGE_ITERATOR_LEVEL_LINE);
  }

  protected <SFIRBS> List<Match> readWords(SFIRBS from) {
    BufferedImage bimg = Element.getBufferedImage(from);
    return readTextItems(bimg, OCR.PAGE_ITERATOR_LEVEL_WORD);
  }
  //</editor-fold>

  //<editor-fold desc="30 helper">
  private static void initDefaultDataPath() {
    if (OCR.Options.defaultDataPath == null) {
      // export SikuliX eng.traineddata, if libs are exported as well
      File fTessDataPath = new File(RunTime.get().fSikulixAppPath, "SikulixTesseract/tessdata");
      boolean shouldExport = RunTime.get().shouldExport();
      boolean fExists = fTessDataPath.exists();
      if (!fExists || shouldExport) {
        if (0 == RunTime.get().extractResourcesToFolder("/tessdataSX", fTessDataPath, null).size()) {
          throw new SikuliXception(String.format("OCR: start: export tessdata did not work: %s", fTessDataPath));
        }
      }
      // if set, try with provided tessdata parent folder
      String defaultDataPath;
      if (Settings.OcrDataPath != null) {
        defaultDataPath = new File(Settings.OcrDataPath, "tessdata").getAbsolutePath();
      } else {
        defaultDataPath = fTessDataPath.getAbsolutePath();
      }
      OCR.Options.defaultDataPath = defaultDataPath;
    }
  }

  protected <SFIRBS> String doRead(SFIRBS from) {
    String text = "";
    BufferedImage bimg = Element.getBufferedImage(from);
    try {
		// P PETIT
	  BufferedImage bimgResized = optimize(bimg);
	  bimgResized = removeBoundariesFromBufferedImage( bimgResized);
      text = getTesseractAPI().doOCR(bimgResized).trim().replace("\n\n", "\n");
	  //
    } catch (TesseractException e) {
      Debug.error("OCR: read: Tess4J: doOCR: %s", e.getMessage());
      return "";
    }
    return text;
  }

  protected <SFIRBS> List<Match> readTextItems(SFIRBS from, int level) {
    List<Match> lines = new ArrayList<>();
    BufferedImage bimg = Element.getBufferedImage(from);
	BufferedImage bimgResized = optimize(bimg);
	// add by P PETIT
    bimgResized = removeBoundariesFromBufferedImage( bimgResized);
	//
    List<Word> textItems = getTesseractAPI().getWords(bimgResized, level);
    double wFactor = (double) bimg.getWidth() / bimgResized.getWidth();
    double hFactor = (double) bimg.getHeight() / bimgResized.getHeight();
	for (Word textItem : textItems) {
	  Rectangle boundingBox = textItem.getBoundingBox();
	  Rectangle realBox = new Rectangle(
				  (int) (boundingBox.x * wFactor) - 1,
				  (int) (boundingBox.y * hFactor) - 1,
				  1 + (int) (boundingBox.width * wFactor) + 2,
				  1 + (int) (boundingBox.height * hFactor) + 2);
	  lines.add(new Match(realBox, textItem.getConfidence(), textItem.getText().trim()));
	  Debug.log(3, String.format("%s Confidence: %f x%d y %d height %d width %d \n",
						 textItem.getText(),
						 textItem.getConfidence() ,
						 (int) (boundingBox.x * wFactor) - 1,
						 (int) (boundingBox.y * hFactor) - 1,
						 1 + (int) (boundingBox.width * wFactor) + 2 ,
						 1 + (int) (boundingBox.height * hFactor) + 2));
    }

	return lines;

  }
  // P PETIT my modifications
  // 

public static BufferedImage removeBoundariesFromBufferedImage(BufferedImage bimgResized){
		
	Mat MatImg = matify (bimgResized);
	MatImg = removeContours(MatImg);
	bimgResized = MatToBufferedImage(MatImg);
	return bimgResized;
	
}
public static Mat matify(BufferedImage im) {

    byte[] pixels = ((DataBufferByte) im.getRaster().getDataBuffer()).getData();
    Mat image = new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC1);
	Mat _image = new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC1);
    image.put(0, 0, pixels);
	Imgproc.adaptiveThreshold(image, _image, 100, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
	Mat Hist = new Mat();
	List<Mat> gPlane = new ArrayList<>();
	Core.split(_image, gPlane);
	float[] range = {1, 255};
	MatOfFloat histRange = new MatOfFloat(range);
	boolean accumulate = false;
	Imgproc.calcHist(gPlane, new MatOfInt(0), new Mat(), Hist, new MatOfInt(255), histRange, accumulate);
	Core.MinMaxLocResult MinMax = Core.minMaxLoc​(Hist);

	Debug.log(3, String.format("max value : %d  gray level : %d y ",
						  (int) MinMax.maxVal,
						  (int) MinMax.maxLoc.y));

	int imageBackground = (int) MinMax.maxLoc.y -10;					  
	Imgproc.threshold(_image, _image, imageBackground, 255, Imgproc.THRESH_BINARY_INV);
	imageToPNG("image_threshold_", _image);
    return _image;

}
public static BufferedImage MatToBufferedImage(Mat MatImage){
	int type;
	if(MatImage.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else
            type = BufferedImage.TYPE_3BYTE_BGR;

	BufferedImage outputBufferedImage = new BufferedImage(MatImage.width(), MatImage.height(), type);

	byte[] data = ((DataBufferByte) outputBufferedImage.getRaster().getDataBuffer()).getData();
	MatImage.get(0, 0, data);
	return outputBufferedImage;
}

private static Mat removeContours(Mat mat) {
	Mat hierarchy = new Mat();
	Mat image = mat.clone();
	List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	int[] current_hierarchy = new int[4];
	Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
	Rect rect = null;
	double maxArea = 300;
	ArrayList<Rect> arr = new ArrayList<Rect>();
	Scalar color = new Scalar(150);
	for (int i = 0; i < contours.size(); i++) {

		Mat contour = contours.get(i);
		double contourArea = Imgproc.contourArea(contour);
		hierarchy.get(0, i, current_hierarchy);
		if (contourArea > maxArea) {
			//rect = Imgproc.boundingRect(contours.get(i));
			rect = Imgproc.boundingRect(contours.get(i));
			int boxArea = (int) rect.height * (int) rect.width;
			Debug.log(3, String.format("level %d Next %d Previous %d First_Child %d Parent %d area %d rectArea %d x %d y %d height %d width %d", 
											i+1, (int) current_hierarchy[0],(int) current_hierarchy[1], 
											(int) current_hierarchy[2], (int) current_hierarchy[3],
											(int)contourArea, 
											boxArea,
											(int)rect.x,
											(int)rect.y,
											(int)rect.height,
											(int)rect.width
											));
			arr.add(rect);
			color = new Scalar(0);
			Imgproc.drawContours(image, contours, i, color, 2);		
		} 
	}
	imageToPNG("image_contour_", image);

	return image;

}

private static String imageToPNG(String fileName, Mat image){
	String dir = System.getProperty("user.dir");
	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	String Name = dir + "\\"+ fileName +"_"+ sdf.format(timestamp) +".jpg";
	Imgcodecs.imwrite(Name, image);
	return Name;
}

// end of P PETIT modifications

  //</editor-fold>

  //<editor-fold desc="99 obsolete">

  /**
   * @return the current screen resolution in dots per inch
   * @deprecated Will be removed in future versions<br>
   * use Toolkit.getDefaultToolkit().getScreenResolution()
   */
  @Deprecated
  public int getActualDPI() {
    return Toolkit.getDefaultToolkit().getScreenResolution();
  }

  /**
   * @param simg
   * @return the text read
   * @see OCR#readText(Object)
   * @deprecated use OCR.readText() instead
   */
  @Deprecated
  public String doOCR(ScreenImage simg) {
    return OCR.readText(simg);
  }

  /**
   * @param bimg
   * @return the text read
   * @see OCR#readText(Object)
   * @deprecated use OCR.readText() instead
   */
  @Deprecated
  public String doOCR(BufferedImage bimg) {
    return OCR.readText(bimg);
  }

  /**
   * @param simg
   * @return text
   * @see OCR#readText(Object)
   * @deprecated use OCR.readText() instead
   */
  @Deprecated
  public String recognize(ScreenImage simg) {
    BufferedImage bimg = simg.getImage();
    return OCR.readText(bimg);
  }

  /**
   * @param bimg
   * @return text
   * @see OCR#readText(Object)
   * @deprecated use OCR.readText() instead
   */
  @Deprecated
  public String recognize(BufferedImage bimg) {
    return OCR.readText(bimg);
  }
  //</editor-fold>

}

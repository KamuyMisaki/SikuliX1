/*
 * Copyright (c) 2010-2020, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.script;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.sikuli.basics.Debug;
import org.sikuli.basics.Settings;
import org.sikuli.script.support.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * INTERNAL: An abstract super-class for {@link Region}, {@link Location}, {@link Image} ... .
 * <br>
 * <p>BE AWARE: This class cannot be used as such (cannot be instantiated)
 * <br>... instead use the sub-classes as needed</p>
 * NOTES:
 * <br>- the intention is, to have only one implementation for features,
 * that are the same for all or some pixel/screen related classes
 * <br>- the implementation here is ongoing beginning with version 2.0.2 and hence not complete yet
 * <br>- you might get <b>not-implemented exceptions</b> until complete
 */
public abstract class Element {

  protected static final int logLevel = 3;

  protected static void log(int level, String message, Object... args) {
    String className = Thread.currentThread().getStackTrace()[2].getClassName();
    String caller = className.substring(className.lastIndexOf(".") + 1);
    Debug.logx(level, caller + ": " + message, args);
  }

  protected static void terminate(String message, Object... args) {
    String className = Thread.currentThread().getStackTrace()[2].getClassName();
    String caller = className.substring(className.lastIndexOf(".") + 1);
    throw new SikuliXception(caller + ": " + String.format(message, args));
  }

  public String toString() {
    String clazz = this.getClass().getSimpleName();
    String fileName = "";
    if (fileName() != null) {
      fileName = "(" + new File(fileName()).getName() + ")";
    }
    return String.format("[Element: %s(%s) (%d,%d %dx%d) %s]", clazz, sourceClass, x, y, w, h, fileName);
  }

  //<editor-fold desc="000 Fields x, y - top left corner or point (0 for Images)">

  /**
   * @return x of top left corner or point
   */
  public Element setX(int x) {
    this.x = x;
    return this;
  }

  /**
   * sets x to the given value.
   * convenience: allow calculated decimals
   *
   * @param x new x
   * @return this
   */
  public Element setX(double x) {
    this.x = (int) x;
    return this;
  }

  /**
   * @return x of top left corner or point
   */
  public int getX() {
    return x;
  }

  /**
   * x (horizontal) of top left corner or point.
   */
  public int x = 0;

  /**
   * sets y to the given value
   *
   * @param y new y
   * @return this
   */
  public Element setY(int y) {
    this.y = y;
    return this;
  }

  /**
   * sets y to the given value
   * convenience: allow calculated decimals
   *
   * @param y new y
   * @return this
   */
  public Element setY(double y) {
    this.y = (int) y;
    return this;
  }

  /**
   * @return y of top left corner or point
   */
  public int getY() {
    return y;
  }

  /**
   * y (vertical) of top left corner or point
   */
  public int y = 0;

  /**
   * sets the coordinates/top left corner to the given values
   *
   * @param x new x
   * @param y new y
   * @return this
   */
  public Element set(int x, int y) {
    this.x = x;
    this.y = y;
    return this;
  }

  /**
   * sets the coordinates/top left corner to the given values.
   * convenience: allow calculated decimals
   *
   * @param x new x double
   * @param y new y double
   * @return this
   */
  public Element set(double x, double y) {
    set((int) x, (int) y);
    return this;
  }

  /**
   * get as AWT point
   *
   * @return Point
   */
  public Point getPoint() {
    return new Point(x, y);
  }

  protected void copyElementRectangle(Element element) {
    x = element.x;
    y = element.y;
    w = element.w;
    h = element.h;
  }
  //</editor-fold>

  //<editor-fold desc="001 Fields w, h - dimension (0 for points)">

  /**
   * @return width of the Element (0 for Points)
   */
  public int getW() {
    return w;
  }

  public void setW(int w) {
    this.w = w;
  }

  /**
   * Width of the Element (0 for Points)
   */
  public int w = 0;

  /**
   * @return height of Element (0 for Points)
   */
  public int getH() {
    return h;
  }

  public void setH(int h) {
    this.h = h;
  }

  /**
   * Height of the Element (0 for Points)
   */
  public int h = 0;
  //</editor-fold>

  //<editor-fold desc="002 Fields rectangle">
  protected void setSize(Mat mat) {
    w = mat.cols();
    h = mat.rows();
  }

  /**
   * @return size of image
   */
  public Dimension getSize() {
    return new Dimension(w, h);
  }

  /**
   * @return the AWT Rectangle of the region
   */
  public Rectangle getRect() {
    return new Rectangle(x, y, w, h);
  }

  /**
   * set the regions position/size.
   * <br>this might move the region even to another screen
   *
   * @param r the AWT Rectangle to use for position/size
   * @return the region itself
   */
  public Element setRect(Rectangle r) {
    return setRect(r.x, r.y, r.width, r.height);
  }

  /**
   * set the regions position/size.
   * <br>this might move the region even to another screen
   *
   * @param X new x of top left corner
   * @param Y new y of top left corner
   * @param W new width
   * @param H new height
   * @return the region itself
   */
  public Element setRect(int X, int Y, int W, int H) {
    x = X;
    y = Y;
    w = W > 1 ? W : 1;
    h = H > 1 ? H : 1;
    initScreen(null);
    return this;
  }

  /**
   * set the regions position/size.
   * <br>this might move the region even to another screen
   *
   * @param r the region to use for position/size
   * @return the region itself
   */
  public Element setRect(Region r) {
    return setRect(r.x, r.y, r.w, r.h);
  }
  //</editor-fold>

  //<editor-fold desc="003 Fields pixel content">

  public static <SUFEBM> Image getImage(SUFEBM target) {
    if (target instanceof Image) {
      return (Image) target;
    } else if (target instanceof Pattern) {
      return ((Pattern) target).getImage();
    }
    return new Image(target);
  }

  public static <SUFEBM> BufferedImage getBufferedImage(SUFEBM whatEver) {
    return getImage(whatEver).getBufferedImage();
  }

  public Image getImage() {
    return (Image) this;
  }

  public BufferedImage getBufferedImage() {
    return SXOpenCV.makeBufferedImage(getImage().getContent());
  }

  /**
   * check whether image has pixel content<br>
   *
   * @return true if has pixel content
   */
  public boolean isValid() {
    if (isOnScreen() || null == imageURL) {
      if (content.empty() && this instanceof ScreenImage) {
        content = ((ScreenImage) this).makeMat();
      }
      return !content.empty();
    }
    return Image.ImageCache.isValid(imageURL);
  }

  public Mat getContent() {
    if (isOnScreen() || null == imageURL) {
      if (content.empty() && this instanceof ScreenImage) {
        content = ((ScreenImage) this).makeMat();
      }
      return content;
    }
    Mat mat = Image.ImageCache.get(imageURL);
    return mat;
  }

  public Mat cloneContent() {
    return getContent().clone();
  }

  public void setContent(Mat mat) {
    content = mat;
  }

  protected void copyElementContent(Element element) {
    setContent(element.cloneContent());
  }

  public boolean isFakeImage() {
    return getName().equals(FAKE_IMAGE);
  }

  public void asFakeImage() {
    setName(FAKE_IMAGE);
  }

  private static final String FAKE_IMAGE = "Internal-Fake-Image";

  protected static void reload(String fpImage) {
    URL url = evalURL(new File(fpImage));
    if (url != null) {
      Image image = new Image();
      image.asFakeImage();
      image.createContent(url);
    }
  }

  protected static URL evalURL(File imagefile) {
    URL url = null;
    if (imagefile.isAbsolute() || imagefile.getPath().startsWith("\\")) {
      try {
        url = imagefile.toURI().toURL();
      } catch (MalformedURLException e) {
      }
    } else {
      url = ImagePath.find(imagefile.getPath());
    }
    if (url == null || !new File(url.getPath()).exists()) {
      return null;
    }
    return url;
  }

  protected void createContent(URL url) {
    if ("file".equals(url.getProtocol())) {
      try {
        url = new File(url.getPath()).getCanonicalFile().toURI().toURL();
      } catch (IOException e) {
        terminate("content: io error: %s", url);
      }
    }
    byte[] bytes = null;
    try {
      InputStream inputStream = url.openStream();
      imageURL = url;
      if (!isFakeImage() && Image.ImageCache.isValid(url)) {
        setSize(Image.ImageCache.get(url));
        return;
      }
      bytes = inputStream.readAllBytes();
    } catch (IOException e) {
      terminate("content: io error: %s", url);
    }
    MatOfByte matOfByte = new MatOfByte();
    matOfByte.fromArray(bytes);
    Mat content = Imgcodecs.imdecode(matOfByte, -1);
    if (!isFakeImage()) {
      setSize(content);
    }
    Image.ImageCache.put(url, content);
  }

  private Mat content = SXOpenCV.newMat();

  public double diffPercentage(Image otherImage) {
    if (SX.isNull(otherImage)) {
      return 1.0;
    }
    return SXOpenCV.diffPercentage(getContent(), otherImage.getContent());
  }

  private final static String PNG = "png";
  private final static String dotPNG = "." + PNG;

  public static URL makeURL(Object item) {
    return makeURL(item, RunTime.get().fWorkDir);
  }

  public static URL makeURL(Object item, Object base) {
    URL url = null;
    File file = null;
    if (item instanceof String) {
      String itemName = (String) item;
      try {
        URI uri = new URI(itemName);
        if (uri.getScheme() != null && !"file".equals(uri.getScheme())) {
          try {
            return uri.toURL();
          } catch (MalformedURLException e) {
          }
        }
      } catch (URISyntaxException e) {
      }
      file = new File(itemName);
    } else if (item instanceof File) {
      file = (File) item;
    }
    if (file == null) {
      return null;
    }
    if (!file.isAbsolute() && !file.getPath().startsWith("\\") && base != null) {
      String itemName = file.getPath();
      if (base instanceof URL) {
        URL baseURL = (URL) base;
        if ("file".equals(baseURL.getProtocol())) {
          file = new File(new File(baseURL.getPath()), itemName);
        } else
          terminate("makeURL: not supported for non-File base: %s", base);
      }
      if (base instanceof String) {
        file = new File((String) base, itemName);
      }
      if (base instanceof File) {
        file = new File((File) base, itemName);
      }
    }
    try {
      url = file.toURI().toURL();
    } catch (MalformedURLException e) {
    }
    return url;
  }

  public URL url() {
    return imageURL;
  }

  public void url(URL url) {
    if (url.getProtocol().equals("file")) {
      try {
        url = new File(url.getPath()).getCanonicalFile().toURI().toURL();
      } catch (IOException e) {
        terminate("url: problem with file: %s", e.getMessage());
      }
    }
    this.imageURL = url;
  }

  public void url(String fileName) {
    String problem = null;
    try {
      fileName = new File(fileName).getCanonicalPath();
    } catch (IOException e) {
      problem = e.getMessage();
    }
    try {
      imageURL = new File(fileName).toURI().toURL();
    } catch (MalformedURLException e) {
      problem = e.getMessage();
    }
    if (null != problem) {
      terminate("url: problem with file: %s (%s)", fileName, problem);
    }
  }

  private URL imageURL = null;

  public String fileName() {
    File file = file();
    return (file == null ? null : file.getAbsolutePath());
  }

  public File file() {
    if (imageURL != null) {
      return new File(imageURL.getPath());
    }
    return null;
  }
  //</editor-fold>

  //<editor-fold desc="005 Fields name, lastMatch, ...">

  /**
   * INTERNAL: to identify an Element
   *
   * @return the name
   */
  public String getName() {
    if (name.isEmpty() && fileName() != null) {
      return new File(fileName()).getName();
    }
    return name;
  }

  /**
   * INTERNAL: to identify an Element
   *
   * @param name to be used
   */
  public void setName(String name) {
    this.name = name;
  }

  private String name = "";
  protected String sourceClass = "";

  protected Match lastMatch = null;
  protected Iterator<Match> lastMatches = null;

  //TODO need to be cloned?
  protected void copyMatchAttributes(Element element) {
    lastMatch = element.lastMatch;
    lastMatches = element.lastMatches;
  }

  protected long lastSearchTime = -1;
  protected long lastFindTime = -1;
  protected long lastSearchTimeRepeat = -1;

  /**
   * a find operation saves its match on success in this region/image.
   * <br>... unchanged if not successful
   *
   * @return the Match object from last successful find
   */
  public Match getLastMatch() {
    return lastMatch;
  }

  public Match match() {
    if (null != lastMatch) {
      return lastMatch;
    }
    return new Match(this);
  }

  public Match match(Match match) {
    lastMatch = match;
    return match;
  }

  /**
   * a searchAll operation saves its matches on success in this region/image
   * <br>... unchanged if not successful
   *
   * @return a Match-Iterator of matches from last successful searchAll
   */
  public Iterator<Match> getLastMatches() {
    return lastMatches;
  }
  //</editor-fold>

  //<editor-fold desc="007 Fields wait observe timing">
  protected void copyTimingAttributes(Element element) {
    autoWaitTimeout = element.autoWaitTimeout;
    waitScanRate = element.waitScanRate;
    observeScanRate = element.observeScanRate;
    repeatWaitTime = element.repeatWaitTime;
  }

  /**
   * the time in seconds a find operation should wait.
   * <br>for the appearence of the target in this element
   * <br>initial value is {@link Settings#AutoWaitTimeout} (default 3) - ignored for non-screen
   *
   * @param sec seconds
   */
  public void setAutoWaitTimeout(double sec) {
    autoWaitTimeout = sec;
  }

  /**
   * current autoWaitTimeout
   *
   * @return value in seconds
   * @see #setAutoWaitTimeout(double)
   */
  public double getAutoWaitTimeout() {
    return autoWaitTimeout;
  }

  private double autoWaitTimeoutDefault = Settings.AutoWaitTimeout;
  private double autoWaitTimeout = autoWaitTimeoutDefault;

  /**
   * @return current WaitScanRate as rate per second
   * @see #setWaitScanRate(float)
   */
  public float getWaitScanRate() {
    return waitScanRate;
  }

  /**
   * the rate how often an image search should be repeated in this element.
   * <br>pause between subsequent searches is 1/waitScanrate in seconds
   * <br>initial value is {@link Settings#WaitScanRate} (default 3) - ignored for non-screen
   *
   * @param waitScanRate decimal number as rate per second
   */
  public void setWaitScanRate(float waitScanRate) {
    this.waitScanRate = waitScanRate;
    scanWait = 1000 / waitScanRate;
  }

  private void waitAfterScan(long before) {
    double time = scanWait - new Date().getTime() + before;
    if (time < 0.01) {
      return;
    }
    wait(time);
  }

  private float waitScanRateDefault = Settings.WaitScanRate;
  private double scanWait = 1000 / waitScanRateDefault;
  private float waitScanRate = waitScanRateDefault;


  /**
   * @return current observeScanRate as rate per second
   * @see #setObserveScanRate(float)
   */
  public float getObserveScanRate() {
    return observeScanRate;
  }

  /**
   * set the individual observeScanRate
   *
   * @param observeScanRate decimal number as rate per second
   */
  public void setObserveScanRate(float observeScanRate) {
    this.observeScanRate = observeScanRate;
  }

  private float observeScanRateDefault = Settings.ObserveScanRate;
  private float observeScanRate = observeScanRateDefault;

  /**
   * INTERNAL: Observe
   *
   * @return the regions current RepeatWaitTime time in seconds
   */
  public int getRepeatWaitTime() {
    return repeatWaitTime;
  }

  /**
   * INTERNAL: Observe set the regions individual WaitForVanish
   *
   * @param time in seconds
   */
  public void setRepeatWaitTime(int time) {
    repeatWaitTime = time;
  }

  private int repeatWaitTimeDefault = Settings.RepeatWaitTime;
  private int repeatWaitTime = repeatWaitTimeDefault;
  //</editor-fold>

  //<editor-fold desc="008 Fields FindFailed Settings">
  protected void copyFindFailedSettings(Element element) {
    throwException = element.throwException;
    findFailedResponse = element.findFailedResponse;
    findFailedHandler = element.findFailedHandler;
  }

  /**
   * true - should throw {@link FindFailed} if not found in this region<br>
   * false - do not abort script on FindFailed (might lead to NPE's later)<br>
   * default: {@link Settings#ThrowException}<br>
   * sideEffects: {@link #setFindFailedResponse(FindFailedResponse)} true:ABORT, false:SKIP<br>
   * see also: {@link #setFindFailedResponse(FindFailedResponse)}<br>
   * and: {@link #setFindFailedHandler(Object)}
   *
   * @param flag true/false
   */
  public void setThrowException(boolean flag) {
    throwException = flag;
    if (throwException) {
      findFailedResponse = FindFailedResponse.ABORT;
    } else {
      findFailedResponse = FindFailedResponse.SKIP;
    }
  }

  /**
   * reset to default {@link #setThrowException(boolean)}
   */
  public void resetThrowException() {
    setThrowException(throwExceptionDefault);
  }

  /**
   * current setting {@link #setThrowException(boolean)}
   *
   * @return true/false
   */
  public boolean getThrowException() {
    return throwException;
  }

  private boolean throwExceptionDefault = Settings.ThrowException;
  private boolean throwException = throwExceptionDefault;

  /**
   * FindFailedResponse.<br>
   * ABORT - abort script on FindFailed <br>
   * SKIP - ignore FindFailed<br>
   * PROMPT - display prompt on FindFailed to let user decide how to proceed<br>
   * RETRY - continue to wait for appearence after FindFailed<br>
   * HANDLE - call a handler on exception {@link #setFindFailedHandler(Object)}<br>
   * default: ABORT<br>
   * see also: {@link #setThrowException(boolean)}
   *
   * @param response {@link FindFailed}
   */
  public void setFindFailedResponse(FindFailedResponse response) {
    findFailedResponse = response;
  }

  /**
   * reset to default {@link #setFindFailedResponse(FindFailedResponse)}
   */
  public void resetFindFailedResponse() {
    setFindFailedResponse(findFailedResponseDefault);
  }

  /**
   * @return the current setting {@link #setFindFailedResponse(FindFailedResponse)}
   */
  public FindFailedResponse getFindFailedResponse() {
    return findFailedResponse;
  }

  private FindFailedResponse findFailedResponseDefault = FindFailed.getResponse();
  private FindFailedResponse findFailedResponse = findFailedResponseDefault;

  public void setFindFailedHandler(Object handler) {
    findFailedResponse = FindFailedResponse.HANDLE;
    findFailedHandler = FindFailed.setHandler(handler, ObserveEvent.Type.FINDFAILED);
    log(logLevel, "Setting FindFailedHandler");
  }

  public Object getFindFailedHandler() {
    return findFailedHandler;
  }

  private Object findFailedHandler = FindFailed.getFindFailedHandler();
  //</editor-fold>

  //<editor-fold desc="009 Fields Pattern aspects">
  private <PE> void copyPatternAttributes(PE source) {
    if (source instanceof Pattern) {
      similarity = ((Pattern) source).getSimilar();
      offset(((Pattern) source).getTargetOffset());
      waitAfter = ((Pattern) source).waitAfter();
    } else if (source instanceof Image) {
      similarity = ((Image) source).similarity();
      offset(((Image) source).offset());
      waitAfter = ((Image) source).waitAfter();
    }
  }

  private double similarity = Settings.MinSimilarity;

  /**
   * Get the value of similarity
   *
   * @return the value of similarity
   */
  public double similarity() {
    return similarity;
  }

  /**
   * Set the value of similarity
   *
   * @param similarity new value of similarity
   * @return the image
   */
  public void similarity(double similarity) {
    this.similarity = similarity;
  }

  private int offsetX = 0;
  private int offsetY = 0;

  /**
   * Get the value of offset
   *
   * @return the value of offset
   */
  public Location offset() {
    return new Location(offsetX, offsetY);
  }

  /**
   * Set the value of offset
   *
   * @param offset new value of offset
   * @return the image
   */
  public void offset(Location offset) {
    this.offsetX = offset.x;
    this.offsetY = offset.y;
  }

  private int waitAfter;

  /**
   * Get the value of waitAfter
   *
   * @return the value of waitAfter
   */
  public int waitAfter() {
    return waitAfter;
  }

  /**
   * Set the value of waitAfter
   *
   * @param waitAfter new value of waitAfter
   * @return the image
   */
  public void waitAfter(int waitAfter) {
    this.waitAfter = waitAfter;
  }
  //</editor-fold>

  //<editor-fold desc="010 global features">
  protected void copyElementAttributes(Element element) {
    name = element.name;
    copyElementRectangle(element);
    copyElementContent(element);
    copyMatchAttributes(element);
    copyTimingAttributes(element);
    copyFindFailedSettings(element);
  }

  protected static boolean isValidImageFilename(String fname) {
    String validEndings = ".png.jpg.jpeg";
    String ending = FilenameUtils.getExtension(fname);
    return !ending.isEmpty() && validEndings.contains(ending.toLowerCase());
  }

  protected static String getValidImageFilename(String fname) {
    if (isValidImageFilename(fname)) {
      return fname;
    }
    return fname + ".png";
  }

  protected Location getLocationFromTarget(Object target) throws FindFailed {
    if (!(target instanceof ArrayList)) {
      if (target instanceof Element && ((Element) target).isOnScreen()) {
        return ((Element) target).getTarget();
      }
      Image image = Image.getImage(target);
      if (image.isValid()) {
        Match match = wait(image);
        if (match != null) {
          if (isOtherScreen()) {
            return (Location) match.getTarget().setOtherScreen(getScreen());
          } else {
            return match.getTarget();
          }
        }
        return null;
      }
      //TODO allow AWT elements: Rectangle, Point, ...
    } else {
      ArrayList parms = (ArrayList) target;
      if (parms.size() == 1) {
        target = parms.get(0);
      } else if (parms.size() == 2) {
        if (parms.get(0) instanceof Integer && parms.get(0) instanceof Integer) {
          return new Location((Integer) parms.get(0), (Integer) parms.get(1));
        }
      } else {
        return null;
      }
    }
    return null;
  }

  // to avoid NPE for Regions being outside any screen
  protected IRobot getRobotForElement() {
    if (getScreen() == null) {
      return Screen.getGlobalRobot();
    }
    return getScreen().getRobot();
  }

  /**
   * convenience method
   *
   * @return the region's center
   */
  public Location getTarget() {
    return getCenter();
  }

  /**
   * @return the center pixel location of the Element or the Point
   */
  public Location getCenter() {
    return checkAndSetRemote(new Location(getX() + getW() / 2, getY() + getH() / 2));
  }

  /**
   * Moves the region to the area, whose center is the given location
   *
   * @param loc the location which is the new center of the region
   * @return the region itself
   */
  public Element setCenter(Location loc) {
    Location c = getCenter();
    x = x - c.x + loc.x;
    y = y - c.y + loc.y;
    initScreen(null);
    return this;
  }

  /**
   * WARNING: wait(long timeout) is taken by Java Object as final. This method catches any interruptedExceptions
   *
   * @param timeout The time to wait
   */
  public void wait(double timeout) {
    try {
      Thread.sleep((long) (timeout * 1000L));
    } catch (InterruptedException e) {
    }
  }

  protected boolean isPoint() {
    return false;
  }

  protected boolean isEmpty() {
    return w <= 1 && h <= 1;
  }
  //</editor-fold>

  //<editor-fold desc="011 save content to file">
  public String save(String name) {
    return save(name, ImagePath.getBundlePath());
  }

  public String save(String name, String path) {
    if (!isValid() || path == null) {
      return null;
    }
    File fImg = new File(path, name);
    return save(fImg);
  }

  public String save(File imageFile) {
    boolean imwrite = false;
    File parent = imageFile.getParentFile();
    String name = getValidImageFilename(imageFile.getName());
    imageFile = new File(parent, name);
    try {
      FileUtils.forceMkdir(parent);
      imwrite = Imgcodecs.imwrite(imageFile.getAbsolutePath(), getContent());
    } catch (IOException e) {
    }
    if (imwrite) {
      Debug.log(3, "Image::save: %s", imageFile);
    } else {
      Debug.error("Image::save: %s did not work", imageFile);
      return null;
    }
    return imageFile.getAbsolutePath();
  }
  //</editor-fold>

  //<editor-fold desc="015 Screen related">
  protected boolean isOnScreen() {
    return isScreenElement;
  }

  protected void onScreen(boolean state) {
    isScreenElement = state;
  }

  private boolean isScreenElement = true;

  //TODO revise local/remote screen handling
  protected Location checkAndSetRemote(Location loc) {
    if (!isOtherScreen()) {
      return loc;
    }
    return (Location) loc.setOtherScreen(getScreen());
  }

  //TODO revise initScreen/initRegion

  /**
   * INTERNAL: USE
   *
   * @param iscr screen
   */
  public void initScreen(IScreen iscr) {
    // check given screen first
    Rectangle rect, screenRect;
    IScreen screen, screenOn;
    if (iscr != null) {
      if (iscr.isOtherScreen()) {
        if (x < 0) {
          w = w + x;
          x = 0;
        }
        if (y < 0) {
          h = h + y;
          y = 0;
        }
        setScreen(iscr);
        setOtherScreen(iscr);
        return;
      }
      if (iscr.getID() > -1) {
        rect = regionOnScreen(iscr);
        if (rect != null) {
          x = rect.x;
          y = rect.y;
          w = rect.width;
          h = rect.height;
          setScreen(iscr);
          return;
        }
      } else {
        // is ScreenUnion
        return;
      }
    }
    // check all possible screens if no screen was given or the region is not on given screen
    // crop to the screen with the largest intersection
    screenRect = new Rectangle(0, 0, 0, 0);
    screenOn = null;

    if (getScreen() == null || !isOtherScreen()) {
      for (int i = 0; i < Screen.getNumberScreens(); i++) {
        screen = Screen.getScreen(i);
        rect = regionOnScreen(screen);
        if (rect != null) {
          if (rect.width * rect.height > screenRect.width * screenRect.height) {
            screenRect = rect;
            screenOn = screen;
          }
        }
      }
    } else {
      rect = regionOnScreen(getScreen());
      if (rect != null) {
        if (rect.width * rect.height > screenRect.width * screenRect.height) {
          screenRect = rect;
          screenOn = getScreen();
        }
      }
    }

    if (screenOn != null) {
      x = screenRect.x;
      y = screenRect.y;
      w = screenRect.width;
      h = screenRect.height;
      setScreen(screenOn);
    } else {
      // no screen found
      setScreen(null);
      Debug.error("Region(%d,%d,%d,%d) outside any screen - subsequent actions might not work as expected", x, y, w, h);
    }
  }

  /**
   * Checks if the Screen contains the Region.
   *
   * @param screen The Screen in which the Region might be
   * @return True, if the Region is on the Screen. False if the Region is not inside the Screen
   */
  protected Rectangle regionOnScreen(IScreen screen) {
    if (screen == null) {
      return null;
    }
    // get intersection of Region and Screen
    Rectangle rect = screen.getRect().intersection(getRect());
    // no Intersection, Region is not on the Screen
    if (rect.isEmpty()) {
      return null;
    }
    return rect;
  }

  /**
   * INTERNAL: USE - EXPERIMENTAL if true: this region is not bound to any screen
   *
   * @param rect rectangle
   * @return the current state
   */
  public static Region virtual(Rectangle rect) {
    Region reg = new Region();
    reg.x = rect.x;
    reg.y = rect.y;
    reg.w = rect.width;
    reg.h = rect.height;
    reg.setVirtual(true);
    reg.setScreen(Screen.getPrimaryScreen());
    return reg;
  }

  /**
   * INTERNAL: USE - EXPERIMENTAL if true: this region is not bound to any screen
   *
   * @return the current state
   */
  public boolean isVirtual() {
    return isVirtual;
  }

  /**
   * INTERNAL: USE - EXPERIMENTAL
   *
   * @param state if true: this region is not bound to any screen
   */
  public void setVirtual(boolean state) {
    isVirtual = state;
  }

  private boolean isVirtual = false;

  public IScreen getScreen() {
    if (this instanceof IScreen) {
      return (IScreen) this;
    }
    if (isOtherScreen()) {
      return getOtherScreen();
    }
    if (isOnScreen() && scr == null) {
      if (Screen.isHeadless())
        throw new SikuliXception("Element::getScreen: not possible - running headless");
      for (int i = 0; i < Screen.getNumberScreens(); i++) {
        if (Screen.getScreen(i).getBounds().contains(this.x, this.y)) {
          setScreen(Screen.getScreen(i));
          break;
        }
      }
      return Screen.getPrimaryScreen();
    }
    return scr;
  }

  public void setScreen(IScreen scr) {
    if (this instanceof IScreen) {
      return;
    }
    if (isOnScreen()) {
      this.scr = scr;
      isOnScreen();
    }
  }

  /**
   * Sets a new Screen for this region.
   *
   * @param id the containing screen object's id
   * @return the region itself
   */
  protected Element setScreen(int id) {
    setScreen(Screen.getScreen(id));
    return this;
  }

  /**
   * The Screen containing the Region
   */
  private IScreen scr;

  //TODO feature otherScreen has to be revised

  /**
   * INTERNAL USE
   * reveals wether the containing screen is a DeskTopScreen or not
   *
   * @return null if DeskTopScreen
   */
  public boolean isOtherScreen() {
    return (otherScreen != null);
  }

  /**
   * INTERNAL USE
   * identifies the point as being on a non-desktop-screen
   *
   * @param scr Screen
   * @return this
   */
  public Element setOtherScreen(IScreen scr) {
    otherScreen = scr;
    return this;
  }

  /**
   * INTERNAL USE
   * identifies the point as being on a non-desktop-screen
   * if this is true for the given Element
   *
   * @return this
   */
  protected Element setOtherScreenOf(Element element) {
    if (element.isOtherScreen()) {
      setOtherScreen(element.getOtherScreen());
    }
    return this;
  }

  /**
   * @return the non-desktop-screen
   */
  public IScreen getOtherScreen() {
    return otherScreen;
  }

  private IScreen otherScreen = null;
  //</editor-fold>

  //<editor-fold desc="016 handle FindFailed and ImageMissing">
  protected <PSI> Boolean handleFindFailed(PSI target, Image img) {
    log(logLevel, "handleFindFailed: %s", target);
    Boolean state = null;
    ObserveEvent evt = null;
    FindFailedResponse response = findFailedResponse;
    if (FindFailedResponse.HANDLE.equals(response)) {
      ObserveEvent.Type type = ObserveEvent.Type.FINDFAILED;
      if (findFailedHandler != null && ((ObserverCallBack) findFailedHandler).getType().equals(type)) {
        log(logLevel, "handleFindFailed: Response.HANDLE: calling handler");
        evt = new ObserveEvent("", type, target, img, this, 0);
        ((ObserverCallBack) findFailedHandler).findfailed(evt);
        response = evt.getResponse();
      }
    }
    if (FindFailedResponse.ABORT.equals(response)) {
      state = null;
    } else if (FindFailedResponse.SKIP.equals(response)) {
      state = false;
    } else if (FindFailedResponse.RETRY.equals(response)) {
      state = true;
    }
    if (FindFailedResponse.PROMPT.equals(response)) {
      response = handleFindFailedShowDialog(img, false);
    } else {
      return state;
    }
    if (FindFailedResponse.ABORT.equals(response)) {
      state = null;
    } else if (FindFailedResponse.SKIP.equals(response)) {
      // TODO HACK to allow recapture on FindFailed PROMPT
      if (img.backup()) {
        img.delete();
        state = handleImageMissing(img, true); //hack: FindFailed-ReCapture
        if (state == null || !state) {
          if (!img.restore()) {
            state = null;
          } else {
            img.getBufferedImage();
          }
        }
      }
    } else if (FindFailedResponse.RETRY.equals(response)) {
      state = true;
    }
    return state;
  }

  protected Boolean handleImageMissing(Image img, boolean recap) {
    log(logLevel, "handleImageMissing: %s", img.getName());
    ObserveEvent evt = null;
    FindFailedResponse response = findFailedResponse;
    if (!recap && imageMissingHandler != null) {
      log(logLevel, "handleImageMissing: calling handler");
      evt = new ObserveEvent("", ObserveEvent.Type.MISSING, null, img, this, 0);
      ((ObserverCallBack) imageMissingHandler).missing(evt);
      response = evt.getResponse();
    }
    if (recap || FindFailedResponse.PROMPT.equals(response)) {
      if (!recap) {
        log(logLevel, "handleImageMissing: Response.PROMPT");
      }
      response = handleFindFailedShowDialog(img, true);
    }
    if (FindFailedResponse.RETRY.equals(response)) {
      log(logLevel, "handleImageMissing: Response.RETRY: %s", (recap ? "recapture " : "capture missing "));
      getRobotForElement().delay(500);
      ScreenImage simg = getScreen().userCapture((recap ? "recapture " : "capture missing ") + img.getName());
      if (simg != null) {
        String path = ImagePath.getBundlePath();
        if (path == null) {
          log(-1, "handleImageMissing: no bundle path - aborting");
          return null;
        }
        simg.getFile(path, img.getName());
        img.reloadContent();
        if (img.isValid()) {
          log(logLevel, "handleImageMissing: %scaptured: %s", (recap ? "re" : ""), img);
          Image.setIDEshouldReload(img);
          return true;
        }
      }
      return null;
    } else if (findFailedResponse.ABORT.equals(response)) {
      log(-1, "handleImageMissing: Response.ABORT: aborting");
      log(-1, "Did you want to find text? If yes, use text methods (see docs).");
      return null;
    }
    log(logLevel, "handleImageMissing: skip requested on %s", (recap ? "recapture " : "capture missing "));
    return false;
  }

  protected FindFailedResponse handleFindFailedShowDialog(Image img, boolean shouldCapture) {
    log(logLevel, "handleFindFailedShowDialog: requested %s", (shouldCapture ? "(with capture)" : ""));
    FindFailedResponse response;
    FindFailedDialog fd = new FindFailedDialog(img, shouldCapture);
    fd.setVisible(true);
    response = fd.getResponse();
    fd.dispose();
    wait(0.5);
    log(logLevel, "handleFindFailedShowDialog: answer is %s", response);
    return response;
  }

  protected void setImageMissingHandler(Object handler) {
    imageMissingHandler = FindFailed.setHandler(handler, ObserveEvent.Type.MISSING);
  }

  protected Object imageMissingHandler = FindFailed.getImageMissingHandler();
  //</editor-fold>

  //<editor-fold desc="020 find image">

  /**
   * finds the given Pattern, String or Image in the Element and returns the best match.
   *
   * @param <PSI>  Pattern, String or Image
   * @param target what (PSI) to find in this Region
   * @return If found, the element. null otherwise
   * @throws FindFailed if the Find operation failed
   */
  public <PSI> Match find(PSI target) throws FindFailed {
    Match match = null;
    if (isValid()) {
      Image image = new Image(target);
      if (image.isValid()) {
        match = doFind(this, image);
      }
    }
    return match;
  }

  private Match doFind(Element element, Image image) {
    Mat where = element.getImage().getContent();
    Mat what = image.getContent();
    double wantedScore = image.similarity();
    Mat result = new Mat();
    Match match = null;
    Imgproc.matchTemplate(where, what, result, Imgproc.TM_CCOEFF_NORMED);
    Core.MinMaxLocResult minMax = Core.minMaxLoc(result);
    double maxVal = minMax.maxVal;
    if (maxVal > wantedScore) {
      match = new Match();
      match.setX(image.x + (int) minMax.maxLoc.x);
      match.setY(image.y + (int) minMax.maxLoc.y);
      match.setW(image.w);
      match.setH(image.h);
      match.score(maxVal);
      match.offset(image.offset());
      match.setImage(image);
      match.onScreen(isOnScreen());
    }
    return match;
  }

  /**
   * Waits for the Pattern, String or Image to appear until the AutoWaitTimeout value is exceeded.
   *
   * @param <PSI>  Pattern, String or Image
   * @param target The target to search for
   * @return The found Match
   * @throws FindFailed if the Find operation finally failed
   */
  public <PSI> Match wait(PSI target) throws FindFailed {
    if (target instanceof Float || target instanceof Double) {
      wait(0.0 + ((Double) target));
      return null;
    }
    return wait(target, autoWaitTimeout);
  }

  public <PSI> Match wait(PSI target, double timeout) throws FindFailed {
    if (!isOnScreen() || timeout < 0.01) {
      return find(target);
    }
    Match match = null;
    if (isValid()) {
      Image image = new Image(target);
      if (image.isValid()) {
        long waitUntil = new Date().getTime() + (int) (timeout * 1000);
        while (true) {
          long before = new Date().getTime();
          if (before > waitUntil) {
            break;
          }
          match = doFind(this, image);
          waitAfterScan(before);
        }
      }
    }
    return match;
  }

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="030 OCR - read text, line, word, char">

  /**
   * tries to read the text in this region/image<br>
   * might contain misread characters, NL characters and
   * other stuff, when interpreting contained grafics as text<br>
   * Best results: one or more lines of text with no contained grafics
   *
   * @return the text read (utf8 encoded)
   */
  public String text() {
    return OCR.readText(this);
  }

  /**
   * get text from this region/image
   * supposing it is one line of text
   *
   * @return the text or empty string
   */
  public String textLine() {
    return OCR.readLine(this);
  }

  /**
   * get text from this region/image
   * supposing it is one word
   *
   * @return the text or empty string
   */
  public String textWord() {
    return OCR.readWord(this);
  }

  /**
   * get text from this region/image
   * supposing it is one character
   *
   * @return the text or empty string
   */
  public String textChar() {
    return OCR.readChar(this);
  }

  /**
   * find text lines in this region/image
   *
   * @return list of strings each representing one line of text
   */
  public List<String> textLines() {
    List<String> lines = new ArrayList<>();
    List<Match> matches = findLines();
    for (Match match : matches) {
      lines.add(match.getText());
    }
    return lines;
  }

  /**
   * find the words as text in this region/image (top left to bottom right)<br>
   * a word is a sequence of detected utf8-characters surrounded by significant background space
   * might contain characters misinterpreted from contained grafics
   *
   * @return list of strings each representing one word
   */
  public List<String> textWords() {
    List<String> words = new ArrayList<>();
    List<Match> matches = findWords();
    for (Match match : matches) {
      words.add(match.getText());
    }
    return words;
  }

  /**
   * Find all lines as text (top left to bottom right) in this {@link Region} or {@link Image}
   *
   * @return a list of text {@link Match}es or empty list if not found
   */
  public List<Match> findLines() {
    return relocate(OCR.readLines(this));
  }

  /**
   * Find all words as text (top left to bottom right)
   *
   * @return a list of text matches
   */
  public List<Match> findWords() {
    return relocate(OCR.readWords(this));
  }
  //</editor-fold>

  //<editor-fold desc="032 find text as word or line">

  /**
   * Find the first word as text (top left to bottom right) containing the given text
   *
   * @param word to be searched
   * @return a text match or null if not found
   */
  public Match findWord(String word) {
    Match match = null;
    if (!word.isEmpty()) {
      Object result = doFindText(word, levelWord, false);
      if (result != null) {
        match = relocate((Match) result);
      }
    }
    return match;
  }

  /**
   * Find all words as text (top left to bottom right) containing the given text
   *
   * @param word to be searched
   * @return a list of text matches
   */
  public List<Match> findWords(String word) {
    Finder finder = ((Finder) doFindText(word, levelWord, true));
    if (null != finder) {
      return finder.getListFor(this);
    }
    return new ArrayList<>();
  }

  /**
   * Find the first line as text (top left to bottom right) containing the given text
   *
   * @param text the line should contain
   * @return a text match or null if not found
   */
  public Match findLine(String text) {
    Match match = null;
    if (!text.isEmpty()) {
      Object result = doFindText(text, levelLine, false);
      if (result != null) {
        match = relocate((Match) result);
      }
    }
    return match;
  }

  /**
   * Find all lines as text (top left to bottom right) containing the given text
   *
   * @param text the lines should contain
   * @return a list of text matches or empty list if not found
   */
  public List<Match> findLines(String text) {
    Finder finder = (Finder) doFindText(text, levelLine, true);
    if (null != finder) {
      return finder.getListFor(this);
    }
    return new ArrayList<>();
  }

  private int levelWord = 3;
  private int levelLine = 2;

  private Object doFindText(String text, int level, boolean multi) {
    Object returnValue = null;
    Finder finder = new Finder(this);
    lastSearchTime = (new Date()).getTime();
    if (level == levelWord) {
      if (multi) {
        if (finder.findWords(text)) {
          returnValue = finder;
        }
      } else {
        if (finder.findWord(text)) {
          returnValue = finder.next();
        }
      }
    } else if (level == levelLine) {
      if (multi) {
        if (finder.findLines(text)) {
          returnValue = finder;
        }
      } else {
        if (finder.findLine(text)) {
          returnValue = finder.next();
        }
      }
    }
    return returnValue;
  }
  //</editor-fold>

  //<editor-fold desc="034 find text like find image">
  public Match findText(String text) throws FindFailed {
    //TODO implement findText
    throw new SikuliXception(String.format("Pixels: findText: not implemented for", this.getClass().getCanonicalName()));
    //return match;
  }

  public Match findT(String text) throws FindFailed {
    return findText(text);
  }

  public Match existsText(String text) {
    //TODO existsText: try: findText:true catch: false
    throw new SikuliXception(String.format("Pixels: existsText: not implemented for", this.getClass().getCanonicalName()));
    //return match;
  }

  public Match existsT(String text) {
    return existsText(text);
  }

  public boolean hasText(String text) {
    return null != existsText(text);
  }

  public boolean hasT(String text) {
    return hasText(text);
  }

  public List<Match> findAllText(String text) {
    List<Match> matches = new ArrayList<>();
    throw new SikuliXception(String.format("Pixels: findAllText: not implemented for", this.getClass().getCanonicalName()));
    //return matches;
  }

  public List<Match> findAllT(String text) {
    return findAllText(text);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="040 Mouse - low level">

  /**
   * press and hold the specified buttons - use + to combine Button.LEFT left mouse button Button.MIDDLE middle mouse
   * button Button.RIGHT right mouse button
   *
   * @param buttons spec
   */
  public void mouseDown(int buttons) {
    Mouse.down(buttons, this);
  }

  /**
   * release all currently held buttons
   */
  public void mouseUp() {
    Mouse.up(0, this);
  }

  /**
   * release the specified mouse buttons (see mouseDown) if buttons==0, all currently held buttons are released
   *
   * @param buttons spec
   */
  public void mouseUp(int buttons) {
    Mouse.up(buttons, this);
  }

  /**
   * move the mouse pointer to the region's last successful match.
   * <br>same as hover
   * <br>
   *
   * @return 1 if possible, 0 otherwise
   */
  public int mouseMove() {
    try { // needed to cut throw chain for FindFailed
      return mouseMove(match());
    } catch (FindFailed ex) {
    }
    return 0;
  }

  /**
   * move the mouse pointer to the given target location
   * <br> same as hover
   * <br> Pattern or Filename - do a find before and use the match
   * <br> Region - position at center
   * <br> Match - position at match's targetOffset
   * <br> Location - position at that point
   * <br>
   *
   * @param <PFRML> Pattern, Filename, Text, Region, Match or Location
   * @param target  Pattern, Filename, Text, Region, Match or Location
   * @return 1 if possible, 0 otherwise
   * @throws FindFailed for Pattern or Filename
   */
  public <PFRML> int mouseMove(PFRML target) throws FindFailed {
    int ret = 0;
    Location loc = getLocationFromTarget(target);
    if (null != loc) {
      ret = Mouse.move(loc, this);
    }
    return ret;
  }

  /**
   * move the mouse from the current position to the offset position given by the parameters
   *
   * @param xoff horizontal offset (&lt; 0 left, &gt; 0 right)
   * @param yoff vertical offset (&lt; 0 up, &gt; 0 down)
   * @return 1 if possible, 0 otherwise
   */
  public int mouseMove(int xoff, int yoff) {
    try {
      return mouseMove(Mouse.at().offset(xoff, yoff));
    } catch (Exception ex) {
      return 0;
    }
  }
  //</editor-fold>

  //<editor-fold desc="042 Mouse - click">

  /**
   * time in milliseconds to delay between button down/up at next click only (max 1000)
   *
   * @param millisecs value
   */
  public void delayClick(int millisecs) {
    Settings.ClickDelay = millisecs;
  }

  /**
   * move the mouse pointer to region's last successful match.
   * <br>use center if no lastMatch
   * <br>if region is a match: move to targetOffset
   * <br>same as mouseMove
   *
   * @return 1 if possible, 0 otherwise
   */
  public int hover() {
    return mouseMove();
  }

  /**
   * move the mouse pointer to the given target location.
   * <br>same as mouseMove
   * <br> Pattern or Filename - do a find before and use the match
   * <br> Region - position at center
   * <br> Match - position at match's targetOffset
   * <br> Location - position at that point
   * <br>
   *
   * @param <PFRML> to search: Pattern, Filename, Text, Region, Match or Location
   * @param target  Pattern, Filename, Text, Region, Match or Location
   * @return 1 if possible, 0 otherwise
   * @throws FindFailed for Pattern or Filename
   */
  public <PFRML> int hover(PFRML target) throws FindFailed {
    log(logLevel, "hover: " + target);
    return mouseMove(target);
  }

  /**
   * left click at the region's last successful match.
   * <br>use center if no lastMatch
   * <br>if region is a match: click targetOffset
   *
   * @return 1 if possible, 0 otherwise
   */
  public int click() {
    try { // needed to cut throw chain for FindFailed
      return click(match(), 0);
    } catch (FindFailed ex) {
      return 0;
    }
  }

  /**
   * left click at the given target location.
   * <br> Pattern or Filename - do a find before and use the match
   * <br> Region - position at center
   * <br> Match - position at match's targetOffset
   * <br> Location - position at that point
   * <br>
   *
   * @param <PFRML> to search: Pattern, Filename, Text, Region, Match or Location
   * @param target  Pattern, Filename, Text, Region, Match or Location
   * @return 1 if possible, 0 otherwise
   * @throws FindFailed for Pattern or Filename
   */
  public <PFRML> int click(PFRML target) throws FindFailed {
    return click(target, 0);
  }

  /**
   * left click at the given target location.
   * <br> holding down the given modifier keys
   * <br> Pattern or Filename - do a find before and use the match
   * <br> Region - position at center
   * <br> Match - position at match's targetOffset
   * <br> Location - position at that point
   * <br>
   *
   * @param <PFRML>   to search: Pattern, Filename, Text, Region, Match or Location
   * @param target    Pattern, Filename, Text, Region, Match or Location
   * @param modifiers the value of the resulting bitmask (see KeyModifier)
   * @return 1 if possible, 0 otherwise
   * @throws FindFailed for Pattern or Filename
   */
  public <PFRML> int click(PFRML target, Integer modifiers) throws FindFailed {
    int ret = 0;
    Location loc = getLocationFromTarget(target);
    if (null != loc) {
      ret = Mouse.click(loc, InputEvent.BUTTON1_DOWN_MASK, modifiers, false, this);
    }
    return ret;
  }
  //</editor-fold>

  //<editor-fold desc="090 helper private">
  protected List<Match> relocate(List<Match> matches) {
    return matches;
  }

  protected Match relocate(Match match) {
    return match;
  }
  //</editor-fold>

  //<editor-fold desc="99 deprecated features">

  /**
   * @return a list of matches
   * @see #findLines()
   * @deprecated use findLines() instead
   */
  public List<Match> collectLines() {
    return findLines();
  }

  /**
   * @return a list of lines as strings
   * @see #textLines()
   * @deprecated use textLines() instead
   */
  @Deprecated
  public List<String> collectLinesText() {
    return textLines();
  }

  /**
   * @return a list of matches
   * @see #findWords()
   * @deprecated use findWords() instead
   */
  @Deprecated
  public List<Match> collectWords() {
    return findWords();
  }

  /**
   * @return a list of words sa strings
   * @see #textWords()
   * @deprecated use textWords() instead
   */
  @Deprecated
  public List<String> collectWordsText() {
    return textWords();
  }
  //</editor-fold>
}

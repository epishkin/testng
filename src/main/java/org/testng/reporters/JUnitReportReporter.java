package org.testng.reporters;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.collections.Lists;
import org.testng.collections.Maps;
import org.testng.internal.Utils;
import org.testng.internal.annotations.Sets;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class JUnitReportReporter implements IReporter {

  @Override
  public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites,
      String defaultOutputDirectory) {

    String outputDirectory = defaultOutputDirectory + File.separator + "junitreports";
    Map<Class<?>, Set<ITestResult>> results = Maps.newHashMap();
    for (ISuite suite : suites) {
      Map<String, ISuiteResult> suiteResults = suite.getResults();
      for (ISuiteResult sr : suiteResults.values()) {
        ITestContext tc = sr.getTestContext();
        addResults(tc.getPassedTests().getAllResults(), results);
        addResults(tc.getFailedTests().getAllResults(), results);
        addResults(tc.getSkippedTests().getAllResults(), results);
      }
    }

    for (Map.Entry<Class<?>, Set<ITestResult>> entry : results.entrySet()) {
      Class<?> cls = entry.getKey();
      Properties p1 = new Properties();
      p1.setProperty("name", cls.getName());
      Date timeStamp = Calendar.getInstance().getTime();
      p1.setProperty(XMLConstants.ATTR_TIMESTAMP, timeStamp.toGMTString());

      List<TestTag> testCases = Lists.newArrayList();
      int failures = 0;
      int errors = 0;
      int testCount = 0;
      int totalTime = 0;

      for (ITestResult tr: entry.getValue()) {
        TestTag testTag = new TestTag();

        if (tr.getStatus() != ITestResult.SUCCESS) failures++;
        Properties p2 = new Properties();
        p2.setProperty("classname", tr.getMethod().getMethod().getDeclaringClass().getName());
        p2.setProperty("name", tr.getMethod().getMethodName());
        long time = tr.getEndMillis() - tr.getStartMillis();
        p2.setProperty("time", "" + time);
        Throwable t = tr.getThrowable();
        if (t != null) {
          t.fillInStackTrace();
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          t.printStackTrace(pw);
          testTag.message = t.getMessage();
          testTag.type = t.getClass().getName();
          testTag.stackTrace = sw.toString();
          errors++;
        }
        totalTime += time;
        testCount++;
        testTag.properties = p2;
        testCases.add(testTag);
      }

      p1.setProperty("failures", "" + failures);
      p1.setProperty("errors", "" + errors);
      p1.setProperty("name", cls.getName());
      p1.setProperty("tests", "" + testCount);
      p1.setProperty("time", "" + totalTime);
      try {
        p1.setProperty(XMLConstants.ATTR_HOSTNAME, InetAddress.getLocalHost().getHostName());
      } catch (UnknownHostException e) {
        // ignore
      }

      //
      // Now that we have all the information we need, generate the file
      //
      XMLStringBuffer xsb = new XMLStringBuffer("");
      xsb.setXmlDetails("1.0", "UTF-8");
      xsb.addComment("Generated by " + getClass().getName());

      xsb.push("testsuite", p1);
      for (TestTag testTag : testCases) {
        if (testTag.stackTrace == null) xsb.addEmptyElement("testcase", testTag.properties);
        else {
          xsb.push("testcase", testTag.properties);

          Properties p = new Properties();
          p.setProperty("message", testTag.message);
          p.setProperty("type", testTag.type);
          xsb.push("error", p);
          xsb.addCDATA(testTag.stackTrace);
          xsb.pop("error");

          xsb.pop("testcase");
        }
      }
      xsb.pop("testsuite");

      String fileName = "TEST-" + cls.getName() + ".xml";
      Utils.writeFile(outputDirectory, fileName, xsb.toXML());
    }

//    System.out.println(xsb.toXML());
//    System.out.println("");

  }

  class TestTag {
    public Properties properties;
    public String message;
    public String type;
    public String stackTrace;
  }

  private void addResults(Set<ITestResult> allResults, Map<Class<?>, Set<ITestResult>> out) {
    for (ITestResult tr : allResults) {
      Class<?> cls = tr.getMethod().getTestClass().getRealClass();
      Set<ITestResult> l = out.get(cls);
      if (l == null) {
        l = Sets.newHashSet();
        out.put(cls, l);
      }
      l.add(tr);
    }
  }

}

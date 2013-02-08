package com.twitter;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author mmcbride
 */
public class FinagleThriftRunner {
  private String thriftPlatform;
  private String thriftName;
  private String thriftBin;
  private boolean fixHash = false;

  public FinagleThriftRunner() {
    this("thrift", false);
  }

  public FinagleThriftRunner(String thriftName) {
    this(thriftName, false);
  }

  public FinagleThriftRunner(boolean fixHashCode) {
    this("thrift", fixHashCode);
  }

  public FinagleThriftRunner(String thriftName, boolean fixHashCode) {
    this.thriftName = thriftName;
    String osName = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    thriftPlatform = platformFromOs(osName, arch);
    thriftBin = thriftName + "." + thriftPlatform;
    fixHash = fixHashCode;
  }

  public File getBinary(Log log) throws IOException {
    File thrift = File.createTempFile(thriftName, "");
    String thriftResource = String.format("/thrift/%s", thriftBin);
    log.info("copying " + thriftResource + " to " + thrift);
    InputStream binStream = this.getClass().getResourceAsStream(thriftResource);
    FileUtils.copyStreamToFile(new RawInputStreamFacade(binStream), thrift);
    thrift.setExecutable(true, true);
    return thrift;
  }

  public void compile(Log log, File output, Set<File> thriftFiles, Set<File> thriftIncludes, String generator)
  throws MojoFailureException {
    try {
      for (File thriftFile: thriftFiles) {
        Commandline cl = new Commandline();
        List<String> command = new ArrayList<String>();
        cl.setExecutable(getBinary(log).getCanonicalPath());
        for (File inc : thriftIncludes) {
          command.add("-I");
          command.add(inc.getCanonicalPath());
        }
        command.add("-o");
        command.add(output.getCanonicalPath());
        command.add("--gen");
        if(fixHash && generator.equals("java")){
          command.add("java:hashcode");
        } else {
          command.add(generator);
        }
        command.add(thriftFile.getCanonicalPath());
        cl.addArguments(command.toArray(new String[0]));
        CommandLineUtils.StringStreamConsumer clOutput = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer clError = new CommandLineUtils.StringStreamConsumer();
        log.info("running " + command);
        int responseCode = CommandLineUtils.executeCommandLine(cl, null, clOutput, clError);
        if (responseCode != 0) {
          log.error("thrift failed output: " + clOutput.getOutput());
          log.error("thrift failed error: " + clError.getOutput());
          throw new MojoFailureException(
                  "thrift did not exit cleanly. Review output for more information.");
        }
      }
    } catch(IOException ioe) {
      throw new MojoFailureException(
        "IOException running thrift: " + ioe.getMessage());
    } catch(CommandLineException cle) {
      throw new MojoFailureException(
        "CommandLine exception running thrift: " + cle.getMessage());
    }
  }

  public String platformFromOs(String osName, String arch) {
    if ("Mac OS X".equals(osName)) {
      return "osx10.6";
    } else if ("Linux".equals(osName)) {
      if ("i386".equals(arch)) return "linux32";
      if ("amd64".equals(arch)) return "linux64";
    }
    return null;
  }
}

package org.ippul;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.core.cli.Main;

public class CucumberRunner {

  private static final Logger logger = LoggerFactory.getLogger(CucumberRunner.class);

  private static final String RUNTIME_DATA_PREFIX = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd/HH:mm:ss"));
  
  private static final String RUN_ID = System.getenv("RUN_ID");
  
  private static final String BASE_DATA_FOLDER = "/data";
  
  private static final String FEATURE_DATA_FOLDER = BASE_DATA_FOLDER + "/features/";
  
  private static final String OUTPUT_DATA_FOLDER = BASE_DATA_FOLDER + "/output/";
  
  private static final String STEPS_DATA_FOLDER = BASE_DATA_FOLDER + "/steps/" + RUN_ID + "/";
  
  private static final String CONFIGURATION_FOLDER =  BASE_DATA_FOLDER + "/conf" ;
  
  public static void main(String[] args) throws Throwable {
    Properties prop = new Properties();
    try (InputStream input = new FileInputStream(CONFIGURATION_FOLDER + "/" + RUN_ID + ".properties")) {
      prop.load(input);
    } catch (IOException ex) {
        ex.printStackTrace();
    }
    //
    String[] cucumberRunParameters = prepareRunParameter(prop);
    for(String argument : cucumberRunParameters){
      logger.info("Running parameters: {}", argument);
    }
    downloadMavenArtifact(prop);
    URLClassLoader child = loadSteps();
    byte exitStatus = Main.run(cucumberRunParameters, child);
    cleanStepFolder();
    System.exit(exitStatus);
  }

  private static void cleanStepFolder() throws IOException {
    FileUtils.deleteDirectory(Paths.get(STEPS_DATA_FOLDER).toFile());
  }

  public static String[] prepareRunParameter(Properties prop) {
    StringBuilder glue = new StringBuilder();
    StringBuilder filterTag = new StringBuilder();
    for(Object key: prop.keySet()){
      if(key.toString().startsWith("cucumber.glue")) {
        glue.append(prop.getProperty(key.toString())).append(",");
      } else if(key.toString().startsWith("cucumber.filter.tag")) {
        filterTag.append(prop.getProperty(key.toString())).append(",");
      }
    }
    if(glue.length() > 0 && filterTag.length() > 0) {
      glue.setLength(glue.length() - 1);
      filterTag.setLength(filterTag.length() - 1);
      return new String[]{"--glue", glue.toString(), "--filter.tags", filterTag.toString(), "--plugin", "html:" + OUTPUT_DATA_FOLDER + RUNTIME_DATA_PREFIX + "-" + RUN_ID +".html", FEATURE_DATA_FOLDER};
    } else if(glue.length() > 0) {
      glue.setLength(glue.length() - 1);
      return new String[]{"--glue", glue.toString(), "--plugin", "html:" + OUTPUT_DATA_FOLDER + RUNTIME_DATA_PREFIX + "-" + RUN_ID +".html", FEATURE_DATA_FOLDER};
    } else if(filterTag.length() > 0) {
      filterTag.setLength(filterTag.length() - 1);
      return new String[]{"--filter.tags", filterTag.toString(), "--plugin", "html:" + OUTPUT_DATA_FOLDER + RUNTIME_DATA_PREFIX + "-" + RUN_ID +".html", FEATURE_DATA_FOLDER};
    }    
    return null;
  }

  public static URLClassLoader loadSteps() throws ClassNotFoundException, IOException {
		try (Stream<Path> stream = Files.walk(Paths.get(STEPS_DATA_FOLDER), Integer.MAX_VALUE)) {
      URL[] jars = stream
          .filter(Files::isRegularFile)
          .filter(file -> file.toFile().getName().endsWith("jar"))
          .map(file -> {
            try {
              logger.debug("file {} added to the classpath", file.toFile().getName());
              return file.toUri().toURL();
            } catch (MalformedURLException e) {
              e.printStackTrace();
            }
            return null;
          })
          .toArray(URL[]::new);
      URLClassLoader child = new URLClassLoader(jars, Thread.currentThread().getContextClassLoader());
      return child;
		}
  }

  public static void downloadMavenArtifact(Properties prop) throws IOException{
    Files.createDirectories(Paths.get(STEPS_DATA_FOLDER));
    System.setProperty("maven.repo.local", STEPS_DATA_FOLDER);
    for(Object key: prop.keySet()){
      if(key.toString().startsWith("cucumber.feature.steps.definition")) {
        logger.info("retrieving gav: {}", prop.getProperty(key.toString()));
        Maven.configureResolver().fromFile(CONFIGURATION_FOLDER + "/settings.xml").resolve(prop.getProperty(key.toString())).withTransitivity().asResolvedArtifact();
      }
    }
  }
}



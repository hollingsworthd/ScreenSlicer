/* 
 * ScreenSlicer (TM)
 * Copyright (C) 2013-2015 ScreenSlicer committers
 * https://github.com/MachinePublishers/ScreenSlicer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.screenslicer.webapp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

public class WebAppConfig extends ResourceConfig {
  private final String[] mimeTypes;

  public WebAppConfig() throws IOException {
    Collection<String> mimeTypeList = new HashSet<String>();
    mimeTypeList.add(MediaType.APPLICATION_FORM_URLENCODED);
    mimeTypeList.add(MediaType.APPLICATION_JSON);
    mimeTypeList.add(MediaType.APPLICATION_OCTET_STREAM);
    mimeTypeList.add(MediaType.APPLICATION_SVG_XML);
    mimeTypeList.add(MediaType.APPLICATION_XHTML_XML);
    mimeTypeList.add(MediaType.APPLICATION_XML);
    mimeTypeList.add(MediaType.MULTIPART_FORM_DATA);
    mimeTypeList.add(MediaType.TEXT_HTML);
    mimeTypeList.add(MediaType.TEXT_PLAIN);
    mimeTypeList.add(MediaType.TEXT_XML);
    if (new File("./htdocs").exists()) {
      Collection<File> files = FileUtils.listFiles(new File("./htdocs"), null, true);
      for (File file : files) {
        final byte[] contents = FileUtils.readFileToByteArray(file);
        Resource.Builder resourceBuilder = Resource.builder();
        resourceBuilder.path(file.getAbsolutePath().split("/htdocs/")[1]);
        final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
        String mimeType = MimeTypeFinder.probeContentType(Paths.get(file.toURI()));
        if (!mimeTypeList.contains(mimeType)
            && !file.getName().toLowerCase().endsWith(".jpg")
            && !file.getName().toLowerCase().endsWith(".jpeg")
            && !file.getName().toLowerCase().endsWith(".png")
            && !file.getName().toLowerCase().endsWith(".gif")
            && !file.getName().toLowerCase().endsWith(".ico")) {
          mimeTypeList.add(mimeType);
        }
        final File myFile = file;
        methodBuilder.produces(mimeType)
            .handledBy(new Inflector<ContainerRequestContext, byte[]>() {
              @Override
              public byte[] apply(ContainerRequestContext req) {
                if (!WebApp.DEV) {
                  return contents;
                }
                try {
                  return FileUtils.readFileToByteArray(myFile);
                } catch (IOException e) {
                  return contents;
                }
              }
            });
        registerResources(resourceBuilder.build());
      }
    }
    register(MultiPartFeature.class);
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setUrls(ClasspathHelper.forJavaClassPath())
        .filterInputsBy(new FilterBuilder().include(".*")));
    Set<Class<? extends WebResource>> webResourceClasses = reflections.getSubTypesOf(WebResource.class);
    for (Class<? extends WebResource> webpageClass : webResourceClasses) {
      registerResources(Resource.builder(webpageClass).build());
    }
    register(ExceptionHandler.class);
    mimeTypes = mimeTypeList.toArray(new String[0]);
  }

  public String[] mimeTypes() {
    return mimeTypes;
  }
}
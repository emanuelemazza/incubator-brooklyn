/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.cli.lister;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.basic.BrooklynObject;
import brooklyn.enricher.basic.AbstractEnricher;
import brooklyn.entity.Application;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.policy.Enricher;
import brooklyn.policy.Policy;
import brooklyn.policy.basic.AbstractPolicy;
import brooklyn.util.ResourceUtils;
import brooklyn.util.javalang.UrlClassLoader;
import brooklyn.util.net.Urls;
import brooklyn.util.os.Os;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ClassFinder {

    private static final Logger log = LoggerFactory.getLogger(ClassFinder.class);

    private static final Collection<Class<?>> BORING = ImmutableList.<Class<?>>of(
            Entity.class,
            AbstractEntity.class,
            SoftwareProcessImpl.class,
            Application.class,
            AbstractApplication.class,
            Policy.class,
            Enricher.class,
            AbstractPolicy.class,
            AbstractEnricher.class);

    public static Predicate<Class<?>> notBoring() {
        return new Predicate<Class<?>>() {
            public boolean apply(Class<?> input) {
                return (input != null && !BORING.contains(input));
            }
        };
    }
    
    public static Predicate<Class<?>> withAnnotation(final Class<? extends Annotation> annotation) {
        return new Predicate<Class<?>>() {
            public boolean apply(Class<?> input) {
                return (input != null && input.getAnnotation(annotation) != null);
            }
        };
    }
    
    public static Predicate<? super Class<? extends BrooklynObject>> withClassNameMatching(final String typeRegex) {
        return new Predicate<Class<?>>() {
            public boolean apply(Class<?> input) {
                return (input != null && input.getName() != null && input.getName().matches(typeRegex));
            }
        };
    }
    
    public static List<URL> toJarUrls(String url) throws MalformedURLException {
        if (url==null) throw new NullPointerException("Cannot read from null");
        if (url=="") throw new NullPointerException("Cannot read from empty string");
        
        List<URL> result = Lists.newArrayList();
        
        String protocol = Urls.getProtocol(url);
        if (protocol!=null) {
            // it's a URL - easy
            if ("file".equals(protocol)) {
                url = ResourceUtils.tidyFileUrl(url);
            }
            result.add(new URL(url));
        } else {
            // treat as file
            String tidiedPath = Os.tidyPath(url);
            File tidiedFile = new File(tidiedPath);
            if (tidiedFile.isDirectory()) {
                List<File> toscan = Lists.newLinkedList();
                toscan.add(tidiedFile);
                while (toscan.size() > 0) {
                    File file = toscan.remove(0);
                    if (file.isFile()) {
                        if (file.getName().toLowerCase().endsWith(".jar")) {
                            result.add(new URL("file://"+file.getAbsolutePath()));
                        }
                    } else if (file.isDirectory()) {
                        for (File subfile : file.listFiles()) {
                            toscan.add(subfile);
                        }
                    } else {
                        log.info("Cannot read "+file+"; not a file or directory");
                    }
                }
            }
        }
        
        return result;
    }

    public static <T extends BrooklynObject> Set<Class<? extends T>> findClasses(Collection<URL> urls, Class<T> clazz) {
        ClassLoader classLoader = new UrlClassLoader(urls.toArray(new URL[urls.size()]));
        
        Reflections reflections = new ConfigurationBuilder()
                .addClassLoader(classLoader)
                .addScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new FieldAnnotationsScanner())
                .addUrls(urls)
                .build();
        
        Set<Class<? extends T>> types = reflections.getSubTypesOf(clazz);
        
        return types;
    }
}

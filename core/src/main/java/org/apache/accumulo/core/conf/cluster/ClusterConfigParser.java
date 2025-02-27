/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.accumulo.core.conf.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class ClusterConfigParser {

  private static final String PROPERTY_FORMAT = "%s=\"%s\"%n";
  private static final String[] SECTIONS = new String[] {"manager", "monitor", "gc", "tserver"};

  @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "paths not set by user input")
  public static Map<String,String> parseConfiguration(String configFile) throws IOException {
    Map<String,String> results = new HashMap<>();
    try (InputStream fis = Files.newInputStream(Paths.get(configFile), StandardOpenOption.READ)) {
      Yaml y = new Yaml();
      Map<String,Object> config = y.load(fis);
      config.forEach((k, v) -> flatten("", k, v, results));
    }
    return results;
  }

  private static String addTheDot(String key) {
    return (key.endsWith(".")) ? "" : ".";
  }

  private static void flatten(String parentKey, String key, Object value,
      Map<String,String> results) {
    String parent =
        (parentKey == null || parentKey.equals("")) ? "" : parentKey + addTheDot(parentKey);
    if (value instanceof String) {
      results.put(parent + key, (String) value);
      return;
    } else if (value instanceof List) {
      ((List<?>) value).forEach(l -> {
        if (l instanceof String) {
          // remove the [] at the ends of toString()
          String val = value.toString();
          results.put(parent + key, val.substring(1, val.length() - 1).replace(", ", " "));
          return;
        } else {
          flatten(parent, key, l, results);
        }
      });
    } else if (value instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String,Object> map = (Map<String,Object>) value;
      map.forEach((k, v) -> flatten(parent + key, k, v, results));
    } else {
      throw new RuntimeException("Unhandled object type: " + value.getClass());
    }
  }

  public static void outputShellVariables(Map<String,String> config, PrintStream out) {
    for (String section : SECTIONS) {
      if (config.containsKey(section)) {
        out.printf(PROPERTY_FORMAT, section.toUpperCase() + "_HOSTS", config.get(section));
      } else {
        if (section.equals("manager") || section.equals("tserver")) {
          throw new RuntimeException("Required configuration section is missing: " + section);
        }
        System.err.println("WARN: " + section + " is missing");
      }
    }

    if (config.containsKey("compaction.coordinator")) {
      out.printf(PROPERTY_FORMAT, "COORDINATOR_HOSTS", config.get("compaction.coordinator"));
    }
    if (config.containsKey("compaction.compactor.queue")) {
      out.printf(PROPERTY_FORMAT, "COMPACTION_QUEUES", config.get("compaction.compactor.queue"));
    }
    String queues = config.get("compaction.compactor.queue");
    if (StringUtils.isNotEmpty(queues)) {
      String[] q = queues.split(" ");
      for (int i = 0; i < q.length; i++) {
        if (config.containsKey("compaction.compactor." + q[i])) {
          out.printf(PROPERTY_FORMAT, "COMPACTOR_HOSTS_" + q[i],
              config.get("compaction.compactor." + q[i]));
        }
      }
    }
    out.flush();
  }

  public static void main(String[] args) throws IOException {
    if (args == null || args.length != 1) {
      System.err.println("Usage: ClusterConfigParser <configFile>");
      System.exit(1);
    }
    outputShellVariables(parseConfiguration(args[0]), System.out);
  }

}

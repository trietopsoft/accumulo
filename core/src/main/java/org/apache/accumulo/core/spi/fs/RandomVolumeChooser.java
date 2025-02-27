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
package org.apache.accumulo.core.spi.fs;

import java.security.SecureRandom;
import java.util.Set;

/**
 * @since 2.1.0
 */
public class RandomVolumeChooser implements VolumeChooser {
  private static final SecureRandom random = new SecureRandom();

  @Override
  public String choose(VolumeChooserEnvironment env, Set<String> options) {
    String[] optionsArray = options.toArray(new String[0]);
    return optionsArray[random.nextInt(optionsArray.length)];
  }

  @Override
  public Set<String> choosable(VolumeChooserEnvironment env, Set<String> options) {
    return options;
  }
}

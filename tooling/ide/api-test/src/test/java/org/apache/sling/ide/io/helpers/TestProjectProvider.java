/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.io.helpers;

import org.junit.rules.ExternalResource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * This class will create a Test Project
 * so that it can be used to jUnit Tests.
 *
 * Created by Andreas Schaefer (Headwire.com) on 1/29/16.
 */
public class TestProjectProvider
    extends ExternalResource
{
    private Logger logger = Logger.getLogger(getClass().getName());

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private TestProject project;

    @Override
    protected void before() throws Throwable {
        logger.info("Before creating Test Project");
        project = new TestProject("test-project-" + COUNTER.incrementAndGet());
        logger.info("Created Test Project: " + project);
    }

    @Override
    protected void after() {
        if (project != null) {
            project.clear();
            project = null;
        }
    }

    public TestProject getProject() {
        return project;
    }

}

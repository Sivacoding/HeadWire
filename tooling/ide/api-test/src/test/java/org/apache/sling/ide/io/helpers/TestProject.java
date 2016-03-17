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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/29/16.
 */
public class TestProject {

    private Logger logger = Logger.getLogger(getClass().getName());

    public static final String ROOT_FOLDER = "target/test-projects/";
    public static final String CONTENT_FOLDER = "/jcr_root";

    private String projectName;
    private String folderName;
    private File projectFolder;

    public TestProject(String projectName) {
        this.projectName = projectName;
        // Create Test Folder
        folderName = ROOT_FOLDER + projectName + CONTENT_FOLDER;
        projectFolder = new File(folderName);
        if(projectFolder.exists()) {
            throw new IllegalArgumentException("Test Project Folder: '" + folderName + "' already exists");
        }
        if(!projectFolder.mkdirs()) {
            throw new IllegalArgumentException("Could not create Project Folder: '" + folderName + "'");
        }
    }

    public void clear() {
        if(projectFolder.exists()) {
            projectFolder.deleteOnExit();
        }
    }

    public void createFolder(String path) {
        new File(projectFolder, path).mkdirs();
    }

    public void createOrUpdateFile(String filePath, InputStream content) throws IOException {
        Utils.createFileAndFolders(filePath, projectFolder, content);
    }

    public TestSlingResource getResource(String path) {
        File resource = Utils.findFileByPath(path, projectFolder);
        if(resource == null) {
            throw new IllegalArgumentException("Given path: '" + path + "' does not point to a file");
        }
        logger.info("Create new Test Resource, Project Folder: '" + folderName + "', path: " + path);
        return new TestSlingResource(
            new TestSlingProject(folderName),
            path
        );
    }
}

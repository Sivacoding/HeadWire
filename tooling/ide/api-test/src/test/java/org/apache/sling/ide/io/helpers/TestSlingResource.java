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

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.io.ConnectorException;
import org.apache.sling.ide.io.SlingProject;
import org.apache.sling.ide.io.SlingResource;
import org.apache.sling.ide.io.SlingResourceVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/25/16.
 */
public class TestSlingResource
    implements SlingResource
{

    private Logger logger = Logger.getLogger(getClass().getName());

    public static final String JCR_ROOT_FOLDER_NAME = "jcr_root";

    private SlingProject project;
    private File file;
    // If there is no local file available the resource still could exist on the Sling Server
    private String resourcePath;

    public TestSlingResource(SlingProject project, String filePath) {
        this.project = project;
        logger.info("Sync Directory: " + project.getSyncDirectory());
        logger.info("File Path: " + filePath);
//        logger.log(Level.INFO, "Where we are", new Exception());
        logger.log(INFO, "Project, ''{0}'', Sync Dir: ''{1}''", new Object[]{project, project.getSyncDirectory()});
        this.file = getFileByPath(((TestSlingResource) project.getSyncDirectory()).file, filePath);
    }

    public TestSlingResource(SlingProject project, File file) {
        this.project = project;
        this.file = file;
        logger.log(INFO, "File (2): ''{0}''", this.file);
//        logger.log(Level.INFO, "Where we are (2)", new Exception());
    }

//    public SlingResource4IntelliJ(SlingProject project, String resourcePath) {
//        this.project = project;
//        this.resourcePath = resourcePath;
//        // Try to find the local file
//        SlingResource4IntelliJ syncDirectory = (SlingResource4IntelliJ) project.getSyncDirectory();
//        this.file = syncDirectory.file.findFileByRelativePath(resourcePath);
//    }

    public String toString() {
        return "Spy Sling Resource, file: '" + ( file == null ? "null" : file.getAbsolutePath() + "'");
    }
    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public SlingProject getProject() {
        return project;
    }

    @Override
    public boolean isModified() {
        boolean ret = true;
//        if(file != null) {
//            Long modificationTimestamp = Util.getModificationStamp(file);
//            Long fileModificationTimestamp = file.getModificationStamp();
//            ret = modificationTimestamp < fileModificationTimestamp;
//        }
        return ret;
    }

    @Override
    public boolean isLocalOnly() {
        return false;
    }

    @Override
    public boolean isFolder() {
        //AS TODO: Handle the scenario when it is a Sling Server Resource only
        return file == null ? false : file.isDirectory();
    }

    @Override
    public boolean isFile() {
        //AS TODO: Handle the scenario when it is a Sling Server Resource only
        return file == null ? true : !file.isDirectory();
    }

    @Override
    public String getLocalPath() {
        return getLocalPath(true);
    }

    private String getLocalPath(boolean raw) {
        String ret = file == null ? null : file.getAbsolutePath();
        if(!raw) {
            //AS TODO: Adjust this for Windows
        }
        logger.info("Local Path: " + ret);
        return ret;
    }

    @Override
    public String getResourceLocalPath() {
        return getResourceLocalPath(true);
    }

    private String getResourceLocalPath(boolean raw) {
        String ret = getLocalPath(false);

        if(ret != null) {
            logger.info("Project: " + project);
            logger.info("Project Sync Directory: " + project.getSyncDirectory());
            String syncDirectoryPath = ((TestSlingResource) project.getSyncDirectory()).getLocalPath(false);
            if (ret.startsWith(syncDirectoryPath)) {
                ret = ret.substring(syncDirectoryPath.length());
                if (!ret.startsWith("/")) {
                    ret = "/" + ret;
                }
            } else {
                throw new IllegalArgumentException("Resource Path: '" + ret + "' doest not start with Sync Path: '" + syncDirectoryPath + "'");
            }
            if(!raw) {
                //AS TODO: Adjust this for Windows -> apply raw
            }
        }
        return ret;
    }

    @Override
    public String getResourcePath() {
        String ret = getResourceLocalPath(true);
        if(ret == null && resourcePath != null) {
            ret = resourcePath;
        }
        return ret;
    }

    @Override
    public InputStream getContentStream() throws IOException {
        FileInputStream fis = new FileInputStream(file);
        logger.info("Create File Input Stream for file: " + file);
        logger.info("Content: " + IOUtils.toString(fis));
        fis.close();
        return new FileInputStream(file);
    }

    @Override
    public SlingResource getParent() {
        SlingResource ret = null;
        logger.log(INFO, "getParent(), file: ''{0}'', resource path: ''{1}''", new Object[] {file, resourcePath});
        if(file != null) {
            File parent = file.getParentFile();
            if(!parent.getName().equals(JCR_ROOT_FOLDER_NAME)) {
                logger.log(INFO, "getParent(), us file");
                ret = new TestSlingResource(project, parent);
            }
        } else if(resourcePath != null) {
            String path = resourcePath;
            while(path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            if(path.length() > 0) {
                int index = path.lastIndexOf('/');
                if(index > 0) {
                    String parentResourcePath = path.substring(0, index);
                    ret = new TestSlingResource(project, parentResourcePath);
                }
            }
        }
        return ret;
    }

    @Override
    public SlingResource findInParentByName(String name) {
        SlingResource ret = null;
        SlingResource parent = this;
        if(parent.getName().equals(name)) {
            ret = parent;
        } else {
            while ((parent = parent.getParent()) != null) {
                if (parent.getName().equals(name)) {
                    ret = parent;
                    break;
                }
            }
        }
        return ret;
    }

    @Override
    public SlingResource findChild(String childFileName) {
        SlingResource ret = null;
        for(File child: file.listFiles()) {
            if(child.getName().equals(childFileName)) {
                ret = new TestSlingResource(project, child);
            }
        }
        if(ret == null) {
            ret = new TestSlingResource(project, getResourcePath() + "/" + childFileName);
        }
        return ret;
    }

    @Override
    public SlingResource findChildByPath(String childPath) {
        SlingResource ret = null;
        // Remove trailing slashes
        if(childPath.startsWith("/")) {
            childPath = childPath.substring(1);
        }
        String[] directories = childPath.split("/");
        File directoryFile = file;
        // Go through the child directory. If a final file is found the the directoryFile is set otherwise it is null
        for(final String directory: directories) {
            File[] children = directoryFile.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.equals(directory);
                    }
                }
            );
            if(children.length == 1) {
                directoryFile = children[0];
            } else {
                directoryFile = null;
                break;
            }
        }
        if(directoryFile != null) {
            ret = new TestSlingResource(project, directoryFile);
        } else {
            ret = new TestSlingResource(project, childPath);
        }
        return ret;
    }

    @Override
    public List<SlingResource> getLocalChildren() {
        List<SlingResource> ret = new ArrayList<SlingResource>();
        for(File child: file.listFiles()) {
            ret.add(new TestSlingResource(project, child));
        }
        return ret;
    }

    @Override
    public boolean existsLocally() {
        return file != null;
    }

    @Override
    public boolean existsRemotely() {
        //AS TODO: How to check that? Probably need to defer that until we tested it and then set it
        return false;
    }

    @Override
    public void accept(SlingResourceVisitor visitor) throws ConnectorException {
//        accept(visitor, );
    }

    @Override
    public void accept(SlingResourceVisitor visitor, int depth, int memberFlags) throws ConnectorException {

    }

    @Override
    public SlingResource getSyncDirectory() {
        return getProject().getSyncDirectory();
    }

    @Override
    public SlingResource getResourceFromPath(String resourcePath) {
        return new TestSlingResource(project, resourcePath);
    }

    @Override
    public Filter loadFilter() throws ConnectorException {
        return getProject().loadFilter();
    }

    @Override
    /**
     * Check if the two resource are equal by checking if both have the same file (if set) or the same resource path
     */
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        TestSlingResource that = (TestSlingResource) o;

        if(file != null) {
            return file.equals(that.file);
        } else {
            return
                resourcePath != null ?
                    resourcePath.equals(that.resourcePath) :
                    that.resourcePath == null;
        }
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (getResourcePath() != null ? getResourcePath().hashCode() : 0);
        return result;
    }

    private File getFileByPath(File root, String filePath) {
        while(filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }
        String[] directories = filePath.split("/");
        File directoryFile = root;
        logger.info("Root: " + root + ", Directories: " + Arrays.asList(directories));
//        logger.info("Root: " + root);
        // Go through the child directory. If a final file is found the the directoryFile is set otherwise it is null
        for(final String directory: directories) {
//            logger.info("Directory: " + directory);
            File[] children = directoryFile.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.equals(directory);
                    }
                }
            );
//            logger.info("Children: " + (children == null ? "empty array" : Arrays.asList(children)));
            if(children != null && children.length == 1) {
                directoryFile = children[0];
            } else {
                directoryFile = null;
                break;
            }
        }
        logger.info("File By Path found: " + directoryFile);
        return directoryFile;
    }
}

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

import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.io.ConnectorException;
import org.apache.sling.ide.io.SlingProject;
import org.apache.sling.ide.io.SlingResource;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/25/16.
 */
public class TestSlingProject
    implements SlingProject
{

    private Logger logger = Logger.getLogger(getClass().getName());

    //    private ServerConfiguration.Module module;
    private File baseDirFile;
    private SlingResource syncDirectory;

    public TestSlingProject(String baseDir) {
        baseDirFile = new File(baseDir);
        logger.info("Base Dir: " + baseDirFile);
        if(baseDirFile.exists()) {
            syncDirectory = new TestSlingResource(this, baseDirFile);
            logger.info("Sync Dir: " + syncDirectory);
        }
    }

    @Override
    public SlingResource findFileByPath(String path) {
        SlingResource ret = null;
//        String[] directories = path.split("/");
        File directoryFile = Utils.findFileByPath(path, baseDirFile);
//        // Go through the child directory. If a final file is found the the directoryFile is set otherwise it is null
//        for(final String directory: directories) {
//            File[] children = directoryFile.listFiles(
//                new FilenameFilter() {
//                    @Override
//                    public boolean accept(File dir, String name) {
//                        return name.equals(directory);
//                    }
//                }
//            );
//            if(children.length == 1) {
//                directoryFile = children[0];
//            } else {
//                directoryFile = null;
//                break;
//            }
//        }
        if(directoryFile != null) {
            ret = new TestSlingResource(this, directoryFile);
        }
        return ret;
    }

    @Override
    public SlingResource getSyncDirectory() {
        return syncDirectory;
    }

    public Filter loadFilter() throws ConnectorException {
        Filter filter = null;
//        // First check if the filter file is cached and if it isn't outdated. If it is found and not outdated
//        // then we just return this one. If the filter is outdated then we just reload if the cache file
//        // and if there is not file then we search for it. At the end we place both the file and filter in the cache.
//        Filter filter = module.getFilter();
//        VirtualFile filterFile = module.getFilterFile();
//        if(filter != null) {
//            if(Util.isOutdated(module.getFilterFile())) {
//                filter = null;
//            }
//        }
//        if(filterFile == null) {
//            // First we check if the META-INF folder was already found
//            VirtualFile metaInfFolder = module.getMetaInfFolder();
//            if(metaInfFolder == null) {
//                // Now go through the Maven Resource folder and check
//                ModuleProject moduleProject = module.getModuleProject();
//                for(String contentPath: moduleProject.getContentDirectoryPaths()) {
//                    if(contentPath.endsWith("/" + META_INF_FOLDER_NAME)) {
//                        metaInfFolder = module.getProject().getBaseDir().getFileSystem().findFileByPath(contentPath);
//                        module.setMetaInfFolder(metaInfFolder);
//                    }
//                }
//            }
//            if(metaInfFolder == null) {
//                // Lastly we check if we can find the folder somewhere in the maven project file system
//                ModuleProject moduleProject = module.getModuleProject();
//                VirtualFile test = module.getProject().getBaseDir().getFileSystem().findFileByPath(moduleProject.getModuleDirectory());
//                metaInfFolder = findFileOrFolder(test, META_INF_FOLDER_NAME, true);
//                module.setMetaInfFolder(metaInfFolder);
//            }
//            if(metaInfFolder != null) {
//                // Found META-INF folder
//                // Find filter.xml file
//                filterFile = findFileOrFolder(metaInfFolder, VAULT_FILTER_FILE_NAME, false);
//                module.setFilterFile(filterFile);
//                Util.setModificationStamp(filterFile);
//            }
//        }
//        if(filter == null && filterFile != null) {
//            FilterLocator filterLocator = Activator.getDefault().getFilterLocator();
//            InputStream contents = null;
//            try {
//                contents = filterFile.getInputStream();
//                filter = filterLocator.loadFilter(contents);
//                module.setFilter(filter);
//                Util.setModificationStamp(filterFile);
//            } catch (IOException e) {
//                throw new ConnectorException(
//                    "Failed loading filter file for module " + module
//                        + " from location " + filterFile,
//                    e
//                );
//            } finally {
//                IOUtils.closeQuietly(contents);
//            }
//        }
        return filter;
    }

//    private VirtualFile findFileOrFolder(VirtualFile rootFile, String name, boolean isFolder) {
//        VirtualFile ret = null;
//        for(VirtualFile child: rootFile.getChildren()) {
//            if(child.isDirectory()) {
//                if(isFolder) {
//                    if(child.getName().equals(name)) {
//                        return child;
//                    }
//                }
//                ret = findFileOrFolder(child, name, isFolder);
//                if(ret != null) { break; }
//            } else {
//                if(child.getName().equals(name)) {
//                    ret = child;
//                    break;
//                }
//            }
//        }
//        return ret;
//    }
}

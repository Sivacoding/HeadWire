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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/29/16.
 */
public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class.getName());

    public static File findFileByPath(String path, File baseDirectory) {
        if(baseDirectory == null) {
            throw new IllegalArgumentException("Base Directory must be provided");
        }
        if(path == null) {
            throw new IllegalArgumentException("Path must be provided");
        }
        if(!baseDirectory.exists() && !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base Directory :'" + baseDirectory + "' does not exists or is not a directory ");
        }
        // Any path is considered relative to the base directory
        File fileOrFolder = baseDirectory;
        List<String> directories = splitPath(path);
        // Go through the child directory. If a final file is found the the directoryFile is set otherwise it is null
        for(final String directory: directories) {
            // If there is another directory then it must be a folder otherwise
            File child = obtainChild(fileOrFolder, directory);
            if(child != null) {
                fileOrFolder = child;
            } else {
                fileOrFolder = null;
                break;
            }
        }
        return fileOrFolder;
    }

    public static File createFileAndFolders(String path, File baseDirectory, InputStream content) throws IOException {
        if(baseDirectory == null) {
            throw new IllegalArgumentException("Base Directory must be provided");
        }
        if(path == null) {
            throw new IllegalArgumentException("Path must be provided");
        }
        if(content == null) {
            throw new IllegalArgumentException("Content Input Stream must be provided");
        }
        if(!baseDirectory.exists() && !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base Directory :'" + baseDirectory + "' does not exists or is not a directory ");
        }
        // Any path is considered relative to the base directory
        File fileOrFolder = baseDirectory;
        List<String> directories = splitPath(path);
        String fileName = directories.get(directories.size() - 1);
        directories.remove(directories.size() - 1);
        // Go through the child directory and create them if not found
        for(final String directory: directories) {
            // If there is another directory then it must be a folder otherwise
            File child = obtainChild(fileOrFolder, directory);
            logger.info("Found File:'" + child + "' for directory: '" + directory + "'");
            if(child != null) {
                fileOrFolder = child;
            } else {
                fileOrFolder = new File(fileOrFolder, directory);
                if(!fileOrFolder.mkdir()) {
                    throw new IllegalArgumentException("Could not create folder: '" + directory + "' in folder: '" + fileOrFolder + "'");
                } else {
                    logger.info("Created File:'" + fileOrFolder + "' for directory: '" + directory + "'");
                }
            }
        }
        File file = new File(fileOrFolder, fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        logger.info("Copy Content");
        IOUtils.copy(content, outputStream);
        logger.info("Copy Done");
        IOUtils.closeQuietly(content);
        IOUtils.closeQuietly(outputStream);
        logger.info("Streams closed");
        return file;
    }

    public static File obtainChild(File directory, final String folderOrFileName) {
        if(!directory.exists() && !directory.isDirectory()) {
            throw new IllegalArgumentException("Directory :'" + directory + "' does not exists or is not a directory");
        }
        File ret = null;
        File[] children = directory.listFiles(
            new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.equals(folderOrFileName);
                }
            }
        );
        if(children.length == 1) {
            ret = children[0];
        }
        return ret;
    }

    public static List<String> splitPath(String path) {
        while(path.startsWith("/")) {
            if(path.length() == 1) {
                throw new IllegalArgumentException("Path contained only slashes");
            }
            path = path.substring(1);
        }
        if(path.length() == 0) {
            throw new IllegalArgumentException("Path is empty");
        }
        String[] directories = path.split("/");
        List<String> ret = new ArrayList<String>(Arrays.asList(directories));
        return ret;
    }
}

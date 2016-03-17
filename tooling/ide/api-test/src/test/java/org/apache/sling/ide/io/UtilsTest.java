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
package org.apache.sling.ide.io;

import org.apache.sling.ide.io.helpers.Utils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/29/16.
 */
public class UtilsTest {

    public static final String TEST_FOLDER = "./target/test-classes/resources";

    private InputStream noOPInputStream = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileAndFoldersWithNullBaseDirectory() throws IOException {
        Utils.createFileAndFolders("/test", null, noOPInputStream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileAndFoldersWithNullPath() throws IOException {
        Utils.createFileAndFolders(null, new File("/test"), noOPInputStream);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileAndFoldersWithNullContent() throws IOException {
        Utils.createFileAndFolders("/test", new File("/test"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateFileAndFoldersWithNonExistingBaseDirectory() throws IOException {
        Utils.createFileAndFolders("/test", new File("non-existing-folder"), noOPInputStream);
    }

    @Test
    public void testCreateFileAndFoldersWithEmptyPath() throws IOException {
        try {
            Utils.createFileAndFolders("", new File("target"), noOPInputStream);
        } catch(IllegalArgumentException e) {
            assertEquals("Wrong Error Message for Empty Path", "Path is empty", e.getMessage());
        }
    }

    @Test
    public void testCreateFileAndFoldersWithSlashesOnlyPath() throws IOException {
        try {
            Utils.createFileAndFolders("////", new File("target"), noOPInputStream);
        } catch(IllegalArgumentException e) {
            assertEquals("Wrong Error Message for Empty Path", "Path contained only slashes", e.getMessage());
        }
    }

    @Test
    public void testCreateFileAndFoldersWithEmptyContent() throws IOException {
        File testFolder = new File(TEST_FOLDER);
        if(testFolder.exists()) {
            testFolder.delete();
        }
        assertFalse("Test Folder: '" + TEST_FOLDER + "' could not be deleted", testFolder.exists());
        testFolder.mkdirs();
        assertTrue("Test Folder: '" + TEST_FOLDER + "' could not be created", testFolder.exists());
        Utils.createFileAndFolders("////one/test.xml", new File(TEST_FOLDER), noOPInputStream);
        // Make sure the folders and file were created
        assertTrue("Test Folder: '" + TEST_FOLDER + "' was not created", testFolder.exists());
        assertTrue("Test Folder: '" + TEST_FOLDER + "' is not a directory", testFolder.isDirectory());
        File subFolder = new File(TEST_FOLDER, "one");
        assertTrue("Test Sub Folder: '" + subFolder.getPath() + "' was not created", subFolder.exists());
        assertTrue("Test Sub Folder: '" + subFolder.getPath() + "' is not a directory", subFolder.isDirectory());
        File contentFile = new File(subFolder, "test.xml");
        assertTrue("Test File: '" + contentFile.getPath() + "' was not created", contentFile.exists());
        assertFalse("Test File: '" + contentFile.getPath() + "' is not a file", contentFile.isDirectory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindFileByPathWithNullBaseDirectory() throws IOException {
        Utils.findFileByPath("/test", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindFileByPathWithNullPath() throws IOException {
        Utils.findFileByPath(null, new File("/test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindFileByPathNonExistingBaseDirectory() throws IOException {
        Utils.findFileByPath("/test", new File("non-existing-folder"));
    }

    @Test
    public void testFindFileByPathWithEmptyPath() throws IOException {
        try {
            Utils.findFileByPath("", new File("target"));
        } catch(IllegalArgumentException e) {
            assertEquals("Wrong Error Message for Empty Path", "Path is empty", e.getMessage());
        }
    }

    @Test
    public void testFindFileByPathWithSlashesOnlyPath() throws IOException {
        try {
            Utils.findFileByPath("////", new File("target"));
        } catch(IllegalArgumentException e) {
            assertEquals("Wrong Error Message for Empty Path", "Path contained only slashes", e.getMessage());
        }
    }

    @Test
    public void testFindFileByPath() throws IOException {
        // Setup the environment
        Utils.createFileAndFolders("two/test.xml", new File(TEST_FOLDER), noOPInputStream);
        File file = Utils.findFileByPath("two/test.xml", new File(TEST_FOLDER));
        assertNotNull("File was not found", file);
        assertTrue("Found File does not exit", file.exists());
        assertTrue("Found File is not a file", file.isFile());
    }
}

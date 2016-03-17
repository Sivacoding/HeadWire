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

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sling.ide.impl.vlt.serialization.VltSerializationManager;
import org.apache.sling.ide.io.helpers.SpyCommand;
import org.apache.sling.ide.io.helpers.SpyRepository;
import org.apache.sling.ide.io.helpers.TestProject;
import org.apache.sling.ide.io.helpers.TestProjectProvider;
import org.apache.sling.ide.io.helpers.TestServiceProvider;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.impl.resource.serialization.SimpleXmlSerializationManager;

import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Created by Andreas Schaefer (Headwire.com) on 1/21/16.
 */
public class NewResourceChangeCommandFactoryTest {

    private Logger logger = Logger.getLogger(getClass().getName());

    @Rule
    public TestName name = new TestName();

    @Rule
    public TestProjectProvider projectRule = new TestProjectProvider();

    private TestProject project;
    private NewResourceChangeCommandFactory factory;
    private Repository spyRepo;

    @BeforeClass
    public static void setUpClass() {
        // Clean the Project Folder
        File projectFolder = new File(TestProject.ROOT_FOLDER);
        if(projectFolder.exists()) {
            deleteChildren(projectFolder);
        }
    }

    private static void deleteChildren(File directory) {
        if(directory.isDirectory()) {
            for(File child: directory.listFiles()) {
                if(child.isDirectory()) {
                    deleteChildren(child);
                }
                child.delete();
            }
        } else {
            directory.delete();
        }
    }

    @Before
    public void setUp() throws Exception {
        logger.log(Level.INFO, "\n\nStart Test Case: ''{0}'' --------------\n\n", name.getMethodName());

        ServiceFactory.setServiceProvider(new TestServiceProvider());
        spyRepo = new SpyRepository();
        project = projectRule.getProject();

        //AS Don't use the Simple XML Serialization Handler as it cannot deal with .content.xml files
        SerializationManager serializationManager = new VltSerializationManager();
        factory = new NewResourceChangeCommandFactory(serializationManager);
    }

    @After
    public void tearDown() throws Exception {
        logger.log(Level.INFO, "\n\nEnd Test Case: ''{0}'' --------------\n\n", name.getMethodName());
    }

    @Test
    public void commandForAddedOrUpdatedNtFolder() throws ConnectorException, IOException {
        loadContentXML(
            "sling-folder-nodetype.xml",
            "content/test-root/nested/.content.xml"
        );

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(
            spyRepo,
            project.getResource("content/test-root")
        );

        assertThat("command.path", command.getPath(), nullValue());
        assertThat("command.resource.path", command.getResourceProxy().getPath(), equalTo("/content/test-root"));
        assertThat("command.resource.properties", command.getResourceProxy().getProperties(),
            CoreMatchers.equalTo(singletonMap("jcr:primaryType", (Object) "nt:folder")));
        assertThat("command.fileinfo", command.getFileInfo(), nullValue());
        assertThat("command.kind", command.getSpyKind(), equalTo(SpyCommand.Kind.ADD_OR_UPDATE));
    }

    @Test
    public void commandForAddedOrUpdatedSlingFolder() throws ConnectorException, IOException {
        loadContentXML(
            "sling-folder-nodetype-with-title.xml",
            "content/test-root/nested/.content.xml"
        );

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(
            spyRepo,
            project.getResource("content/test-root/nested")
        );

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("jcr:primaryType", "sling:Folder");
        props.put("jcr:title", "Some Folder");

        assertThat("command.path", command.getPath(), nullValue());
        assertThat("command.resource.path", command.getResourceProxy().getPath(), equalTo("/content/test-root/nested"));
        assertThat("command.resource.properties", command.getResourceProxy().getProperties(), equalTo(props));
        assertThat("command.fileinfo", command.getFileInfo(), nullValue());
        assertThat("command.kind", command.getSpyKind(), equalTo(SpyCommand.Kind.ADD_OR_UPDATE));
    }

    @Test
    public void commandForSlingOrderedFolder_children() throws ConnectorException, IOException {
        loadContentXML(
            "sling-ordered-folder-with-children.xml",
            "content/test-root/.content.xml"
        );

        // create the child folder listed in the .content.xml file
        project.createFolder("/content/test-root/folder");

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(
            spyRepo,
            project.getResource("content/test-root")
        );

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(2));
    }

    @Test
    public void commandForSlingOrderedFolder_childrenMissingFromFilesystem() throws ConnectorException, IOException {
        loadContentXML(
            "sling-ordered-folder-with-children.xml",
            "content/test-root/.content.xml"
        );

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(
            spyRepo,
            project.getResource("content/test-root")
        );

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(1));
    }

    @Test
    public void commandForSlingOrderedFolder_extraChildrenInTheFilesystem() throws ConnectorException, IOException {
        loadContentXML(
            "sling-ordered-folder-with-children.xml",
            "content/test-root/.content.xml"
        );

        // create the child folder listed in the .content.xml file
        project.createFolder("/content/test-root/folder");
        // create an extra folder not listed in the .content.xml file
        project.createFolder("/content/test-root/folder2");

        SpyCommand<?> command = (SpyCommand<?>) factory.newCommandForAddedOrUpdated(
            spyRepo,
            project.getResource("content/test-root")
        );

        List<ResourceProxy> children = command.getResourceProxy().getChildren();

        assertThat("command.resource.children.size", children.size(), equalTo(3));
        assertThat("command.resource.children[2].name", (new File(children.get(2).getPath())).getName(), equalTo("folder2"));
    }

    private void loadContentXML(String sourceFileName, String targetFilePath) throws IOException {
        InputStream childContentXml = getClass().getResourceAsStream(sourceFileName);
        logger.info("Child Content Input Stream: " + childContentXml);
        project.createOrUpdateFile(targetFilePath, childContentXml);
    }
}

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

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.FileInfo2;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.util.PathUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * The <tt>ResourceChangeCommandFactory</tt> creates new {@link # Command commands} correspoding to resource addition,
 * change, or removal
 *
 */
public class NewResourceChangeCommandFactory {

    public static final String DIR_EXTENSION = ".dir";

    private final Set<String> ignoredFileNames = new HashSet<String>();
    {
        ignoredFileNames.add(".vlt");
        ignoredFileNames.add(".vltignore");
    }

    private final SerializationManager serializationManager;

    public NewResourceChangeCommandFactory(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
    }

    public Command<?> newCommandForAddedOrUpdated(Repository repository, SlingResource addedOrUpdated) throws ConnectorException {
        return newCommandForAddedOrUpdated(repository, addedOrUpdated, false);
    }

    public Command<?> newCommandForAddedOrUpdated(Repository repository, SlingResource addedOrUpdated, boolean forcedUpdate) throws ConnectorException {
        try {
            return addFileCommand(repository, addedOrUpdated, forcedUpdate);
        } catch (IOException e) {
            //AS TODO: I18N independented message?
            throw new ConnectorException("Failed updating " + addedOrUpdated, e);
        }
    }

    private Command<?> addFileCommand(Repository repository, SlingResource resource, boolean forcedUpdate) throws ConnectorException, IOException {

        ResourceAndInfo rai = buildResourceAndInfo(resource, repository, forcedUpdate);
        
        if (rai == null) {
            return null;
        }
        
        CommandContext context = new CommandContext(resource.loadFilter());

        if (rai.isOnlyWhenMissing()) {
            return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource(),
                    Repository.CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING);
        }

        return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource());
    }

    /**
     * Convenience method which builds a <tt>ResourceAndInfo</tt> info for a specific <tt>IResource</tt>
     * 
     * @param resource the resource to process
     * @param repository the repository, used to extract serialization information for different resource types
     * @return the build object, or null if one could not be built
     * @throws ConnectorException
     * @throws IOException
     */
    public ResourceAndInfo buildResourceAndInfo(SlingResource resource, Repository repository, boolean forcedUpdate) throws ConnectorException,
            IOException {
        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        if(!(forcedUpdate || resource.isModified())) {
            ServiceFactory.getPluginLogger().trace("Change for resource {0} ignored because it wasn't modified", resource);
            return null;
        }

        if (resource.isLocalOnly()) {
            ServiceFactory.getPluginLogger().trace("Skipping IDE local resource {0}", resource);
            return null;
        }

        FileInfo2 info = createFileInfo(resource);
        ServiceFactory.getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);

        // THe sync directory is the root resource with only an IDE part
        SlingResource syncDirectory = resource.getSyncDirectory();

        Filter filter = resource.loadFilter();

        ResourceProxy resourceProxy = null;

        if (serializationManager.isSerializationFile(resource.getLocalPath())) {
            InputStream contents = null;
            try {
                contents = resource.getContentStream();
                String resourceLocation = resource.getResourcePath();
                resourceProxy = serializationManager.readSerializationData(resourceLocation, contents);
                normaliseResourceChildren(resource, resourceProxy, syncDirectory, repository);


                // TODO - not sure if this 100% correct, but we definitely should not refer to the FileInfo as the
                // .serialization file, since for nt:file/nt:resource nodes this will overwrite the file contents
                String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
                if (Repository.NT_FILE.equals(primaryType)) {
                    // TODO move logic to serializationManager
                    SlingResource parent = resource.getParent();
                    int endIndex = parent.getName().length() - DIR_EXTENSION.length();
                    String newName = parent.getName().substring(0, endIndex);
                    SlingResource child = parent.getParent().findChild(newName);
                    info = new FileInfo2(child, newName);

                    ServiceFactory.getPluginLogger()
                            .trace("Adjusted original name from {0} to {1}", resource.getName(), newName);

                }

            } catch (IOException e) {
//AS TODO: Implement this in an IDE independent way
//                Status s = new Status(Status.WARNING, Activator.PLUGIN_ID, "Failed reading file at "
//                        + resource.getFullPath(), e);
//                StatusManager.getManager().handle(s, StatusManager.LOG | StatusManager.SHOW);
                return null;
            } finally {
                IOUtils.closeQuietly(contents);
            }
        } else {

            // TODO - move logic to serializationManager
            // possible .dir serialization holder
//AS TODO: It looks like there is also the possibility that the .content.xml file was handled but there is a folder
//AS TODO: that is named '_jcr_content' which will corrupt the original setting
//            if(resource.isFolder()) {
                if(
                    (resource.getLocalPath().contains("/_jcr_content/") ||
                        resource.getLocalPath().endsWith("/_jcr_content"))
                    && !resource.getLocalPath().contains("/_jcr_content/renditions")
                ) {
//                if(resource.getLocalPath().contains("_jcr_content")) {
                    SlingResource parent = resource.findInParentByName("_jcr_content");
                    if(parent != null) {
                        SlingResource contentXml = parent.getParent().findChild(".content.xml");
                        if (contentXml != null && contentXml.existsLocally()
                            && serializationManager.isSerializationFile(contentXml.getResourcePath())) {
                            return null;
                        }
                    }
                }
                if(resource.getName().endsWith(DIR_EXTENSION)) {
                    SlingResource contentXml = resource.findChild(".content.xml");
                    // .dir serialization holder ; nothing to process here, the .content.xml will trigger the actual work
                    if (contentXml != null && contentXml.existsLocally()
                        && serializationManager.isSerializationFile(contentXml.getResourcePath())) {
                        return null;
                    }
                }
//            }

            resourceProxy = buildResourceProxyForPlainFileOrFolder(resource, syncDirectory, repository);
        }

        FilterResult filterResult = getFilterResult(resource, resourceProxy, filter);

        switch (filterResult) {

            case ALLOW:
                return new ResourceAndInfo(resourceProxy, info);
            case PREREQUISITE:
                // never try to 'create' the root node, we assume it exists
                if (!resourceProxy.getPath().equals("/")) {
                    // we don't explicitly set the primary type, which will allow the the repository to choose the best
                    // suited one ( typically nt:unstructured )
                    return new ResourceAndInfo(new ResourceProxy(resourceProxy.getPath()), null, true);
                }
            case DENY: // falls through
            default:
                return null;
        }
    }

    private FileInfo2 createFileInfo(SlingResource resource) throws ConnectorException {

        if (resource.isFolder()) {
            return null;
        }

        FileInfo2 info = new FileInfo2(resource, resource.getName());

        ServiceFactory.getPluginLogger().trace("For {0} built fileInfo {1}", resource, info);

        return info;
    }

    /**
     * Gets the filter result for a resource/resource proxy combination
     * 
     * <p>
     * The resourceProxy may be null, typically when a resource is already deleted.
     * 
     * <p>
     * The filter may be null, in which case all combinations are included in the filed, i.e. allowed.
     * 
     * @param resource the resource to filter for, must not be <code>null</code>
     * @param resourceProxy the resource proxy to filter for, possibly <code>null</code>
     * @param filter the filter to use, possibly <tt>null</tt>
     * @return the filtering result, never <code>null</code>
     */
    private FilterResult getFilterResult(SlingResource resource, ResourceProxy resourceProxy, Filter filter) {

        if (filter == null) {
            return FilterResult.ALLOW;
        }

        String repositoryPath = resourceProxy != null ?
            resourceProxy.getPath() :
            getRepositoryPathForDeletedResource(resource);

        FilterResult filterResult = filter.filter(repositoryPath);

        ServiceFactory.getPluginLogger().trace("Filter result for {0} for {1}", repositoryPath, filterResult);

        return filterResult;
    }

    private String getRepositoryPathForDeletedResource(SlingResource resource/*, SlingResource contentSyncResource*/) {
        String repositoryPath = serializationManager.getRepositoryPath(
            resource.getResourcePath()
        );

        ServiceFactory.getPluginLogger()
                .trace("Repository path for deleted resource {0} is {1}", resource, repositoryPath);

        return repositoryPath;
    }

    private ResourceProxy buildResourceProxyForPlainFileOrFolder(SlingResource changedResource, SlingResource syncDirectory,
            Repository repository)
            throws ConnectorException, IOException {

        SerializationKind serializationKind;
        String fallbackNodeType;
        if (changedResource.isFile()) {
            serializationKind = SerializationKind.FILE;
            fallbackNodeType = Repository.NT_FILE;
            java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization File: ''{0}''", serializationKind);
        } else { // i.e. IResource.FOLDER
            serializationKind = SerializationKind.FOLDER;
            fallbackNodeType = Repository.NT_FOLDER;
            java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization Folder: ''{0}''", serializationKind);
        }

        String resourceLocation = changedResource.getResourcePath();
        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Resource Location: ''{0}''", resourceLocation);

        //AS NOTE: not sure what to do here. The Serialization Manager might not be changeable and so we have to deal
        //AS NOTE: OS specific file paths but then where to put the logic?
        String serializationFilePath = serializationManager.getSerializationFilePath(resourceLocation, serializationKind);
        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization File Path: {0}", serializationFilePath);
        SlingResource serializationResource = syncDirectory.getResourceFromPath(serializationFilePath);

        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization Resource: {0}", serializationResource);
        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Changed Resource is folder: {0}", changedResource.isFolder());

        if (serializationResource == null && changedResource.isFolder()) {
            ResourceProxy dataFromCoveringParent = findSerializationDataFromCoveringParent(
                changedResource,
                syncDirectory,
                resourceLocation,
//AS TODO: It turns out that the serialization file path is the relative not absolute
                syncDirectory.findChildByPath(serializationFilePath)
            );

            if (dataFromCoveringParent != null) {
                return dataFromCoveringParent;
            }
        }
        return buildResourceProxy(resourceLocation, serializationResource, syncDirectory, fallbackNodeType, repository);
    }

    /**
     * Tries to find serialization data from a resource in a covering parent
     * 
     * <p>
     * If the serialization resource is null, it's valid to look for a serialization resource higher in the filesystem,
     * given that the found serialization resource covers this resource
     * 
     * @param changedResource the resource which has changed
     * @param syncDirectory the content sync directory for the resource's project
     * @param resourceLocation the resource location relative to the sync directory
     * @param serializationFilePath the location
     * @return a <tt>ResourceProxy</tt> if there is a covering parent, or null is there is not
     * @throws ConnectorException
     * @throws IOException
     */
    private ResourceProxy findSerializationDataFromCoveringParent(
        SlingResource changedResource, SlingResource syncDirectory,
        String resourceLocation, SlingResource serializationFilePath
    ) throws ConnectorException, IOException {

        // TODO - this too should be abstracted in the service layer, rather than in the Eclipse-specific code

        PluginLogger logger = ServiceFactory.getPluginLogger();
        logger.trace("Found plain nt:folder candidate at {0}, trying to find a covering resource for it",
                changedResource.getResourcePath());
        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Found plain nt:folder candidate at {0}, trying to find a covering resource for it", changedResource.getResourcePath());
        // don't use isRoot() to prevent infinite loop when the final path is '//'
        while ((serializationFilePath = serializationFilePath.getParent()) != null) {
            if (!serializationFilePath.existsLocally()) {
                logger.trace("No folder found at {0}, moving up to the next level", serializationFilePath);
                continue;
            }

            // it's safe to use a specific SerializationKind since this scenario is only valid for METADATA_PARTIAL
            // coverage
            String possibleSerializationFilePath = serializationManager.getSerializationFilePath(
                serializationFilePath.getLocalPath(),
                SerializationKind.METADATA_PARTIAL
            );

            logger.trace("Looking for serialization data in {0}", possibleSerializationFilePath);

            if (serializationManager.isSerializationFile(possibleSerializationFilePath)) {
                SlingResource possibleSerializationResource = syncDirectory.getResourceFromPath(possibleSerializationFilePath);
                if (!possibleSerializationResource.existsLocally()) {
                    logger.trace("Potential serialization data file {0} does not exist, moving up to the next level",
                        possibleSerializationResource.getResourcePath());
                    continue;
                }

                InputStream contents = possibleSerializationResource.getContentStream();
                ResourceProxy serializationData;
                try {
                    serializationData = serializationManager.readSerializationData(
                        possibleSerializationResource.getLocalPath(), contents);
                } finally {
                    IOUtils.closeQuietly(contents);
                }

                String repositoryPath = serializationManager.getRepositoryPath(resourceLocation);
                String potentialPath = serializationData.getPath();
                boolean covered = serializationData.covers(repositoryPath);

                logger.trace(
                        "Found possible serialization data at {0}. Resource :{1} ; our resource: {2}. Covered: {3}",
                    possibleSerializationResource, potentialPath, repositoryPath, covered);
                // note what we don't need to normalize the children here since this resource's data is covered by
                // another resource
                if (covered) {
                    return serializationData.getChild(repositoryPath);
                }

                break;
            }
        }

        return null;
    }

    private ResourceProxy buildResourceProxy(String resourceLocation, SlingResource serializationResource,
                                             SlingResource syncDirectory, String fallbackPrimaryType, Repository repository
    ) throws ConnectorException, IOException {
        java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization Resource: ''{0}''", serializationResource);
        if (serializationResource != null && serializationResource.isFile() && serializationResource.existsLocally()) {
            InputStream contents = null;
            try {
                contents = serializationResource.getContentStream();
//AS TODO: I think that path needs to be make OS specific
                String serializationFilePath =  serializationResource.getResourceLocalPath();
                java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Serialization File Path: {0}", serializationFilePath);
                ResourceProxy resourceProxy = serializationManager.readSerializationData(serializationFilePath, contents);
                java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Resource Proxy Path: {0}", resourceProxy.getPath());
                java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Resource Proxy Properties: {0}", resourceProxy.getProperties());
                normaliseResourceChildren(serializationResource, resourceProxy, syncDirectory, repository);
                java.util.logging.Logger.getLogger(getClass().getName()).log(Level.INFO, "Resource Proxy: ''{0}''", resourceProxy);

                return resourceProxy;
            } finally {
                IOUtils.closeQuietly(contents);
            }
        }

        return new ResourceProxy(serializationManager.getRepositoryPath(resourceLocation), Collections.singletonMap(
                Repository.JCR_PRIMARY_TYPE, (Object) fallbackPrimaryType));
    }

    /**
     * Normalises the of the specified <tt>resourceProxy</tt> by comparing the serialization data and the filesystem
     * data
     * 
     * @param serializationFile the file which contains the serialization data
     * @param resourceProxy the resource proxy
     * @param syncDirectory the sync directory
     * @param repository TODO
     * @throws ConnectorException
     */
    private void normaliseResourceChildren(SlingResource serializationFile, ResourceProxy resourceProxy, SlingResource syncDirectory,
            Repository repository) throws ConnectorException {

        // TODO - this logic should be moved to the serializationManager
        try {
            SerializationKindManager skm = new SerializationKindManager();
            skm.init(repository);

            String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
            List<String> mixinTypesList = getMixinTypes(resourceProxy);
            SerializationKind serializationKind = skm.getSerializationKind(primaryType, mixinTypesList);

            if (serializationKind == SerializationKind.METADATA_FULL) {
                return;
            }
        } catch (RepositoryException e) {
            //AS TODO: I18N independented message?
            throw new ConnectorException("Failed creating a " + SerializationDataBuilder.class.getName(), e);
        }

        //AS TODO: What is the difference between Remove Last Seqment and Get Parent (Local vs Sling?)
        SlingResource serializationDirectoryPath = serializationFile.getParent();

        Iterator<ResourceProxy> childIterator = resourceProxy.getChildren().iterator();
        Map<String, SlingResource> extraChildResources = new HashMap<String, SlingResource>();
        for (SlingResource member : serializationFile.getParent().getLocalChildren()) {
            if (member.equals(serializationFile)) {
                continue;
            }
            extraChildResources.put(member.getName(), member);
        }

        while (childIterator.hasNext()) {
            ResourceProxy child = childIterator.next();
            String childName = PathUtil.getName(child.getPath());
            //AS Note: OS refers here to File System representation rather than JCR name like '_cq_content' for cq:content
            String osPath = serializationManager.getOsPath(childName);

            // covered children might have a FS representation, depending on their child nodes, so
            // accept a directory which maps to their name
            extraChildResources.remove(osPath);

            // covered children do not need a filesystem representation
            if (resourceProxy.covers(child.getPath())) {
                continue;
            }

            SlingResource childResource = serializationDirectoryPath.findChild(osPath);

            if (!childResource.existsLocally()) {
                ServiceFactory.getPluginLogger()
                        .trace("For resource at with serialization data {0} the serialized child resource at {1} does not exist in the filesystem and will be ignored",
                                serializationFile, childResource.getLocalPath());
                childIterator.remove();
            }
        }

        for ( SlingResource extraChildResource : extraChildResources.values()) {
            String resourcePath = extraChildResource.getResourcePath();
            String serializedPath = serializationManager.getRepositoryPath(resourcePath);
            String resourceProxyPath = resourceProxy.getPath();
            if(!resourceProxyPath.equals(serializedPath)) {
                resourceProxy.addChild(
                    new ResourceProxy(
                        serializedPath
                    )
                );

                ServiceFactory.getPluginLogger()
                    .trace("For resource at with serialization data {0} the found a child resource at {1} which is not listed in the serialized child resources and will be added",
                        serializationFile, extraChildResource);
            }
        }
    }

    private List<String> getMixinTypes(ResourceProxy resourceProxy) {

        Object mixinTypesProp = resourceProxy.getProperties().get(Repository.JCR_MIXIN_TYPES);

        if (mixinTypesProp == null) {
            return Collections.emptyList();
        }

        if (mixinTypesProp instanceof String) {
            return Collections.singletonList((String) mixinTypesProp);
        }

        return Arrays.asList((String[]) mixinTypesProp);
    }

    public Command<?> newCommandForRemovedResources(Repository repository, SlingResource removed) throws ConnectorException {
        
        try {
            return removeFileCommand(repository, removed);
        } catch (IOException e) {
            //AS TODO: I18N independented message?
            throw new ConnectorException("Failed removing" + removed, e);
        }
    }

    private Command<?> removeFileCommand(Repository repository, SlingResource resource) throws ConnectorException, IOException {

        if (resource.isLocalOnly()) {
            ServiceFactory.getPluginLogger().trace("Skipping team-private resource {0}", resource);
            return null;
        }

        if (ignoredFileNames.contains(resource.getName())) {
            return null;
        }

        SlingResource syncDirectory = resource.getSyncDirectory();

        Filter filter = syncDirectory.loadFilter();

        FilterResult filterResult = getFilterResult(resource, null, filter);
        if (filterResult == FilterResult.DENY || filterResult == FilterResult.PREREQUISITE) {
            return null;
        }
        
        String resourceLocation = getRepositoryPathForDeletedResource(
            resource
        );
        
        // verify whether a resource being deleted does not signal that the content structure
        // was rearranged under a covering parent aggregate
        String serializationFilePath = serializationManager.getSerializationFilePath(
            resourceLocation,
            SerializationKind.FOLDER
        );

        ResourceProxy coveringParentData = findSerializationDataFromCoveringParent(
            resource, syncDirectory,
            resourceLocation,
            syncDirectory.getResourceFromPath(serializationFilePath)
        );
        if (coveringParentData != null) {
            ServiceFactory
                    .getPluginLogger()
                    .trace("Found covering resource data ( repository path = {0} ) for resource at {1},  skipping deletion and performing an update instead",
                            coveringParentData.getPath(), resource.getLocalPath());
            FileInfo2 info = createFileInfo(resource);
            return repository.newAddOrUpdateNodeCommand(new CommandContext(filter), info, coveringParentData);
        }
        
        return repository.newDeleteNodeCommand(serializationManager.getRepositoryPath(resourceLocation));
    }

    public Command<Void> newReorderChildNodesCommand(Repository repository, SlingResource res) throws ConnectorException {

        try {
            ResourceAndInfo rai = buildResourceAndInfo(res, repository, false);

            if (rai == null || rai.isOnlyWhenMissing()) {
                return null;
            }

            return repository.newReorderChildNodesCommand(rai.getResource());
        } catch (IOException e) {
            //AS TODO: I18N independented message?
            throw new ConnectorException("Failed reordering child nodes for " + res, e);
        }
    }
}

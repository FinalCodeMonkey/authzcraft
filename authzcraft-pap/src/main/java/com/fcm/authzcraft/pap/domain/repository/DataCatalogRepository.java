package com.fcm.authzcraft.pap.domain.repository;

import java.util.List;

import com.fcm.authzcraft.pap.domain.model.AccessPath;
import com.fcm.authzcraft.pap.domain.model.RelationResource;
import com.fcm.authzcraft.pap.domain.model.ResourceField;

public interface DataCatalogRepository {

    void saveRelationResource(RelationResource resource);

    RelationResource findRelationResource(Long id);
    List<RelationResource> searchRelationResources(String tenantKey, String appKey, String resourceKey, String keyword,
                                                   String lifecycleState);
    void updateRelationResource(RelationResource resource);

    List<ResourceField> loadFieldsFromDatabase(RelationResource resource);
    void replaceImportedFields(Long relationResourceId, List<ResourceField> fields);
    List<ResourceField> searchResourceFields(Long relationResourceId, Boolean filterableFlag, Boolean joinableFlag,
                                             String lifecycleState);
    ResourceField findResourceField(Long relationResourceId, Long fieldId);
    void updateResourceField(ResourceField field);
    void updateStructureDigest(Long relationResourceId, String structureDigest);

    void saveAccessPath(AccessPath accessPath);
    AccessPath findAccessPath(Long id);
    List<AccessPath> searchAccessPaths(String tenantKey, String appKey, String pathKey, Long rootRelationId,
                                       String lifecycleState);
    void updateAccessPath(AccessPath accessPath);
}
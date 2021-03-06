/*
* Copyright 2015 herd contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.finra.herd.model.jpa;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * An instance of business object data notification.
 */
@XmlRootElement
@XmlType
@Entity
@DiscriminatorValue(NotificationTypeEntity.NOTIFICATION_TYPE_BDATA)
public class BusinessObjectDataNotificationRegistrationEntity extends NotificationRegistrationEntity
{
    @ManyToOne
    @JoinColumn(name = "bus_objct_dfntn_id", referencedColumnName = "bus_objct_dfntn_id")
    private BusinessObjectDefinitionEntity businessObjectDefinition;

    /**
     * The usage column.
     */
    @Column(name = "usage_cd")
    private String usage;

    @ManyToOne
    @JoinColumn(name = "file_type_cd", referencedColumnName = "file_type_cd")
    private FileTypeEntity fileType;

    /**
     * The business object format version column.
     */
    @Column(name = "frmt_vrsn_nb")
    private Integer businessObjectFormatVersion;

    /**
     * The storage column.
     */
    @ManyToOne
    @JoinColumn(name = "strge_cd", referencedColumnName = "strge_cd")
    private StorageEntity storage;

    /**
     * The new business object data status column.
     */
    @ManyToOne
    @JoinColumn(name = "new_bus_objct_data_stts_cd", referencedColumnName = "bus_objct_data_stts_cd")
    private BusinessObjectDataStatusEntity newBusinessObjectDataStatus;

    /**
     * The old business object data status column.
     */
    @ManyToOne
    @JoinColumn(name = "old_bus_objct_data_stts_cd", referencedColumnName = "bus_objct_data_stts_cd")
    private BusinessObjectDataStatusEntity oldBusinessObjectDataStatus;

    @OneToMany(mappedBy = "notificationRegistration", orphanRemoval = true, cascade = {CascadeType.ALL})
    @OrderBy("id")
    private Collection<NotificationActionEntity> notificationActions;

    public BusinessObjectDefinitionEntity getBusinessObjectDefinition()
    {
        return businessObjectDefinition;
    }

    public void setBusinessObjectDefinition(BusinessObjectDefinitionEntity businessObjectDefinition)
    {
        this.businessObjectDefinition = businessObjectDefinition;
    }

    public String getUsage()
    {
        return usage;
    }

    public void setUsage(String usage)
    {
        this.usage = usage;
    }

    public FileTypeEntity getFileType()
    {
        return fileType;
    }

    public void setFileType(FileTypeEntity fileType)
    {
        this.fileType = fileType;
    }

    public Integer getBusinessObjectFormatVersion()
    {
        return businessObjectFormatVersion;
    }

    public void setBusinessObjectFormatVersion(Integer businessObjectFormatVersion)
    {
        this.businessObjectFormatVersion = businessObjectFormatVersion;
    }

    public StorageEntity getStorage()
    {
        return storage;
    }

    public void setStorage(StorageEntity storage)
    {
        this.storage = storage;
    }

    public BusinessObjectDataStatusEntity getNewBusinessObjectDataStatus()
    {
        return newBusinessObjectDataStatus;
    }

    public void setNewBusinessObjectDataStatus(BusinessObjectDataStatusEntity newBusinessObjectDataStatus)
    {
        this.newBusinessObjectDataStatus = newBusinessObjectDataStatus;
    }

    public BusinessObjectDataStatusEntity getOldBusinessObjectDataStatus()
    {
        return oldBusinessObjectDataStatus;
    }

    public void setOldBusinessObjectDataStatus(BusinessObjectDataStatusEntity oldBusinessObjectDataStatus)
    {
        this.oldBusinessObjectDataStatus = oldBusinessObjectDataStatus;
    }

    public Collection<NotificationActionEntity> getNotificationActions()
    {
        return notificationActions;
    }

    public void setNotificationActions(Collection<NotificationActionEntity> notificationActions)
    {
        this.notificationActions = notificationActions;
    }
}

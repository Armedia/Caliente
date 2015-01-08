package com.delta.cmsmf.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;

import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfContentCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfEnumeration;
import com.documentum.fc.client.IDfFormat;
import com.documentum.fc.client.IDfPermit;
import com.documentum.fc.client.IDfRelation;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfType;
import com.documentum.fc.client.IDfValidator;
import com.documentum.fc.client.IDfVersionLabels;
import com.documentum.fc.client.IDfVersionPolicy;
import com.documentum.fc.client.IDfVirtualDocument;
import com.documentum.fc.client.acs.IDfAcsTransferPreferences;
import com.documentum.fc.client.acs.IDfContentTransferCapability;
import com.documentum.fc.client.content.IDfContentAvailability;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfAttr;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfList;
import com.documentum.fc.common.IDfTime;
import com.documentum.fc.common.IDfValue;

public class DfVersionTreePatch implements IDfDocument {
	private final IDfDocument base;

	public DfVersionTreePatch(IDfDocument base) {
		this.base = base;
	}

	@Override
	public IDfRelation addChildRelative(String relationTypeName, IDfId childId, String childLabel, boolean isPermanent,
		String description) throws DfException {
		return this.base.addChildRelative(relationTypeName, childId, childLabel, isPermanent, description);
	}

	@Override
	public IDfId addDigitalSignature(String userName, String reason) throws DfException {
		return this.base.addDigitalSignature(userName, reason);
	}

	@Override
	public IDfId addESignature(String userName, String password, String signatureJustification, String formatToSign,
		String hashAlgorithm, String preSignatureHash, String signatureMethodName, String applicationProperties,
		String passThroughArgument1, String passThroughArgument2) throws DfException {
		return this.base.addESignature(userName, password, signatureJustification, formatToSign, hashAlgorithm,
			preSignatureHash, signatureMethodName, applicationProperties, passThroughArgument1, passThroughArgument2);
	}

	@Override
	public void addNote(IDfId targetId, boolean keepPermanent) throws DfException {
		this.base.addNote(targetId, keepPermanent);
	}

	@Override
	public IDfRelation addParentRelative(String relationTypeName, IDfId parentId, String childLabel,
		boolean isPermanent, String description) throws DfException {
		return this.base.addParentRelative(relationTypeName, parentId, childLabel, isPermanent, description);
	}

	@Override
	public IDfId addReference(IDfId folderId, String bindingCondition, String bindingLabel) throws DfException {
		return this.base.addReference(folderId, bindingCondition, bindingLabel);
	}

	@Override
	public void addRendition(String fileName, String formatName) throws DfException {
		this.base.addRendition(fileName, formatName);
	}

	@Override
	public void addRenditionEx(String fileName, String formatName, int pageNumber, String storageName, boolean atomic)
		throws DfException {
		this.base.addRenditionEx(fileName, formatName, pageNumber, storageName, atomic);
	}

	@Override
	public void addRenditionEx2(String fileName, String formatName, int pageNumber, String pageModifier,
		String storageName, boolean atomic, boolean keep, boolean batch) throws DfException {
		this.base.addRenditionEx2(fileName, formatName, pageNumber, pageModifier, storageName, atomic, keep, batch);
	}

	@Override
	public void addRenditionEx3(String fileName, String formatName, int pageNumber, String pageModifier,
		String storageName, boolean atomic, boolean keep, boolean batch, String otherFileName) throws DfException {
		this.base.addRenditionEx3(fileName, formatName, pageNumber, pageModifier, storageName, atomic, keep, batch,
			otherFileName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean apiExec(String cmd, String args) throws DfException {
		return this.base.apiExec(cmd, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String apiGet(String cmd, String args) throws DfException {
		return this.base.apiGet(cmd, args);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean apiSet(String cmd, String args, String value) throws DfException {
		return this.base.apiSet(cmd, args, value);
	}

	@Override
	public void appendBoolean(String attributeName, boolean value) throws DfException {
		this.base.appendBoolean(attributeName, value);
	}

	@Override
	public void appendContent(ByteArrayOutputStream content) throws DfException {
		this.base.appendContent(content);
	}

	@Override
	public void appendContentEx(ByteArrayOutputStream content, boolean other) throws DfException {
		this.base.appendContentEx(content, other);
	}

	@Override
	public void appendDouble(String attributeName, double value) throws DfException {
		this.base.appendDouble(attributeName, value);
	}

	@Override
	public void appendFile(String fileName) throws DfException {
		this.base.appendFile(fileName);
	}

	@Override
	public void appendFileEx(String fileName, String otherFileName) throws DfException {
		this.base.appendFileEx(fileName, otherFileName);
	}

	@Override
	public void appendId(String attributeName, IDfId value) throws DfException {
		this.base.appendId(attributeName, value);
	}

	@Override
	public void appendInt(String attributeName, int value) throws DfException {
		this.base.appendInt(attributeName, value);
	}

	@Override
	public IDfId appendPart(IDfId componentId, String versionLabel, boolean useNodeVerLabel, boolean followAssembly,
		int copyChild) throws DfException {
		return this.base.appendPart(componentId, versionLabel, useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public void appendString(String attributeName, String value) throws DfException {
		this.base.appendString(attributeName, value);
	}

	@Override
	public void appendTime(String attributeName, IDfTime value) throws DfException {
		this.base.appendTime(attributeName, value);
	}

	@Override
	public void appendValue(String attributeName, IDfValue value) throws DfException {
		this.base.appendValue(attributeName, value);
	}

	@Override
	public boolean areAttributesModifiable() throws DfException {
		return this.base.areAttributesModifiable();
	}

	@Override
	public IDfVirtualDocument asVirtualDocument(String lateBindingValue, boolean followRootAssembly) throws DfException {
		return this.base.asVirtualDocument(lateBindingValue, followRootAssembly);
	}

	@Override
	public IDfCollection assemble(IDfId virtualDocId, int interruptFreq, String qualification, String nodesortList)
		throws DfException {
		return this.base.assemble(virtualDocId, interruptFreq, qualification, nodesortList);
	}

	@Override
	public void attachPolicy(IDfId policyId, String state, String scope) throws DfException {
		this.base.attachPolicy(policyId, state, scope);
	}

	@Override
	public void bindFile(int pageNumber, IDfId srcId, int srcPageNumber) throws DfException {
		this.base.bindFile(pageNumber, srcId, srcPageNumber);
	}

	@Override
	public IDfId branch(String versionLabel) throws DfException {
		return this.base.branch(versionLabel);
	}

	@Override
	public boolean canDemote() throws DfException {
		return this.base.canDemote();
	}

	@Override
	public boolean canPromote() throws DfException {
		return this.base.canPromote();
	}

	@Override
	public boolean canResume() throws DfException {
		return this.base.canResume();
	}

	@Override
	public boolean canSuspend() throws DfException {
		return this.base.canSuspend();
	}

	@Override
	public void cancelCheckout() throws DfException {
		this.base.cancelCheckout();
	}

	@Override
	public void cancelCheckoutEx(boolean sendMail, String compoundValue, String specialValue) throws DfException {
		this.base.cancelCheckoutEx(sendMail, compoundValue, specialValue);
	}

	@Override
	public void cancelScheduledDemote(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledDemote(scheduleDate);
	}

	@Override
	public void cancelScheduledPromote(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledPromote(scheduleDate);
	}

	@Override
	public void cancelScheduledResume(IDfTime schedule) throws DfException {
		this.base.cancelScheduledResume(schedule);
	}

	@Override
	public void cancelScheduledSuspend(IDfTime scheduleDate) throws DfException {
		this.base.cancelScheduledSuspend(scheduleDate);
	}

	@Override
	public IDfId checkin(boolean keepLock, String versionLabels) throws DfException {
		return this.base.checkin(keepLock, versionLabels);
	}

	@Override
	public IDfId checkinEx(boolean keepLock, String versionLabels, String oldCompoundArchValue,
		String oldSpecialAppValue, String newCompoundArchValue, String newSpecialAppValue) throws DfException {
		return this.base.checkinEx(keepLock, versionLabels, oldCompoundArchValue, oldSpecialAppValue,
			newCompoundArchValue, newSpecialAppValue);
	}

	@Override
	public void checkout() throws DfException {
		this.base.checkout();
	}

	@Override
	public IDfId checkoutEx(String versionLabel, String compoundArchValue, String specialAppValue) throws DfException {
		return this.base.checkoutEx(versionLabel, compoundArchValue, specialAppValue);
	}

	@Override
	public void demote(String state, boolean toBase) throws DfException {
		this.base.demote(state, toBase);
	}

	@Override
	public void destroy() throws DfException {
		this.base.destroy();
	}

	@Override
	public void destroyAllVersions() throws DfException {
		this.base.destroyAllVersions();
	}

	@Override
	public void detachPolicy() throws DfException {
		this.base.detachPolicy();
	}

	@Override
	public void disassemble() throws DfException {
		this.base.disassemble();
	}

	@Override
	public String dump() throws DfException {
		return this.base.dump();
	}

	@Override
	public Enumeration<?> enumAttrs() throws DfException {
		return this.base.enumAttrs();
	}

	@Override
	public boolean fetch(String typeNameIgnored) throws DfException {
		return this.base.fetch(typeNameIgnored);
	}

	@Override
	public boolean fetchWithCaching(String currencyCheckValue, boolean usePersistentCache, boolean useSharedCache)
		throws DfException {
		return this.base.fetchWithCaching(currencyCheckValue, usePersistentCache, useSharedCache);
	}

	@Override
	public int findAttrIndex(String attributeName) throws DfException {
		return this.base.findAttrIndex(attributeName);
	}

	@Override
	public int findBoolean(String attributeName, boolean value) throws DfException {
		return this.base.findBoolean(attributeName, value);
	}

	@Override
	public int findDouble(String attributeName, double value) throws DfException {
		return this.base.findDouble(attributeName, value);
	}

	@Override
	public int findId(String attributeName, IDfId value) throws DfException {
		return this.base.findId(attributeName, value);
	}

	@Override
	public int findInt(String attributeName, int value) throws DfException {
		return this.base.findInt(attributeName, value);
	}

	@Override
	public int findString(String attributeName, String value) throws DfException {
		return this.base.findString(attributeName, value);
	}

	@Override
	public int findTime(String attributeName, IDfTime value) throws DfException {
		return this.base.findTime(attributeName, value);
	}

	@Override
	public int findValue(String attributeName, IDfValue value) throws DfException {
		return this.base.findValue(attributeName, value);
	}

	@Override
	public void freeze(boolean freezeComponents) throws DfException {
		this.base.freeze(freezeComponents);
	}

	@Override
	public IDfACL getACL() throws DfException {
		return this.base.getACL();
	}

	@Override
	public String getACLDomain() throws DfException {
		return this.base.getACLDomain();
	}

	@Override
	public String getACLName() throws DfException {
		return this.base.getACLName();
	}

	@Override
	public IDfTime getAccessDate() throws DfException {
		return this.base.getAccessDate();
	}

	@Override
	public String getAccessorApplicationPermit(int index) throws DfException {
		return this.base.getAccessorApplicationPermit(index);
	}

	@Override
	public int getAccessorCount() throws DfException {
		return this.base.getAccessorCount();
	}

	@Override
	public String getAccessorName(int index) throws DfException {
		return this.base.getAccessorName(index);
	}

	@Override
	public String getAccessorPermit(int index) throws DfException {
		return this.base.getAccessorPermit(index);
	}

	@Override
	public int getAccessorPermitType(int index) throws DfException {
		return this.base.getAccessorPermitType(index);
	}

	@Override
	public int getAccessorXPermit(int index) throws DfException {
		return this.base.getAccessorXPermit(index);
	}

	@Override
	public String getAccessorXPermitNames(int index) throws DfException {
		return this.base.getAccessorXPermitNames(index);
	}

	@Override
	public boolean getAclRefValid() throws DfException {
		return this.base.getAclRefValid();
	}

	@Override
	public IDfEnumeration getAcsRequests(String formatName, int pageNumber, String pageModifier,
		IDfAcsTransferPreferences transferPreferences) throws DfException {
		return this.base.getAcsRequests(formatName, pageNumber, pageModifier, transferPreferences);
	}

	@Override
	public String getAliasSet() throws DfException {
		return this.base.getAliasSet();
	}

	@Override
	public IDfId getAliasSetId() throws DfException {
		return this.base.getAliasSetId();
	}

	@Override
	public String getAllRepeatingStrings(String attributeName, String separator) throws DfException {
		return this.base.getAllRepeatingStrings(attributeName, separator);
	}

	@Override
	public IDfId getAntecedentId() throws DfException {
		return this.base.getAntecedentId();
	}

	@Override
	public String getApplicationType() throws DfException {
		return this.base.getApplicationType();
	}

	@Override
	public IDfId getAssembledFromId() throws DfException {
		return this.base.getAssembledFromId();
	}

	@Override
	public IDfAttr getAttr(int attrIndex) throws DfException {
		return this.base.getAttr(attrIndex);
	}

	@SuppressWarnings("deprecation")
	@Override
	public IDfList getAttrAssistance(String attrName) throws DfException {
		return this.base.getAttrAssistance(attrName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public IDfList getAttrAssistanceWithValues(String attrName, IDfList depAttrNameList, IDfList depAttrValueListList)
		throws DfException {
		return this.base.getAttrAssistanceWithValues(attrName, depAttrNameList, depAttrValueListList);
	}

	@SuppressWarnings("deprecation")
	@Override
	public IDfList getAttrAsstDependencies(String attrName) throws DfException {
		return this.base.getAttrAsstDependencies(attrName);
	}

	@Override
	public int getAttrCount() throws DfException {
		return this.base.getAttrCount();
	}

	@Override
	public int getAttrDataType(String attributeName) throws DfException {
		return this.base.getAttrDataType(attributeName);
	}

	@Override
	public String getAuthors(int index) throws DfException {
		return this.base.getAuthors(index);
	}

	@Override
	public int getAuthorsCount() throws DfException {
		return this.base.getAuthorsCount();
	}

	@Override
	public boolean getBoolean(String attributeName) throws DfException {
		return this.base.getBoolean(attributeName);
	}

	@Override
	public int getBranchCount() throws DfException {
		return this.base.getBranchCount();
	}

	@Override
	public IDfId getCabinetId() throws DfException {
		return this.base.getCabinetId();
	}

	@Override
	public IDfCollection getChildRelatives(String relationTypeName) throws DfException {
		return this.base.getChildRelatives(relationTypeName);
	}

	@Override
	public IDfId getChronicleId() throws DfException {
		return this.base.getChronicleId();
	}

	@Override
	public IDfCollection getCollectionForContent(String format, int pageNumber) throws DfException {
		return this.base.getCollectionForContent(format, pageNumber);
	}

	@Override
	public IDfCollection getCollectionForContentEx2(String format, int pageNumber, String pageModifier)
		throws DfException {
		return this.base.getCollectionForContentEx2(format, pageNumber, pageModifier);
	}

	@Override
	public IDfCollection getCollectionForContentEx3(String format, int pageNumber, String pageModifier, boolean other)
		throws DfException {
		return this.base.getCollectionForContentEx3(format, pageNumber, pageModifier, other);
	}

	@Override
	public IDfContentCollection getCollectionForContentEx4(String format, int pageNumber, String pageModifier,
		boolean other) throws DfException {
		return this.base.getCollectionForContentEx4(format, pageNumber, pageModifier, other);
	}

	@Override
	public IDfId getComponentId(int index) throws DfException {
		return this.base.getComponentId(index);
	}

	@Override
	public int getComponentIdCount() throws DfException {
		return this.base.getComponentIdCount();
	}

	@Override
	public String getCompoundArchitecture() throws DfException {
		return this.base.getCompoundArchitecture();
	}

	@Override
	public IDfId getContainId(int index) throws DfException {
		return this.base.getContainId(index);
	}

	@Override
	public int getContainIdCount() throws DfException {
		return this.base.getContainIdCount();
	}

	@Override
	public ByteArrayInputStream getContent() throws DfException {
		return this.base.getContent();
	}

	@Override
	public IDfContentAvailability getContentAvailability(String formatName, int pageNumber, String pageModifier,
		String networkLocationIdentifier) throws DfException {
		return this.base.getContentAvailability(formatName, pageNumber, pageModifier, networkLocationIdentifier);
	}

	@Override
	public ByteArrayInputStream getContentEx(String format, int pageNumber) throws DfException {
		return this.base.getContentEx(format, pageNumber);
	}

	@Override
	public ByteArrayInputStream getContentEx2(String format, int pageNumber, String pageModifier) throws DfException {
		return this.base.getContentEx2(format, pageNumber, pageModifier);
	}

	@Override
	public ByteArrayInputStream getContentEx3(String format, int pageNumber, String pageModifier, boolean other)
		throws DfException {
		return this.base.getContentEx3(format, pageNumber, pageModifier, other);
	}

	@Override
	public long getContentSize() throws DfException {
		return this.base.getContentSize();
	}

	@Override
	public long getContentSize(int page, String formatName, String pageModifier) throws DfException {
		return this.base.getContentSize(page, formatName, pageModifier);
	}

	@Override
	public int getContentState(int index) throws DfException {
		return this.base.getContentState(index);
	}

	@Override
	public int getContentStateCount() throws DfException {
		return this.base.getContentStateCount();
	}

	@Override
	public String getContentType() throws DfException {
		return this.base.getContentType();
	}

	@Override
	public IDfId getContentsId() throws DfException {
		return this.base.getContentsId();
	}

	@Override
	public IDfTime getCreationDate() throws DfException {
		return this.base.getCreationDate();
	}

	@Override
	public String getCreatorName() throws DfException {
		return this.base.getCreatorName();
	}

	@Override
	public int getCurrentState() throws DfException {
		return this.base.getCurrentState();
	}

	@Override
	public String getCurrentStateName() throws DfException {
		return this.base.getCurrentStateName();
	}

	@Override
	public String getDirectDescendant() throws DfException {
		return this.base.getDirectDescendant();
	}

	@Override
	public double getDouble(String attributeName) throws DfException {
		return this.base.getDouble(attributeName);
	}

	@Override
	public Double getDoubleContentAttr(String name, String formatName, int page, String pageModifier)
		throws DfException {
		return this.base.getDoubleContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public String getExceptionStateName() throws DfException {
		return this.base.getExceptionStateName();
	}

	@Override
	public String getFile(String fileName) throws DfException {
		return this.base.getFile(fileName);
	}

	@Override
	public String getFileEx(String fileName, String formatName, int pageNumber, boolean other) throws DfException {
		return this.base.getFileEx(fileName, formatName, pageNumber, other);
	}

	@Override
	public String getFileEx2(String fileName, String formatName, int pageNumber, String pageModifier, boolean other)
		throws DfException {
		return this.base.getFileEx2(fileName, formatName, pageNumber, pageModifier, other);
	}

	@Override
	public IDfId getFolderId(int index) throws DfException {
		return this.base.getFolderId(index);
	}

	@Override
	public int getFolderIdCount() throws DfException {
		return this.base.getFolderIdCount();
	}

	@Override
	public IDfFormat getFormat() throws DfException {
		return this.base.getFormat();
	}

	@Override
	public int getFrozenAssemblyCount() throws DfException {
		return this.base.getFrozenAssemblyCount();
	}

	@Override
	public boolean getFullText() throws DfException {
		return this.base.getFullText();
	}

	@Override
	public String getGroupName() throws DfException {
		return this.base.getGroupName();
	}

	@Override
	public int getGroupPermit() throws DfException {
		return this.base.getGroupPermit();
	}

	@Override
	public boolean getHasEvents() throws DfException {
		return this.base.getHasEvents();
	}

	@Override
	public boolean getHasFolder() throws DfException {
		return this.base.getHasFolder();
	}

	@Override
	public boolean getHasFrozenAssembly() throws DfException {
		return this.base.getHasFrozenAssembly();
	}

	@Override
	public IDfId getId(String attributeName) throws DfException {
		return this.base.getId(attributeName);
	}

	@Override
	public String getImplicitVersionLabel() throws DfException {
		return this.base.getImplicitVersionLabel();
	}

	@Override
	public int getInt(String attributeName) throws DfException {
		return this.base.getInt(attributeName);
	}

	@Override
	public String getKeywords(int index) throws DfException {
		return this.base.getKeywords(index);
	}

	@Override
	public int getKeywordsCount() throws DfException {
		return this.base.getKeywordsCount();
	}

	@Override
	public boolean getLatestFlag() throws DfException {
		return this.base.getLatestFlag();
	}

	@Override
	public int getLinkCount() throws DfException {
		return this.base.getLinkCount();
	}

	@Override
	public int getLinkHighCount() throws DfException {
		return this.base.getLinkHighCount();
	}

	@Override
	public IDfCollection getLocations(String attrNames) throws DfException {
		return this.base.getLocations(attrNames);
	}

	@Override
	public IDfTime getLockDate() throws DfException {
		return this.base.getLockDate();
	}

	@Override
	public String getLockMachine() throws DfException {
		return this.base.getLockMachine();
	}

	@Override
	public String getLockOwner() throws DfException {
		return this.base.getLockOwner();
	}

	@Override
	public String getLogEntry() throws DfException {
		return this.base.getLogEntry();
	}

	@Override
	public long getLong(String attributeName) throws DfException {
		return this.base.getLong(attributeName);
	}

	@Override
	public String getMasterDocbase() throws DfException {
		return this.base.getMasterDocbase();
	}

	@Override
	public String getModifier() throws DfException {
		return this.base.getModifier();
	}

	@Override
	public IDfTime getModifyDate() throws DfException {
		return this.base.getModifyDate();
	}

	@Override
	public String getNextStateName() throws DfException {
		return this.base.getNextStateName();
	}

	@Override
	public IDfId getObjectId() throws DfException {
		return this.base.getObjectId();
	}

	@Override
	public String getObjectName() throws DfException {
		return this.base.getObjectName();
	}

	@Override
	public IDfSession getObjectSession() {
		return this.base.getObjectSession();
	}

	@Override
	public IDfSession getOriginalSession() {
		return this.base.getOriginalSession();
	}

	@Override
	public long getOtherFileSize(int page, String formatName, String pageModifier) throws DfException {
		return this.base.getOtherFileSize(page, formatName, pageModifier);
	}

	@Override
	public String getOwnerName() throws DfException {
		return this.base.getOwnerName();
	}

	@Override
	public int getOwnerPermit() throws DfException {
		return this.base.getOwnerPermit();
	}

	@Override
	public int getPageCount() throws DfException {
		return this.base.getPageCount();
	}

	@Override
	public IDfCollection getParentRelatives(String relationTypeName) throws DfException {
		return this.base.getParentRelatives(relationTypeName);
	}

	@Override
	public int getPartition() throws DfException {
		return this.base.getPartition();
	}

	@Override
	public String getPath(int pageNumber) throws DfException {
		return this.base.getPath(pageNumber);
	}

	@Override
	public String getPathEx(int pageNumber, String pageModifier) throws DfException {
		return this.base.getPathEx(pageNumber, pageModifier);
	}

	@Override
	public String getPathEx2(String formatName, int pageNumber, String pageModifier, boolean other) throws DfException {
		return this.base.getPathEx2(formatName, pageNumber, pageModifier, other);
	}

	@Override
	public IDfList getPermissions() throws DfException {
		return this.base.getPermissions();
	}

	@Override
	public int getPermit() throws DfException {
		return this.base.getPermit();
	}

	@Override
	public int getPermitEx(String accessorName) throws DfException {
		return this.base.getPermitEx(accessorName);
	}

	@Override
	public IDfId getPolicyId() throws DfException {
		return this.base.getPolicyId();
	}

	@Override
	public String getPolicyName() throws DfException {
		return this.base.getPolicyName();
	}

	@Override
	public String getPreviousStateName() throws DfException {
		return this.base.getPreviousStateName();
	}

	@Override
	public int getReferenceCount() throws DfException {
		return this.base.getReferenceCount();
	}

	@Override
	public IDfId getRemoteId() throws DfException {
		return this.base.getRemoteId();
	}

	@Override
	public IDfCollection getRenditions(String attrNames) throws DfException {
		return this.base.getRenditions(attrNames);
	}

	@Override
	public boolean getRepeatingBoolean(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingBoolean(attributeName, valueIndex);
	}

	@Override
	public double getRepeatingDouble(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingDouble(attributeName, valueIndex);
	}

	@Override
	public IDfId getRepeatingId(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingId(attributeName, valueIndex);
	}

	@Override
	public int getRepeatingInt(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingInt(attributeName, valueIndex);
	}

	@Override
	public long getRepeatingLong(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingLong(attributeName, valueIndex);
	}

	@Override
	public String getRepeatingString(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingString(attributeName, valueIndex);
	}

	@Override
	public IDfTime getRepeatingTime(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingTime(attributeName, valueIndex);
	}

	@Override
	public IDfValue getRepeatingValue(String attributeName, int valueIndex) throws DfException {
		return this.base.getRepeatingValue(attributeName, valueIndex);
	}

	@Override
	public String getResolutionLabel() throws DfException {
		return this.base.getResolutionLabel();
	}

	@Override
	public int getResumeState() throws DfException {
		return this.base.getResumeState();
	}

	@Override
	public String getResumeStateName() throws DfException {
		return this.base.getResumeStateName();
	}

	@Override
	public IDfTime getRetainUntilDate() throws DfException {
		return this.base.getRetainUntilDate();
	}

	@Override
	public int getRetainerCount() throws DfException {
		return this.base.getRetainerCount();
	}

	@Override
	public IDfId getRetainerId(int index) throws DfException {
		return this.base.getRetainerId(index);
	}

	@Override
	public IDfTime getRetentionDate() throws DfException {
		return this.base.getRetentionDate();
	}

	@Override
	public IDfCollection getRouters(String additionalAttributes, String orderBy) throws DfException {
		return this.base.getRouters(additionalAttributes, orderBy);
	}

	@Override
	public IDfSession getSession() {
		return this.base.getSession();
	}

	@Override
	public IDfSessionManager getSessionManager() {
		return this.base.getSessionManager();
	}

	@Override
	public String getSpecialApp() throws DfException {
		return this.base.getSpecialApp();
	}

	@Override
	public String getStatus() throws DfException {
		return this.base.getStatus();
	}

	@Override
	public String getStorageType() throws DfException {
		return this.base.getStorageType();
	}

	@Override
	public String getString(String attributeName) throws DfException {
		return this.base.getString(attributeName);
	}

	@Override
	public String getStringContentAttr(String name, String formatName, int page, String pageModifier)
		throws DfException {
		return this.base.getStringContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public String getSubject() throws DfException {
		return this.base.getSubject();
	}

	@Override
	public IDfTime getTime(String attributeName) throws DfException {
		return this.base.getTime(attributeName);
	}

	@Override
	public IDfTime getTimeContentAttr(String name, String formatName, int page, String pageModifier) throws DfException {
		return this.base.getTimeContentAttr(name, formatName, page, pageModifier);
	}

	@Override
	public String getTitle() throws DfException {
		return this.base.getTitle();
	}

	@Override
	public IDfType getType() throws DfException {
		return this.base.getType();
	}

	@Override
	public String getTypeName() throws DfException {
		return this.base.getTypeName();
	}

	@Override
	public int getVStamp() throws DfException {
		return this.base.getVStamp();
	}

	@Override
	public IDfValidator getValidator() throws DfException {
		return this.base.getValidator();
	}

	@Override
	public IDfValue getValue(String attributeName) throws DfException {
		return this.base.getValue(attributeName);
	}

	@Override
	public IDfValue getValueAt(int index) throws DfException {
		return this.base.getValueAt(index);
	}

	@Override
	public int getValueCount(String attributeName) throws DfException {
		return this.base.getValueCount(attributeName);
	}

	@Override
	public IDfCollection getVdmPath(IDfId rootId, boolean shortestPath, String versionList) throws DfException {
		return this.base.getVdmPath(rootId, shortestPath, versionList);
	}

	@Override
	public IDfCollection getVdmPathDQL(IDfId rootId, boolean shortestPath, String parentType, String bindingLabel,
		String nodeSortBy) throws DfException {
		return this.base.getVdmPathDQL(rootId, shortestPath, parentType, bindingLabel, nodeSortBy);
	}

	@Override
	public String getVersionLabel(int index) throws DfException {
		return this.base.getVersionLabel(index);
	}

	@Override
	public int getVersionLabelCount() throws DfException {
		return this.base.getVersionLabelCount();
	}

	@Override
	public IDfVersionLabels getVersionLabels() throws DfException {
		return this.base.getVersionLabels();
	}

	@Override
	public IDfVersionPolicy getVersionPolicy() throws DfException {
		return this.base.getVersionPolicy();
	}

	@Override
	public IDfCollection getVersions(String attrNames) throws DfException {
		return this.base.getVersions(attrNames);
	}

	@SuppressWarnings("deprecation")
	@Override
	public String getWidgetType(int environment, String attrName) throws DfException {
		return this.base.getWidgetType(environment, attrName);
	}

	@Override
	public IDfCollection getWorkflows(String additionalAttributes, String orderBy) throws DfException {
		return this.base.getWorkflows(additionalAttributes, orderBy);
	}

	@Override
	public int getWorldPermit() throws DfException {
		return this.base.getWorldPermit();
	}

	@Override
	public int getXPermit(String accessorName) throws DfException {
		return this.base.getXPermit(accessorName);
	}

	@Override
	public String getXPermitList() throws DfException {
		return this.base.getXPermitList();
	}

	@Override
	public String getXPermitNames(String accessorName) throws DfException {
		return this.base.getXPermitNames(accessorName);
	}

	@Override
	public void grant(String accessorName, int accessorPermit, String extendedPermission) throws DfException {
		this.base.grant(accessorName, accessorPermit, extendedPermission);
	}

	@Override
	public void grantPermit(IDfPermit permit) throws DfException {
		this.base.grantPermit(permit);
	}

	@Override
	public boolean hasAttr(String attributeName) throws DfException {
		return this.base.hasAttr(attributeName);
	}

	@Override
	public boolean hasPermission(String permission, String accessorName) throws DfException {
		return this.base.hasPermission(permission, accessorName);
	}

	@Override
	public void insertBoolean(String attributeName, int valueIndex, boolean value) throws DfException {
		this.base.insertBoolean(attributeName, valueIndex, value);
	}

	@Override
	public void insertContent(ByteArrayOutputStream content, int pageNumber) throws DfException {
		this.base.insertContent(content, pageNumber);
	}

	@Override
	public void insertContentEx(ByteArrayOutputStream content, int pageNumber, boolean other) throws DfException {
		this.base.insertContentEx(content, pageNumber, other);
	}

	@Override
	public void insertDouble(String attributeName, int valueIndex, double value) throws DfException {
		this.base.insertDouble(attributeName, valueIndex, value);
	}

	@Override
	public void insertFile(String fileName, int pageNumber) throws DfException {
		this.base.insertFile(fileName, pageNumber);
	}

	@Override
	public void insertFileEx(String fileName, int pageNumber, String otherFileName) throws DfException {
		this.base.insertFileEx(fileName, pageNumber, otherFileName);
	}

	@Override
	public void insertId(String attributeName, int valueIndex, IDfId value) throws DfException {
		this.base.insertId(attributeName, valueIndex, value);
	}

	@Override
	public void insertInt(String attributeName, int valueIndex, int value) throws DfException {
		this.base.insertInt(attributeName, valueIndex, value);
	}

	@Override
	public IDfId insertPart(IDfId componentID, String versionLabel, IDfId beforeContainmentId, double orderNo,
		boolean orderNoFlag, boolean useNodeVerLabel, boolean followAssembly, int copyChild) throws DfException {
		return this.base.insertPart(componentID, versionLabel, beforeContainmentId, orderNo, orderNoFlag,
			useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public void insertString(String attributeName, int valueIndex, String value) throws DfException {
		this.base.insertString(attributeName, valueIndex, value);
	}

	@Override
	public void insertTime(String attributeName, int valueIndex, IDfTime value) throws DfException {
		this.base.insertTime(attributeName, valueIndex, value);
	}

	@Override
	public void insertValue(String attributeName, int valueIndex, IDfValue value) throws DfException {
		this.base.insertValue(attributeName, valueIndex, value);
	}

	@Override
	public boolean isArchived() throws DfException {
		return this.base.isArchived();
	}

	@Override
	public boolean isAttrRepeating(String attributeName) throws DfException {
		return this.base.isAttrRepeating(attributeName);
	}

	@Override
	public boolean isCheckedOut() throws DfException {
		return this.base.isCheckedOut();
	}

	@Override
	public boolean isCheckedOutBy(String userName) throws DfException {
		return this.base.isCheckedOutBy(userName);
	}

	@Override
	public boolean isContentTransferCapabilityEnabled(String networkLocationIdentifier,
		IDfContentTransferCapability capability) throws DfException {
		return this.base.isContentTransferCapabilityEnabled(networkLocationIdentifier, capability);
	}

	@Override
	public boolean isDeleted() throws DfException {
		return this.base.isDeleted();
	}

	@Override
	public boolean isDirty() throws DfException {
		return this.base.isDirty();
	}

	@Override
	public boolean isFrozen() throws DfException {
		return this.base.isFrozen();
	}

	@Override
	public boolean isHidden() throws DfException {
		return this.base.isHidden();
	}

	@Override
	public boolean isImmutable() throws DfException {
		return this.base.isImmutable();
	}

	@Override
	public boolean isInstanceOf(String typeName) throws DfException {
		return this.base.isInstanceOf(typeName);
	}

	@Override
	public boolean isLinkResolved() throws DfException {
		return this.base.isLinkResolved();
	}

	@Override
	public boolean isNew() throws DfException {
		return this.base.isNew();
	}

	@Override
	public boolean isNull(String attributeName) throws DfException {
		return this.base.isNull(attributeName);
	}

	@Override
	public boolean isPublic() throws DfException {
		return this.base.isPublic();
	}

	@Override
	public boolean isReference() throws DfException {
		return this.base.isReference();
	}

	@Override
	public boolean isReplica() throws DfException {
		return this.base.isReplica();
	}

	@Override
	public boolean isSuspended() throws DfException {
		return this.base.isSuspended();
	}

	@Override
	public boolean isVirtualDocument() throws DfException {
		return this.base.isVirtualDocument();
	}

	@Override
	public void link(String folderSpec) throws DfException {
		this.base.link(folderSpec);
	}

	@Override
	public void lock() throws DfException {
		this.base.lock();
	}

	@Override
	public void lockEx(boolean validateStamp) throws DfException {
		this.base.lockEx(validateStamp);
	}

	@Override
	public void mark(String versionLabels) throws DfException {
		this.base.mark(versionLabels);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void mount(String path) throws DfException {
		this.base.mount(path);
	}

	@Override
	public String print(String printer, boolean printCover, boolean saveOutput, int numCopies, int startingContentPage,
		int endingContentPage) throws DfException {
		return this.base.print(printer, printCover, saveOutput, numCopies, startingContentPage, endingContentPage);
	}

	@Override
	public void promote(String state, boolean override, boolean fTestOnly) throws DfException {
		this.base.promote(state, override, fTestOnly);
	}

	@Override
	public void prune(boolean keepLabels) throws DfException {
		this.base.prune(keepLabels);
	}

	@Override
	public IDfId queue(String queueOwner, String event, int priority, boolean sendMail, IDfTime dueDate, String message)
		throws DfException {
		return this.base.queue(queueOwner, event, priority, sendMail, dueDate, message);
	}

	@Override
	public void refreshReference() throws DfException {
		this.base.refreshReference();
	}

	@Override
	public void registerEvent(String message, String event, int priority, boolean sendMail) throws DfException {
		this.base.registerEvent(message, event, priority, sendMail);
	}

	@Override
	public void remove(String attributeName, int valueIndex) throws DfException {
		this.base.remove(attributeName, valueIndex);
	}

	@Override
	public void removeAll(String attributeName) throws DfException {
		this.base.removeAll(attributeName);
	}

	@Override
	public void removeChildRelative(String relationTypeName, IDfId childId, String childLabel) throws DfException {
		this.base.removeChildRelative(relationTypeName, childId, childLabel);
	}

	@Override
	public void removeContent(int pageNumber) throws DfException {
		this.base.removeContent(pageNumber);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void removeNote(IDfId annotationId) throws DfException {
		this.base.removeNote(annotationId);
	}

	@Override
	public void removeParentRelative(String relationTypeName, IDfId parentId, String childLabel) throws DfException {
		this.base.removeParentRelative(relationTypeName, parentId, childLabel);
	}

	@Override
	public void removePart(IDfId containmentId, double orderNo, boolean orderNoFlag) throws DfException {
		this.base.removePart(containmentId, orderNo, orderNoFlag);
	}

	@Override
	public void removeRendition(String formatName) throws DfException {
		this.base.removeRendition(formatName);
	}

	@Override
	public void removeRenditionEx(String formatName, int pageNumber, boolean atomic) throws DfException {
		this.base.removeRenditionEx(formatName, pageNumber, atomic);
	}

	@Override
	public void removeRenditionEx2(String formatName, int pageNumber, String pageModifier, boolean atomic)
		throws DfException {
		this.base.removeRenditionEx2(formatName, pageNumber, pageModifier, atomic);
	}

	@Override
	public String resolveAlias(String scopeAlias) throws DfException {
		return this.base.resolveAlias(scopeAlias);
	}

	@Override
	public void resume(String state, boolean toBase, boolean override, boolean fTestOnly) throws DfException {
		this.base.resume(state, toBase, override, fTestOnly);
	}

	@Override
	public void revert() throws DfException {
		this.base.revert();
	}

	@Override
	public void revertACL() throws DfException {
		this.base.revertACL();
	}

	@Override
	public void revoke(String accessorName, String extendedPermission) throws DfException {
		this.base.revoke(accessorName, extendedPermission);
	}

	@Override
	public void revokePermit(IDfPermit permit) throws DfException {
		this.base.revokePermit(permit);
	}

	@Override
	public void save() throws DfException {
		this.base.save();
	}

	@Override
	public IDfId saveAsNew(boolean shareContent) throws DfException {
		return this.base.saveAsNew(shareContent);
	}

	@Override
	public void saveLock() throws DfException {
		this.base.saveLock();
	}

	@Override
	public void scheduleDemote(String state, IDfTime schedule_date) throws DfException {
		this.base.scheduleDemote(state, schedule_date);
	}

	@Override
	public void schedulePromote(String state, IDfTime scheduleDate, boolean override) throws DfException {
		this.base.schedulePromote(state, scheduleDate, override);
	}

	@Override
	public void scheduleResume(String state, IDfTime scheduleDate, boolean toBase, boolean override) throws DfException {
		this.base.scheduleResume(state, scheduleDate, toBase, override);
	}

	@Override
	public void scheduleSuspend(String state, IDfTime scheduleDate, boolean override) throws DfException {
		this.base.scheduleSuspend(state, scheduleDate, override);
	}

	@Override
	public void setACL(IDfACL acl) throws DfException {
		this.base.setACL(acl);
	}

	@Override
	public void setACLDomain(String aclDomain) throws DfException {
		this.base.setACLDomain(aclDomain);
	}

	@Override
	public void setACLName(String ACLName) throws DfException {
		this.base.setACLName(ACLName);
	}

	@Override
	public void setApplicationType(String type) throws DfException {
		this.base.setApplicationType(type);
	}

	@Override
	public void setArchived(boolean archived) throws DfException {
		this.base.setArchived(archived);
	}

	@Override
	public void setAuthors(int index, String author) throws DfException {
		this.base.setAuthors(index, author);
	}

	@Override
	public void setBoolean(String attributeName, boolean value) throws DfException {
		this.base.setBoolean(attributeName, value);
	}

	@Override
	public void setCompoundArchitecture(String compoundArchitecture) throws DfException {
		this.base.setCompoundArchitecture(compoundArchitecture);
	}

	@Override
	public boolean setContent(ByteArrayOutputStream content) throws DfException {
		return this.base.setContent(content);
	}

	@Override
	public boolean setContentEx(ByteArrayOutputStream content, String format, int pageNumber) throws DfException {
		return this.base.setContentEx(content, format, pageNumber);
	}

	@Override
	public boolean setContentEx2(ByteArrayOutputStream content, String format, int pageNumber, boolean other)
		throws DfException {
		return this.base.setContentEx2(content, format, pageNumber, other);
	}

	@Override
	public void setContentType(String contentType) throws DfException {
		this.base.setContentType(contentType);
	}

	@Override
	public void setDouble(String attributeName, double value) throws DfException {
		this.base.setDouble(attributeName, value);
	}

	@Override
	public void setDoubleContentAttribute(String name, double value, String formatName, int page, String pageModifier)
		throws DfException {
		this.base.setDoubleContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public void setFile(String fileName) throws DfException {
		this.base.setFile(fileName);
	}

	@Override
	public void setFileEx(String fileName, String formatName, int pageNumber, String otherFile) throws DfException {
		this.base.setFileEx(fileName, formatName, pageNumber, otherFile);
	}

	@Override
	public void setFullText(boolean fullText) throws DfException {
		this.base.setFullText(fullText);
	}

	@Override
	public void setGroupName(String name) throws DfException {
		this.base.setGroupName(name);
	}

	@Override
	public void setGroupPermit(int permit) throws DfException {
		this.base.setGroupPermit(permit);
	}

	@Override
	public void setHidden(boolean isHidden) throws DfException {
		this.base.setHidden(isHidden);
	}

	@Override
	public void setId(String attributeName, IDfId value) throws DfException {
		this.base.setId(attributeName, value);
	}

	@Override
	public void setInt(String attributeName, int value) throws DfException {
		this.base.setInt(attributeName, value);
	}

	@Override
	public void setIsVirtualDocument(boolean is_virtual_doc) throws DfException {
		this.base.setIsVirtualDocument(is_virtual_doc);
	}

	@Override
	public void setKeywords(int index, String keyword) throws DfException {
		this.base.setKeywords(index, keyword);
	}

	@Override
	public void setLinkResolved(boolean linkResolved) throws DfException {
		this.base.setLinkResolved(linkResolved);
	}

	@Override
	public void setLogEntry(String logEntry) throws DfException {
		this.base.setLogEntry(logEntry);
	}

	@Override
	public void setNull(String attributeName) throws DfException {
		this.base.setNull(attributeName);
	}

	@Override
	public void setObjectName(String objectName) throws DfException {
		this.base.setObjectName(objectName);
	}

	@Override
	public void setOwnerName(String ownerName) throws DfException {
		this.base.setOwnerName(ownerName);
	}

	@Override
	public void setOwnerPermit(int permit) throws DfException {
		this.base.setOwnerPermit(permit);
	}

	@Override
	public void setPartition(int partition) throws DfException {
		this.base.setPartition(partition);
	}

	@Override
	public void setPath(String fileName, String formatName, int pageNumber, String otherFile) throws DfException {
		this.base.setPath(fileName, formatName, pageNumber, otherFile);
	}

	@Override
	public void setRepeatingBoolean(String attributeName, int valueIndex, boolean value) throws DfException {
		this.base.setRepeatingBoolean(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingDouble(String attributeName, int valueIndex, double value) throws DfException {
		this.base.setRepeatingDouble(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingId(String attributeName, int valueIndex, IDfId value) throws DfException {
		this.base.setRepeatingId(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingInt(String attributeName, int valueIndex, int value) throws DfException {
		this.base.setRepeatingInt(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingString(String attributeName, int valueIndex, String value) throws DfException {
		this.base.setRepeatingString(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingTime(String attributeName, int valueIndex, IDfTime value) throws DfException {
		this.base.setRepeatingTime(attributeName, valueIndex, value);
	}

	@Override
	public void setRepeatingValue(String attributeName, int valueIndex, IDfValue value) throws DfException {
		this.base.setRepeatingValue(attributeName, valueIndex, value);
	}

	@Override
	public void setResolutionLabel(String label) throws DfException {
		this.base.setResolutionLabel(label);
	}

	@Override
	public void setSessionManager(IDfSessionManager sessMgr) throws DfException {
		this.base.setSessionManager(sessMgr);
	}

	@Override
	public void setSpecialApp(String specialApp) throws DfException {
		this.base.setSpecialApp(specialApp);
	}

	@Override
	public void setStatus(String status) throws DfException {
		this.base.setStatus(status);
	}

	@Override
	public void setStorageType(String type) throws DfException {
		this.base.setStorageType(type);
	}

	@Override
	public void setString(String attributeName, String value) throws DfException {
		this.base.setString(attributeName, value);
	}

	@Override
	public void setStringContentAttribute(String name, String value, String formatName, int page, String pageModifier)
		throws DfException {
		this.base.setStringContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public void setSubject(String subject) throws DfException {
		this.base.setSubject(subject);
	}

	@Override
	public void setTime(String attributeName, IDfTime value) throws DfException {
		this.base.setTime(attributeName, value);
	}

	@Override
	public void setTimeContentAttribute(String name, IDfTime value, String formatName, int page, String pageModifier)
		throws DfException {
		this.base.setTimeContentAttribute(name, value, formatName, page, pageModifier);
	}

	@Override
	public void setTitle(String title) throws DfException {
		this.base.setTitle(title);
	}

	@Override
	public void setValue(String attributeName, IDfValue value) throws DfException {
		this.base.setValue(attributeName, value);
	}

	@Override
	public void setWorldPermit(int permit) throws DfException {
		this.base.setWorldPermit(permit);
	}

	@Override
	public void signoff(String user, String os_password, String reason) throws DfException {
		this.base.signoff(user, os_password, reason);
	}

	@Override
	public void suspend(String state, boolean override, boolean fTestOnly) throws DfException {
		this.base.suspend(state, override, fTestOnly);
	}

	@Override
	public void truncate(String attributeName, int valueIndex) throws DfException {
		this.base.truncate(attributeName, valueIndex);
	}

	@Override
	public void unRegisterEvent(String event) throws DfException {
		this.base.unRegisterEvent(event);
	}

	@Override
	public void unRegisterEventEx(String event, String userName) throws DfException {
		this.base.unRegisterEventEx(event, userName);
	}

	@Override
	public void unfreeze(boolean thawComponents) throws DfException {
		this.base.unfreeze(thawComponents);
	}

	@Override
	public void unlink(String folderSpec) throws DfException {
		this.base.unlink(folderSpec);
	}

	@Override
	public void unmark(String versionLabels) throws DfException {
		this.base.unmark(versionLabels);
	}

	@Override
	public void updatePart(IDfId containmentId, String versionLabel, double orderNumber, boolean useNodeVerLabel,
		boolean followAssembly, int copyChild) throws DfException {
		this.base.updatePart(containmentId, versionLabel, orderNumber, useNodeVerLabel, followAssembly, copyChild);
	}

	@Override
	public void updatePartEx(IDfId containmentId, String versionLabel, double orderNumber, boolean useNodeVerLabel,
		boolean followAssembly, int copyChild, String containType, String containDesc) throws DfException {
		this.base.updatePartEx(containmentId, versionLabel, orderNumber, useNodeVerLabel, followAssembly, copyChild,
			containType, containDesc);
	}

	@Override
	public void useACL(String aclType) throws DfException {
		this.base.useACL(aclType);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateAllRules(int stopAfterNumOfErrors) throws DfException {
		this.base.validateAllRules(stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateAttrRules(String attrName, int stopAfterNumOfErrors) throws DfException {
		this.base.validateAttrRules(attrName, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateAttrRulesWithValue(String attrName, String value, int stopAfterNumOfErrors) throws DfException {
		this.base.validateAttrRulesWithValue(attrName, value, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateAttrRulesWithValues(String attrName, IDfList valueList, int stopAfterNumOfErrors)
		throws DfException {
		this.base.validateAttrRulesWithValues(attrName, valueList, stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateObjRules(int stopAfterNumOfErrors) throws DfException {
		this.base.validateObjRules(stopAfterNumOfErrors);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateObjRulesWithValues(IDfList attrNameList, IDfList valueListList, int stopAfterNumOfErrors)
		throws DfException {
		this.base.validateObjRulesWithValues(attrNameList, valueListList, stopAfterNumOfErrors);
	}

	@Override
	public void verifyESignature() throws DfException {
		this.base.verifyESignature();
	}
}
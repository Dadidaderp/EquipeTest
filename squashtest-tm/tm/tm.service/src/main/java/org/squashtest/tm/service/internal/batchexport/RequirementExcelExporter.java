/**
 *     This file is part of the Squashtest platform.
 *     Copyright (C) 2010 - 2016 Henix, henix.fr
 *
 *     See the NOTICE file distributed with this work for additional
 *     information regarding copyright ownership.
 *
 *     This is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     this software is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.squashtest.tm.service.internal.batchexport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.squashtest.tm.core.foundation.lang.DateUtils;
import org.squashtest.tm.service.feature.FeatureManager;
import org.squashtest.tm.service.feature.FeatureManager.Feature;
import org.squashtest.tm.service.internal.batchexport.ExportModel.CoverageModel;
import org.squashtest.tm.service.internal.batchexport.ExportModel.CustomField;
import org.squashtest.tm.service.internal.batchexport.RequirementExportModel.RequirementModel;
import org.squashtest.tm.service.internal.batchimport.requirement.excel.RequirementSheetColumn;
import org.squashtest.tm.service.internal.batchimport.testcase.excel.CoverageSheetColumn;
import org.squashtest.tm.service.internal.batchimport.testcase.excel.TemplateColumn;
import org.squashtest.tm.service.internal.batchimport.testcase.excel.TemplateWorksheet;

/**
 * @author jthebault
 */
@Component
@Scope("prototype")
public class RequirementExcelExporter {
	private static final String REQUIREMENT_SHEET = TemplateWorksheet.REQUIREMENT_SHEET.sheetName;
	private static final String COV_SHEET = TemplateWorksheet.COVERAGE_SHEET.sheetName;

	private static final List<CoverageSheetColumn> COVERAGE_COLUMNS = Arrays.asList(
		CoverageSheetColumn.REQ_PATH,
		CoverageSheetColumn.REQ_VERSION_NUM,
		CoverageSheetColumn.TC_PATH);

	private static final RequirementSheetColumn[] BASIC_REQ_COLUMNS = {
		RequirementSheetColumn.PROJECT_ID,
		RequirementSheetColumn.PROJECT_NAME,
		RequirementSheetColumn.REQ_PATH,
		RequirementSheetColumn.REQ_NUM,
		RequirementSheetColumn.REQ_VERSION_NUM,
		RequirementSheetColumn.REQ_VERSION_REFERENCE,
		RequirementSheetColumn.REQ_VERSION_NAME,
		RequirementSheetColumn.REQ_VERSION_CRITICALITY,
		RequirementSheetColumn.REQ_VERSION_CATEGORY,
		RequirementSheetColumn.REQ_VERSION_STATUS,
		RequirementSheetColumn.REQ_VERSION_DESCRIPTION,
		RequirementSheetColumn.REQ_VERSION_NB_TC,
		RequirementSheetColumn.REQ_VERSION_NB_ATTACHEMENT,
		RequirementSheetColumn.REQ_VERSION_CREATED_ON,
		RequirementSheetColumn.REQ_VERSION_CREATED_BY,
		RequirementSheetColumn.REQ_VERSION_LAST_MODIFIED_ON,
		RequirementSheetColumn.REQ_VERSION_LAST_MODIFIED_BY};

	private static final List<RequirementSheetColumn> REQUIREMENT_COLUMNS_MILESTONES = Arrays.asList(ArrayUtils.add(BASIC_REQ_COLUMNS, RequirementSheetColumn.REQ_VERSION_MILESTONE));

	private static final List<RequirementSheetColumn> REQUIREMENT_COLUMNS = Arrays.asList(BASIC_REQ_COLUMNS);
	// that map will remember which column index is
	private Map<String, Integer> cufColumnsByCode = new HashMap<>();

	private Workbook workbook;

	protected boolean milestonesEnabled;

	private MessageSource messageSource;

	private String errorCellTooLargeMessage;

	@Inject
	public RequirementExcelExporter(FeatureManager featureManager, MessageSource messageSource) {
		super();
		milestonesEnabled = featureManager.isEnabled(Feature.MILESTONE);

		createWorkbook();
		createHeaders();

	}

	void setMessageSource(MessageSource messageSource) {

		this.messageSource = messageSource;
		errorCellTooLargeMessage = this.messageSource.getMessage("test-case.export.errors.celltoolarge", null, LocaleContextHolder.getLocale());

	}

	public void appendToWorkbook(RequirementExportModel model, boolean keepRteFormat) {
		if (!keepRteFormat) {
			removeRteFormat(model);
		}
		sort(model);
		appendRequirementModel(model);
		appendCoverage(model);
	}

	private void sort(RequirementExportModel model) {
		Collections.sort(model.getCoverages(), CoverageModel.REQ_COMPARATOR);
		Collections.sort(model.getRequirementsModels(), RequirementModel.COMPARATOR);
	}

	private void appendCoverage(RequirementExportModel model) {
		List<CoverageModel> models = model.getCoverages();
		Sheet covSheet = workbook.getSheet(COV_SHEET);

		Row r;
		int rIdx = covSheet.getLastRowNum() + 1;
		int cIdx = 0;

		for (CoverageModel cm : models) {
			r = covSheet.createRow(rIdx);


			r.createCell(cIdx++).setCellValue(cm.getReqPath());
			r.createCell(cIdx++).setCellValue(cm.getRequirementVersionNumber());
			r.createCell(cIdx++).setCellValue(cm.getTcPath());

			rIdx++;
			cIdx = 0;
		}

	}

	private void appendRequirementModel(RequirementExportModel model) {
		List<RequirementModel> models = model.getRequirementsModels();
		Sheet reqSheet = workbook.getSheet(REQUIREMENT_SHEET);
		int rowIndex = reqSheet.getLastRowNum() + 1;

		for (RequirementModel reqModel : models) {
			appendOneRequirement(reqSheet, rowIndex, reqModel);
			rowIndex++;
		}

	}


	private void createCoverageHeaders() {
		createSheetHeaders(COV_SHEET, COVERAGE_COLUMNS);
	}

	private void createSheetHeaders(String sheetName, List<? extends TemplateColumn> cols) {
		Sheet dsSheet = workbook.getSheet(sheetName);
		Row h = dsSheet.createRow(0);
		int cIdx = 0;
		for (TemplateColumn t : cols) {
			h.createCell(cIdx++).setCellValue(t.getHeader());
		}
		//call extension point and get the new column index in return
		doOptionalCreateSheetHeader(h, cIdx);
	}

	protected int doOptionalCreateSheetHeader(Row h, int cIdx) {
		// Extension point for additional export columns (example : search columns)
		return cIdx;
	}

	private void createRequirementHeaders() {
		List<RequirementSheetColumn> columns = milestonesEnabled ? REQUIREMENT_COLUMNS_MILESTONES : REQUIREMENT_COLUMNS;
		createSheetHeaders(REQUIREMENT_SHEET, columns);
	}

	private void appendOneRequirement(Sheet reqSheet, int rowIndex,
									  RequirementModel reqModel) {
		Row row = reqSheet.createRow(rowIndex);
		int colIndex = 0;

		try {
			row.createCell(colIndex++).setCellValue(reqModel.getProjectId());
			row.createCell(colIndex++).setCellValue(reqModel.getProjectName());
			row.createCell(colIndex++).setCellValue(reqModel.getPath());
			row.createCell(colIndex++).setCellValue(reqModel.getRequirementIndex());
			row.createCell(colIndex++).setCellValue(reqModel.getRequirementVersionNumber());
			row.createCell(colIndex++).setCellValue(reqModel.getReference());
			row.createCell(colIndex++).setCellValue(reqModel.getName());
			row.createCell(colIndex++).setCellValue(reqModel.getCriticality().toString());
			row.createCell(colIndex++).setCellValue(reqModel.getCategoryCode());
			row.createCell(colIndex++).setCellValue(reqModel.getStatus().toString());
			row.createCell(colIndex++).setCellValue(reqModel.getDescription());
			row.createCell(colIndex++).setCellValue(reqModel.getRequirementVersionCoveragesSize());
			row.createCell(colIndex++).setCellValue(reqModel.getAttachmentListSize());
			row.createCell(colIndex++).setCellValue(format(reqModel.getCreatedOn()));
			row.createCell(colIndex++).setCellValue(reqModel.getCreatedBy());
			row.createCell(colIndex++).setCellValue(format(reqModel.getLastModifiedOn()));
			row.createCell(colIndex++).setCellValue(reqModel.getLastModifiedBy());
			if (milestonesEnabled) {
				row.createCell(colIndex++).setCellValue(reqModel.getMilestonesLabels());
			}
			appendCustomFields(row, "REQ_VERSION_CUF_", reqModel.getCufs());
			//call extension point and get the new column index in return
			colIndex = doOptionnalAppendRequirement(row, colIndex, reqModel);
		} catch (IllegalArgumentException wtf) {
			reqSheet.removeRow(row);
			row = reqSheet.createRow(rowIndex);
			row.createCell(0).setCellValue(errorCellTooLargeMessage);
		}
	}

	protected int doOptionnalAppendRequirement(Row row, int colIndex,
											   RequirementModel reqModel) {
		// Extension point for additional export columns (example : search columns)
		return colIndex;
	}

	private void removeRteFormat(RequirementExportModel model) {
		removeRteFormatFromRequirement(model.getRequirementsModels());
	}

	private void removeRteFormatFromRequirement(
		List<RequirementModel> requirementsModels) {
		for (RequirementModel requirementModel : requirementsModels) {
			requirementModel.setDescription(removeHtml(requirementModel.getDescription()));
		}
	}

	private String removeHtml(String html) {
		return html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", "");
	}

	public File print() {
		try {
			File temp = File.createTempFile("req_export_", "xls");
			temp.deleteOnExit();

			FileOutputStream fos = new FileOutputStream(temp);
			workbook.write(fos);
			fos.close();

			return temp;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void appendCustomFields(Row r, String codePrefix, List<CustomField> cufs) {

		for (CustomField cuf : cufs) {

			String code = codePrefix + cuf.getCode();
			Integer idx = cufColumnsByCode.get(code);

			// if unknown : register it
			if (idx == null) {
				idx = registerCuf(r.getSheet(), code);
			}

			Cell c = r.createCell(idx);
			String value = nullSafeValue(cuf);
			c.setCellValue(value);
		}
	}

	private String nullSafeValue(CustomField customField) {
		String value = customField.getValue();
		return value == null ? "" : value;
	}

	private int registerCuf(Sheet sheet, String code) {

		Row headers = sheet.getRow(0);
		int nextIdx = headers.getLastCellNum();
		headers.createCell(nextIdx).setCellValue(code);

		cufColumnsByCode.put(code, nextIdx);

		return nextIdx;
	}

	private String format(Date date) {
		if (date == null) {
			return "";
		} else {
			return DateUtils.formatIso8601Date(date);
		}
	}

	// for now we care only of Excel 2003
	private void createWorkbook() {
		Workbook wb = new HSSFWorkbook();
		wb.createSheet(REQUIREMENT_SHEET);
		wb.createSheet(COV_SHEET);

		this.workbook = wb;
	}

	private void createHeaders() {
		createRequirementHeaders();
		createCoverageHeaders();
	}


}

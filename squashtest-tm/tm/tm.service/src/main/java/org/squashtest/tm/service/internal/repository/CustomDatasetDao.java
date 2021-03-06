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
package org.squashtest.tm.service.internal.repository;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.squashtest.tm.domain.testcase.Dataset;
/**
 * 
 * @author flaurens, mpagnon, bsiri
 *
 */
public interface CustomDatasetDao {

	
	
	/**
	 * Will return all datasets for the given test case.
	 * @param testCaseId
	 * 
	 * @return the list of all test cases's datasets.
	 */
	@UsesANamedQueryInPackageInfoOrElsewhere
	List<Dataset> findOwnDatasetsByTestCase(@Param("testCaseId") Long testCaseId);

	
	

	/**
	 * Will return all datasets found for the given test cases ids.
	 * 
	 * @param testCaseIds : the concerned test cases ids.
	 * @return the list of all given test cases's datasets
	 */
	List<Dataset> findOwnDatasetsByTestCases(List<Long> testCaseIds);


	/**
	 * Given a test case TC, will return all the datasets that directly inherits
	 * from parameters of this test case.
	 * Note that the datasets that belong to this TC are NOT included.
	 * 
	 * @param testCaseId
	 * @return
	 */
	List<Dataset> findImmediateDelegateDatasets(Long testCaseId);

	/**
	 * Given a test case TC, will return all the datasets
	 * that inherits directly and transitively some parameters from TC.
	 * Note that the datasets that belong to this TC are NOT included.
	 * 
	 * @param testCaseId
	 * @return
	 */
	List<Dataset> findAllDelegateDatasets(Long testCaseId);

	/**
	 * equivalent to findOwnDatasetsByTestCase(id) + findAllDelegateDatasets(id)
	 * 
	 * @param testCaseId
	 * @return
	 */
	List<Dataset> findOwnAndDelegateDatasets(Long testCaseId);




	/**
	 * TODO
	 * @param datasetId
	 */
	void removeDatasetFromTestPlanItems(Long datasetId);
}

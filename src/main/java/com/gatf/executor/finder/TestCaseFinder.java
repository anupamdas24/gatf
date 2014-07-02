package com.gatf.executor.finder;

/*
Copyright 2013-2014, Sumeet Chhetri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.AlphanumComparator;
import com.gatf.executor.core.AcceptanceTestContext;
import com.gatf.executor.core.TestCase;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * @author Sumeet Chhetri
 * Defines contract to find all test cases from files inside a given test case directory
 */
public abstract class TestCaseFinder {

	public enum TestCaseFileType
	{
		XML(".xml"),
		JSON(".json"),
		CSV(".csv");
		
		public String ext;
		
		private TestCaseFileType(String ext)
		{
			this.ext = ext;
		}
	}
	
	protected abstract TestCaseFileType getFileType();
	public abstract List<TestCase> resolveTestCases(File testCaseFile) throws Exception;
	
	private static final FileFilter dfilter = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};
	
	public static void getFiles(File dir, FilenameFilter filter, List<File> fileLst)
	{
		if (dir.isDirectory()) {
			File[] files = dir.listFiles(filter);
			for (File file : files) {
				fileLst.add(file);
				getFiles(file, filter, fileLst);
			}
			
			files = dir.listFiles(dfilter);
			for (File file : files) {
				getFiles(file, filter, fileLst);
			}
		}
	}
	
	public List<TestCase> findTestCases(File dir, AcceptanceTestContext context, boolean considerConfig)
	{
		if(context==null)
			considerConfig = false;
		
		String[] ignoreFiles = considerConfig?context.getGatfExecutorConfig().getIgnoreFiles():null;
		String[] orderedFiles = considerConfig?context.getGatfExecutorConfig().getOrderedFiles():null;
		boolean isOrderByFileName = considerConfig?context.getGatfExecutorConfig().isOrderByFileName():false;
		
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File folder, String name) {
				return name.toLowerCase().endsWith(getFileType().ext);
			}
		};
		
		List<TestCase> testcases = new ArrayList<TestCase>();
		if (dir.isDirectory()) {
			List<File> files = new ArrayList<File>();
			getFiles(dir, filter, files);
			
			List<File> allFiles = new ArrayList<File>();
			if(considerConfig)
			{
				if(orderedFiles!=null)
				{
					for (String fileN : orderedFiles) {
						for (File file : files) {
							if(file.getName().equals(fileN)) {
								allFiles.add(file);
								break;
							}
						}
					}
					for (File file : files) {
						if(!allFiles.contains(file)) {
							allFiles.add(file);
						}
					}
				}
				else
				{
					for (File file : files) {
						allFiles.add(file);
					}
					AlphanumComparator comparator = new AlphanumComparator();
					if(isOrderByFileName) {
						Collections.sort(allFiles, comparator);
					}
				}
	
				if(ignoreFiles!=null)
				{
					for (String fileN : ignoreFiles) {
						fileN = fileN.trim();
						if(fileN.isEmpty()) {
							continue;
						}
						if(fileN.equals("*") || fileN.equals("*.*")) {
							return testcases;
						}
					}
				}
			}
			else
			{
				for (File file : files) {
					allFiles.add(file);
				}
			}
			
			for (File file : allFiles) {
				boolean isIgnore = false;
				
				if(considerConfig && ignoreFiles!=null)
				{
					for (String fileN : ignoreFiles) {
						fileN = fileN.trim();
						if(fileN.isEmpty()) {
							continue;
						}
						
						if(fileN.startsWith("*.")) {
							String ext = fileN.substring(2);
							if(file.getName().endsWith(ext)) {
								isIgnore = true; 
							}
						} else if(fileN.endsWith("*")) {
							fileN = fileN.substring(0, fileN.lastIndexOf("*"));
							if(file.getName().startsWith(fileN)) {
								isIgnore = true; 
							}
						} else if(fileN.startsWith("*")) {
							fileN = fileN.substring(1);
							if(file.getName().endsWith(fileN)) {
								isIgnore = true; 
							}
						} else if(file.getName().equals(fileN)) {
							isIgnore = true;
						}
					}
					if(isIgnore)
						continue;
				}
				
				try {
					List<TestCase> testcasesTemp = resolveTestCases(file);
					if(testcasesTemp != null)
					{
						for (TestCase testCase : testcasesTemp) {
							testCase.setSourcefileName(file.getName());
							if(testCase.getSimulationNumber()==null)
							{
								testCase.setSimulationNumber(0);
							}
							if(considerConfig)
							{
								testCase.setBaseUrl(context.getGatfExecutorConfig().getBaseUrl());
							}
						}
						
						if(considerConfig)
						{
							Integer runNums = context.getGatfExecutorConfig().getConcurrentUserSimulationNum();
							if(considerConfig && context.getGatfExecutorConfig().getCompareBaseUrlsNum()!=null)
							{
								runNums = context.getGatfExecutorConfig().getCompareBaseUrlsNum();
							}
							
							context.initializeResultsHolders(runNums, file.getName());
						}
						testcases.addAll(testcasesTemp);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return testcases;
	}
}
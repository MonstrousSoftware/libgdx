/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.tiledmappacker;

import java.io.File;

/** Processes the maps located in gdx-tests-android: "assets/data/maps/tiled-atlas-src" Creates the directory
 * "assets/data/maps/tiled-atlas-processed/deleteMe" which contains processed maps. Run TiledMapPackerTestRender to render the
 * maps and, optionally, delete the created folder on exit. */
public class TiledMapPackerTest {

	// TestTypes "NoArgs" and "BadOption" do not create/process maps.
	public enum TestType {
		NoArgs, DefaultUsage, DefaultUsageWithProjectFile, Verbose, DefaultUsageWithProjectFileVerbose, StripUnused, CombineTilesets, UnusedAndCombine, BadOption
	}

	public static void main (String[] args) throws Exception {
		String path = "data/maps/";
		String input = path + "tiled-atlas-src";
		String output = path + "tiled-atlas-processed/deleteMe";
		String verboseOpt = "-v";
		String unused = "--strip-unused";
		String combine = "--combine-tilesets";
		String badOpt = "bad";

		/** There is an optional 3rd path parameter, which is specifically meant to support maps which use the custom class
		 * properties. You must specify the path to the tiled project file if your files requires it. If not, that map will be
		 * skipped during processing. Can be tested by setting TestType testType = TestType.DefaultUsageWithProjectFile; */
		String projectFilePath = input + "/tiled-prop-test.tiled-project";

		File outputDir = new File(output);
		if (outputDir.exists()) {
			System.out.println("Please run TiledMapPackerTestRender or delete \"deleteMe\" folder located in");
			System.out.println("gdx-tests-android: assets/data/maps/tiled-atlas-processed/deleteMe");
			return;
		}

		TestType testType = TestType.DefaultUsage;

		String[] noArgs = {};
		String[] defaultUsage = {input, output};
		String[] defaultUsageWithProjectFile = {input, output, projectFilePath};
		String[] defaultUsageWithProjectFileVerbose = {input, output, projectFilePath, verboseOpt};
		String[] verbose = {input, output, verboseOpt};
		String[] stripUnused = {input, output, unused};
		String[] combineTilesets = {input, output, combine};
		String[] unusedAndCombine = {input, output, unused, combine};
		String[] badOption = {input, output, unused, verboseOpt, combine, badOpt};

		switch (testType) {
		case NoArgs:
			TiledMapPacker.main(noArgs);
			break;
		case DefaultUsage:
			TiledMapPacker.main(defaultUsage);
			break;
		case DefaultUsageWithProjectFile:
			TiledMapPacker.main(defaultUsageWithProjectFile);
			break;
		case Verbose:
			TiledMapPacker.main(verbose);
			break;
		case DefaultUsageWithProjectFileVerbose:
			TiledMapPacker.main(defaultUsageWithProjectFileVerbose);
			break;
		case StripUnused:
			TiledMapPacker.main(stripUnused);
			break;
		case CombineTilesets:
			TiledMapPacker.main(combineTilesets);
			break;
		case UnusedAndCombine:
			TiledMapPacker.main(unusedAndCombine);
			break;
		case BadOption:
			TiledMapPacker.main(badOption);
			break;
		default:
			break;
		}
	}
}

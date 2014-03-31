/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.datamodeller.driver.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.kie.workbench.common.services.datamodeller.core.AnnotationDefinition;
import org.kie.workbench.common.services.datamodeller.core.DataModel;
import org.kie.workbench.common.services.datamodeller.core.impl.ModelFactoryImpl;
import org.kie.workbench.common.services.datamodeller.driver.AnnotationDriver;
import org.kie.workbench.common.services.datamodeller.driver.ModelDriver;
import org.kie.workbench.common.services.datamodeller.driver.ModelDriverException;
import org.kie.workbench.common.services.datamodeller.driver.ModelDriverListener;
import org.kie.workbench.common.services.datamodeller.parser.JavaParser;
import org.kie.workbench.common.services.datamodeller.parser.JavaParserFactory;
import org.kie.workbench.common.services.datamodeller.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.io.IOService;
import org.uberfire.java.nio.file.Path;

public class JavaModelDriver implements ModelDriver {

    private static final Logger logger = LoggerFactory.getLogger( JavaModelDriver.class );

    IOService ioService;

    boolean recursiveScan;

    Path javaRootPath;

    private ClassLoader classLoader;

    public JavaModelDriver( IOService ioService, Path javaRootPath, boolean recursiveScan, ClassLoader classLoader ) {
        this.ioService = ioService;
        this.recursiveScan = recursiveScan;
        this.javaRootPath = javaRootPath;
        this.classLoader = classLoader;
    }

    @Override public List<AnnotationDefinition> getConfiguredAnnotations( ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public AnnotationDefinition getConfiguredAnnotation( String annotationClass ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public AnnotationDriver getAnnotationDriver( String annotationClass ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public void generateModel( DataModel dataModel, ModelDriverListener generationListener ) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DataModel loadModel( ) throws ModelDriverException {

        JavaParser parser;
        DataModel dataModel;
        String fileContent;
        DataObjectBuilder objectBuilder = new DataObjectBuilder();
        dataModel = createModel();

        List<Path> rootPaths = new ArrayList<Path>(  );
        rootPaths.add( javaRootPath );

        Collection<FileUtils.ScanResult> scanResults = FileUtils.getInstance().scan( ioService, rootPaths, ".java", true );
        if (scanResults != null) {
            for (FileUtils.ScanResult scanResult : scanResults) {

                logger.debug( "Starting processing for file: " + scanResult.getFile() );
                fileContent = ioService.readAllString(scanResult.getFile());
                if (fileContent == null || "".equals( fileContent )) {
                    logger.debug( "file: " + scanResult.getFile()  + " is empty." );
                    continue;
                }
                try {
                    parser = JavaParserFactory.newParser( fileContent );
                    parser.compilationUnit();

                    //TODO check that the parsed file is a Class an not an Interface, etc.
                    if (parser.getFileDescr().getClassDescr() != null) {
                        objectBuilder.buildDataObject( dataModel, parser.getFileDescr() );
                    } else {
                        logger.debug( "No Class definition was found for file: " + scanResult.getFile() );
                    }

                } catch (Exception e) {
                    //TODO add parsing errors processing. When a file can't be parsed the user should receive
                    //a notification and the data object won't be loaded into the IU.
                    logger.error("An error was produced during file parsing: " + scanResult.getFile(), e);
                    throw new ModelDriverException(e.getMessage(), e);
                }
            }
        }
        return dataModel;
    }

    @Override public DataModel createModel( ) {
        return ModelFactoryImpl.getInstance( ).newModel();
    }
}
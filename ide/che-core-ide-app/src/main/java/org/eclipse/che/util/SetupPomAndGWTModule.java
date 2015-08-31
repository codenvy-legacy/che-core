/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.util;

import org.eclipse.che.ide.util.loging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * This is prototype (PoC) utility, that properly registers and Extension in Codenvy Project build.
 * Usecase: A new extension project is created in IDE itself, when it's about to be deployed and started
 * as dedicated Codenvy Web App with extension installed. This tool should patch the unpacked sources of
 * IDE, copy project folder and set up the project.
 * <p/>
 * <ul>
 * <li>Alter root module pom.xml:<ul>
 * <li>add dependency description</li>
 * <li>add module declaration</li>
 * </ul></li>
 * <li>Alter client module:<ul>
 * <li>pom.xml:<ul>
 * <li>add dependency</li>
 * </ul></li>
 * <li>IDE.gwt.xml<ul>
 * <li>add inherits</li>
 * </ul></li>
 * </ul></li>
 * </ul>
 *
 * @author <a href="mailto:nzamosenchuk@exoplatform.com">Nikolay Zamosenchuk</a>
 */
public class SetupPomAndGWTModule {

    public static final String GEN_START = "<!-- START OF AUTO GENERATED BLOCK -->\n";

    public static final String GEN_END = "<!-- END OF AUTO GENERATED BLOCK -->\n";

    public static final String ROOT_POM = "pom.xml";

    public static final String DEP_MANAGE_END_TAG = "</dependencyManagement>";

    public static final String MODULE_CLEINT = "<module>codenvy-ide-client</module>";

    public static final String DEP_END_TAG = "</dependencies>";

    public static final String CLIENT_POM = "codenvy-ide-client/pom.xml";

    public static final String IDE_GWT_MODULE = "codenvy-ide-client/src/main/resources/org/eclipse/che/ide/IDE.gwt.xml";

    public static final String IDE_GWT_ENTRY_TAG = "<entry-point class='org.eclipse.che.ide.client.IDE' />";

    // =======================================================================================================

    private final String mavenGroupId;

    private final String mavenArtifactId;

    private final String mavenModuleVersion;

    private final String gwtModuleFQN;

    private final File projectRoot;

    // =======================================================================================================

    private static final Logger LOG = LoggerFactory.getLogger(SetupPomAndGWTModule.class);

    /**
     * Collect extension description and process project settings
     *
     * @param args
     */
    public static void main(String args[]) {
        // validate args
        if (args.length != 4) {
            StringBuilder stringBuilder = new StringBuilder("Ooops, wrong usage. This tool requires 4 arguments to be able to register an extension properly:\n");
            stringBuilder.append("- Maven Group ID of the extension (i.e. 'org.eclipse.che.ide');\n");
            stringBuilder.append("- Maven Artifact ID of the extension (i.e. 'ide-ext-tasks');\n");
            stringBuilder.append("- Maven Module Version of the extension (i.e. '3.0');\n");
            stringBuilder.append("- GWT Module FQN (i.e. 'org.eclipse.che.ide.extension.tasks.Tasks').");
            LOG.info(stringBuilder.toString());
            return;
        }

        // read input args

        String mavenGroupId = args[0];
        String mavenArtifactId = args[1];
        String mavenModuleVersion = args[2];
        String gwtModuleFQN = args[3];
        SetupPomAndGWTModule projectWithExtensionsInitializer =
                new SetupPomAndGWTModule(mavenGroupId, mavenArtifactId, mavenModuleVersion, gwtModuleFQN,
                                         new File("."));

        // try process
        try {
            projectWithExtensionsInitializer.setupProject();
        } catch (IOException e) {
            LOG.error("Failed to setup the project.");//NOSONAR
            e.printStackTrace(); //NOSONAR // display on console
            System.exit(1); //NOSONAR abnormal exit, exception ocurred.
        }
    }

    public SetupPomAndGWTModule(String mavenGroupId, String mavenArtifactId, String mavenModuleVersion, String gwtModuleFQN,
                                File projectRoot) {
        this.mavenGroupId = mavenGroupId;
        this.mavenArtifactId = mavenArtifactId;
        this.mavenModuleVersion = mavenModuleVersion;
        this.gwtModuleFQN = gwtModuleFQN;
        this.projectRoot = projectRoot;
    }

    /**
     * Setup project, to include give Extension into the build.
     *
     * @throws IOException
     */
    public void setupProject() throws IOException {
        try {
            processRootPom();
        } catch (Exception e) {
            throw new IOException("Failed to process Root POM XML", e);
        }
        try {
            processClientPom();
        } catch (Exception e) {
            throw new IOException("Failed to process Client module POM XML", e);
        }
        try {
            processClientGwtModuleXML();
        } catch (Exception e) {
            throw new IOException("Failed to process Client GWT XML", e);
        }
    }

    /**
     * <li>Alter root module pom.xml:<ul>
     * <li>add dependency description</li>
     * <li>add module declaration</li>    *
     *
     * @throws IOException
     */
    protected void processRootPom() throws IOException {
        // add dependency declaration
        // insert as the last one element
        File rootPom = new File(projectRoot, ROOT_POM);
        String rootPomContent = readFileContent(rootPom);
        String depString =
                GEN_START
                + String
                        .format(
                                "<dependency>%n<groupId>%s</groupId>%n<artifactId>%s</artifactId>%n<version>%s</version>%n</dependency>%n",
                                mavenGroupId, mavenArtifactId, mavenModuleVersion) + GEN_END;
        rootPomContent = rootPomContent.replace(DEP_MANAGE_END_TAG, depString + DEP_MANAGE_END_TAG);

        // add module, befor client module declaration
        String moduleString = GEN_START + String.format("<module>%s</module>%n", mavenArtifactId) + GEN_END;
        rootPomContent = rootPomContent.replace(MODULE_CLEINT, moduleString + MODULE_CLEINT);
        // write content
        writeFileContent(rootPomContent, rootPom);
    }

    // =======================================================================================================

    /** Insert dependency at the end of "dependencies" section. */
    protected void processClientPom() throws IOException {
        File pom = new File(projectRoot, CLIENT_POM);
        String pomContent = readFileContent(pom);

        String depString =
                GEN_START
                + String.format("<dependency>%n<groupId>%s</groupId>%n<artifactId>%s</artifactId>%n</dependency>%n",
                                mavenGroupId, mavenArtifactId) + GEN_END;

        // assert
        if (!pomContent.contains(DEP_END_TAG)) {
            throw new IOException(String.format("File '%s' doesn't contain '%s'. Can't process file.", CLIENT_POM,
                                                DEP_END_TAG));
        }

        pomContent = pomContent.replace(DEP_END_TAG, depString + DEP_END_TAG);
        // write content
        writeFileContent(pomContent, pom);
    }

    // =======================================================================================================


    /** Insert "inherits" tag before "entry-point". */
    protected void processClientGwtModuleXML() throws IOException {
        File gwtModuleFile = new File(projectRoot, IDE_GWT_MODULE);
        String gwtModuleContent = readFileContent(gwtModuleFile);

        String inheritsString = GEN_START + String.format("<inherits name='%s' />%n", gwtModuleFQN) + GEN_END;

        // assert
        if (!gwtModuleContent.contains(IDE_GWT_ENTRY_TAG)) {
            throw new IOException(String.format("File '%s' doesn't contain '%s'. Can't process file.", IDE_GWT_MODULE,
                                                IDE_GWT_ENTRY_TAG));
        }

        gwtModuleContent = gwtModuleContent.replace(IDE_GWT_ENTRY_TAG, inheritsString + IDE_GWT_ENTRY_TAG);
        // write content
        writeFileContent(gwtModuleContent, gwtModuleFile);
    }

    // =======================================================================================================

    /** Read file. */
    public static String readFileContent(File file) throws IOException {
        byte[] b = Files.readAllBytes(file.toPath());
        return new String(b, "UTF-8");
    }

    /** Update file content */
    public static void writeFileContent(String text, File file) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"))) {
            out.write(text);
        }
    }
}

/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.spectrumcomputing.cwl.parser.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ibm.spectrumcomputing.cwl.model.exception.CWLException;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLDirectory;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFile;
import com.ibm.spectrumcomputing.cwl.model.process.parameter.type.file.CWLFileBase;

/**
 * Utility methods for IO operations
 */
public final class IOUtil {


    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    private static final String READ_FILE_FAILED_MSG = "cwl.io.read.failed";
    private static final String CWL_IO_FILE_INVALID_PATH = "cwl.io.file.invalid.path";

    /**
     * The key for output directory
     */
    public static final String OUTPUT_TOP_DIR = "output.top.dir";
    /**
     * The key for working directory
     */
    public static final String WORK_TOP_DIR = "work.top.dir";
    /**
     * A flag to create symbol link instead of coping input files
     */
    public static final String USING_SYMBOL_LINK = "using.symbol.link";
    /**
     * The prefix of FTP protocol
     */
    public static final String FTP_PREFIX = "ftp://";
    /**
     * The prefix of HTTPS protocol
     */
    public static final String HTTPS_PREFIX = "https://";
    /**
     * The prefix of HTTP protocol
     */
    public static final String HTTP_PREFIX = "http://";
    /**
     * The prefix of file:// URI scheme
     */
    public static final String FILE_PREFIX = "file://";

    private static final String JSON_SUFFIX = ".json";

    private IOUtil() {
    }

    /**
     * Removes the secondaryFiles extensions
     * 
     * @param primary
     *            the primary file path
     * @param secondary
     *            the secondary file suffix
     * @return the secondary file path
     */
    public static String removeFileExt(String primary, String secondary) {
        String r = null;
        if (primary != null && secondary != null) {
            Matcher matcher = Pattern.compile("^\\^+").matcher(secondary);
            if (matcher.find()) {
                String carets = matcher.group(0);
                String ext = secondary.replace(carets, "");
                for (int i = 0; i < carets.length(); i++) {
                    int last = primary.lastIndexOf('.');
                    if (last != -1) {
                        primary = primary.substring(0, primary.lastIndexOf('.'));
                    }
                }
                r = primary + ext;
            }
        }
        return r;
    }

    /**
     * Splits the main id (if had) from a CWL document description file path
     * 
     * @param descFilePath
     *            A CWL document description file path
     * @return the split file path
     */
    public static String[] splitDescFilePath(String descFilePath) {
        String[] parts = null;
        if (descFilePath != null) {
            parts = new String[2];
            String[] paths = descFilePath.split("#");
            if (paths.length == 1) {
                parts[0] = paths[0];
                parts[1] = null;
            } else if (paths.length == 2) {
                parts[0] = paths[0];
                parts[1] = paths[1];
            } else {
                String last = paths[paths.length - 1];
                parts[0] = descFilePath.replace("#" + last, "");
                parts[1] = last;
            }
        }
        return parts;
    }

    /**
     * Creates an executable shell script
     * 
     * @param scriptPath
     *            the path of shell script
     * @param command
     *            the execution command
     * @throws CWLException
     *             Failed to create the shell script
     */
    public static void createCommandScript(Path scriptPath, String command) throws CWLException {
        if (scriptPath != null) {
            File scriptFile = scriptPath.toFile();
            if (scriptFile.setExecutable(true)) {
                logger.trace("Set file executable attribute.");
            }
            if (scriptFile.setReadable(true)) {
                logger.trace("Set file readable attribute.");
            }
            if (scriptFile.setWritable(true)) {
                logger.trace("Set file writable attribute.");
            }
            if (command != null) {
                write(scriptFile, command);
            } else {
                write(scriptFile, "#!/bin/bash");
            }
        }
    }

    /**
     * Remove the suffix (.*) from a given file path
     * 
     * @param cwlFilePath
     *            A file path
     * @return the file path without suffix
     */
    public static String findFileNameRoot(String cwlFilePath) {
        String cwlFileName = null;
        if (cwlFilePath != null) {
            cwlFileName = Paths.get(cwlFilePath).getFileName().toString();
            int suffixIndex = cwlFileName.lastIndexOf('.');
            if (suffixIndex != -1) {
                cwlFileName = cwlFileName.substring(0, suffixIndex);
            }
        }
        return cwlFileName;
    }

    /**
     * Downloads a file from the given URL
     * 
     * @param source
     *            An URL
     * @param dest
     *            A destination path
     * @return If the file is got, return true, otherwise, return false
     */
    public static boolean wget(URL source, Path dest) {
        if (source == null) {
            throw new IllegalArgumentException("The source URL is null");
        }
        if (dest == null) {
            throw new IllegalArgumentException("The destination file path is null");
        }
        boolean successful = true;
        try (FileOutputStream out = new FileOutputStream(dest.toFile())) {
            try (InputStream in = source.openStream()) {
                int numread = 0;
                int bufSize = 1024;
                byte[] buffer = new byte[bufSize];
                while ((numread = in.read(buffer, 0, bufSize)) != -1) {
                    out.write(buffer, 0, numread);
                }
            }
        } catch (IOException e) {
            logger.warn("Fail to download file from \"{}\" to \"{}\", {}", source, dest.toFile().getAbsolutePath(),
                    e.getMessage());
            successful = false;
        }
        return successful;
    }

    /**
     * Makes a directory by given path
     * 
     * @param owner
     *            the owner of directory
     * @param path
     *            the direcoty path
     * @throws CWLException
     *             Failed to make the directory
     */
    public static void mkdirs(String owner, Path path) throws CWLException {
        File dir = path.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new CWLException(ResourceLoader.getMessage("cwl.io.mkdir.failed", path.toString(), owner), 255);
        }
    }

    /**
     * Copies a given file (or directory) to the target file (or directory)
     * 
     * @param owner
     *            the owner of the file
     * @param src
     *            the source file path
     * @param target
     *            the target file path
     * @throws CWLException
     *             Failed to copy the file
     */
    public static void copy(String owner, Path src, Path target) throws CWLException {
        final Path finalSrc = toFinalSrcPath(src);
        if (src.toFile().exists() && Files.isReadable(src)) {
            CopyOption[] options = new CopyOption[] {
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING
            };
            final Path finalTarget = toFinalTargetPath(src, target);
            if (!finalTarget.getParent().toFile().exists()) {
                logger.debug("mkdir \"{}\"", finalTarget.getParent());
                mkdirs(owner, finalTarget.getParent());
            }
            logger.debug("copy \"{}\" to \"{}\"", finalSrc, finalTarget);
            try {
                if (finalSrc.toFile().isDirectory()) {
                    Files.walkFileTree(finalSrc, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                BasicFileAttributes attrs) throws IOException {
                            Files.copy(dir, finalTarget.resolve(finalSrc.relativize(dir)),
                                    StandardCopyOption.COPY_ATTRIBUTES);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, finalTarget.resolve(finalSrc.relativize(file)),
                                    StandardCopyOption.COPY_ATTRIBUTES);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            throw exc;
                        }
                    });
                } else {
                    Files.copy(finalSrc, finalTarget, options);
                }
            } catch (IOException e) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.io.copy.failed",
                                finalSrc.toString(),
                                finalTarget.toString(),
                                e.getMessage()),
                        255);
            }
        } else {
            throw new CWLException(ResourceLoader.getMessage("cwl.io.file.unaccessed", finalSrc.toString()),
                    255);
        }
    }

    /**
     * Find files from the given location, using POSIX glob(3) pathname matching
     * 
     * @param filePath
     *            the file will be found
     * @param location
     *            the location will be globbed
     * @return The found files
     */
    public static List<Path> glob(String filePath, Path location) {
        List<Path> matched = new ArrayList<>();
        if (filePath != null && location != null) {
            Path globFilePath = Paths.get(location.toString(), filePath);
            if (!globFilePath.toFile().exists()) {
                String globPattern = String.format("glob:**%s", filePath);
                walkFies(filePath, globPattern, location, matched);
            } else {
                matched.add(globFilePath);
            }
        }
        return matched;
    }

    /**
     * Writes contents to a given file
     * 
     * @param file
     *            A given file
     * @param contents
     *            The file contents
     * @throws CWLException
     *             Failed to write the contents
     */
    public static void write(File file, String contents) throws CWLException {
        if (file != null && contents != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] bytes = contents.getBytes(StandardCharsets.UTF_8.name());
                out.write(bytes);
            } catch (IOException e) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.io.write.failed", file.getAbsolutePath(), e.getMessage()),
                        255);
            }
        }
    }

    /**
     * Writes 64 KiB contents to a given file
     * 
     * @param file
     *            A given file
     * @param contents
     *            The file contents
     * @throws CWLException
     *             Failed to write file contents
     */
    public static void write64Kib(File file, String contents) throws CWLException {
        if (file != null && contents != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] bytes = contents.getBytes(StandardCharsets.UTF_8.name());
                byte[] buffer = new byte[bytes.length > 65536 ? 65536 : bytes.length];
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = bytes[i];
                }
                out.write(buffer);
            } catch (IOException e) {
                throw new CWLException(
                        ResourceLoader.getMessage("cwl.io.write.failed", file.getAbsolutePath(), e.getMessage()),
                        255);
            }
        }
    }

    /**
     * Reads the file contents from a file with UTF-8
     * 
     * @param file
     *            A given file
     * @return the file contents with UTF-8
     * @throws CWLException
     *             Failed to read the file contents
     */
    public static String read(File file) throws CWLException {
        StringBuilder contents = new StringBuilder();
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = in.read(buffer)) > 0) {
                contents.append(new String(buffer, 0, read, StandardCharsets.UTF_8.name()));
            }
        } catch (IOException e) {
            throw new CWLException(
                    ResourceLoader.getMessage(READ_FILE_FAILED_MSG, file.getAbsolutePath(), e.getMessage()),
                    255);
        }
        return contents.toString();
    }

    /**
     * Reads the file contents only 64KiB from a file with UTF-8
     * 
     * @param file
     *            A given file
     * @return The file contents with UTF-8
     * @throws CWLException
     *             Failed to read the file contents
     */
    public static String read64KiB(File file) throws CWLException {
        String contents = null;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int readNumber = in.read(buffer);
            if (readNumber != -1) {
                byte[] readed = new byte[readNumber];
                for (int i = 0; i < readNumber; i++) {
                    readed[i] = buffer[i];
                }
                contents = new String(readed, StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new CWLException(
                    ResourceLoader.getMessage(READ_FILE_FAILED_MSG, file.getAbsolutePath(), e.getMessage()),
                    255);
        }
        return contents;
    }

    /**
     * Reads the LSF stdout
     * 
     * @param filePath
     *            the path of LSF stdout
     * @return the contents of LSF stdout
     */
    public static StringBuilder readLSFOutputFile(Path filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        if (filePath != null && filePath.toFile().exists()) {
            Stream<String> stream = null;
            try {
                stream = Files.lines(filePath, StandardCharsets.UTF_8);
                stream.forEach(s -> {
                    if ((s.length() != 0) && (!s.startsWith("Sender") &&
                            !s.startsWith("Subject") &&
                            !s.startsWith("Your") &&
                            !s.startsWith("PS") &&
                            !s.startsWith("Read"))) {
                        contentBuilder.append(s).append(System.getProperty("line.separator"));
                    }
                });
            } catch (IOException e) {
                logger.error(ResourceLoader.getMessage(READ_FILE_FAILED_MSG, filePath, e.getMessage()));
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
        return contentBuilder;
    }

    /**
     * Reads the contents from a Javascipt file, the code comments will be
     * replaced
     * 
     * @param filePath
     *            The Javascript file path
     * @return The contents of the Javascript file
     */
    public static StringBuilder readJSFile(Path filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        if (filePath != null && filePath.toFile().exists()) {
            Stream<String> stream = null;
            try {
                stream = Files.lines(filePath, StandardCharsets.UTF_8);
                stream.forEach(s -> {
                    if ((s.length() != 0) && (!s.trim().startsWith("//"))) {
                        contentBuilder.append(s.replaceAll("\r", "").replaceAll("\n", ""));
                    }
                });
            } catch (IOException e) {
                logger.error(ResourceLoader.getMessage(READ_FILE_FAILED_MSG, filePath, e.getMessage()));
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
        return contentBuilder;
    }

    /**
     * Traverses a directory and find the files in it
     * 
     * @param dir
     *            A directory
     * @param listing
     *            A list that contains the files in the directory
     * @param nochecksum Controls to calculate file checksum 
     */
    public static void traverseDirListing(String dir, List<CWLFileBase> listing, boolean nochecksum) {
        File file = new File(dir);
        File[] subFiles = file.listFiles();
        if (subFiles != null && subFiles.length != 0) {
            for (File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    CWLDirectory cwlDir = toCWLDirectory(Paths.get(subFile.getAbsolutePath()));
                    if (cwlDir != null) {
                        listing.add(cwlDir);
                        traverseDirListing(subFile.getAbsolutePath(), cwlDir.getListing(), nochecksum);
                    }
                } else {
                    CWLFile cwlFile = toCWLFile(Paths.get(subFile.getAbsolutePath()), nochecksum);
                    listing.add(cwlFile);
                }
            }
        }
    }

    /**
     * Creates a CWLDirectory object from a directory
     * 
     * @param cwlDirPath
     *            The path of a directory
     * @return A CWLDirectory object
     */
    public static CWLDirectory toCWLDirectory(Path cwlDirPath) {
        CWLDirectory cwlDir = null;
        if (cwlDirPath != null) {
            cwlDir = new CWLDirectory();
            cwlDir.setBasename(cwlDirPath.getFileName().toString());
            cwlDir.setLocation(FILE_PREFIX + cwlDirPath.toString());
            cwlDir.setPath(cwlDirPath.toString());
            cwlDir.setListing(new ArrayList<>());
        }
        return cwlDir;
    }

    /**
     * Produce a File instance according to file URI. It it's a remote file,
     * download it first.
     * 
     * @param fileURI
     *            File URI
     * @param tmpDir
     *            Locale directory to save remote file
     * @param exts
     *            File extensions, like '.cwl'. If specified, file extension
     *            will be checked.
     * @param needSuffix
     *            If true, generate random suffix for remote file when saving as
     *            locale file.
     * @return A file instance
     * @throws CWLException
     *             Failed to create a file
     */
    public static File yieldFile(String fileURI,
            String tmpDir,
            String[] exts,
            boolean needSuffix) throws CWLException {
        if (fileURI == null) {
            throw new IllegalArgumentException("The file path is null.");
        }
        // check file extension if exts is specified
        if ((exts != null && exts.length != 0) && !validateExts(fileURI, exts)) {
            int index = fileURI.lastIndexOf('.');
            throw new CWLException(ResourceLoader.getMessage("cwl.io.file.invalid.ext",
                    fileURI,
                    (index == -1 ? "" : fileURI.substring(index)),
                    String.join(", ", exts)),
                    255);
        }
        // download file if it's not a local file
        if (fileURI.startsWith(HTTP_PREFIX)
                || fileURI.startsWith(HTTPS_PREFIX)
                || fileURI.startsWith(FTP_PREFIX)) {
            fileURI = downloadFile(fileURI, tmpDir, needSuffix);
        }
        Path descriptionFilePath = Paths.get(fileURI);
        if (!descriptionFilePath.isAbsolute()) {
            descriptionFilePath = Paths.get(System.getProperty("user.dir"), fileURI);
        }
        return descriptionFilePath.toFile();
    }

    /**
     * Return the base URI of a file URI.
     * 
     * @param fileURI
     *            A file URI
     * @return The base URI
     * @throws CWLException
     *             Failed to resolve the given URI
     */
    public static String resolveBaseURI(String fileURI) throws CWLException {
        if (fileURI == null) {
            throw new IllegalArgumentException("fileURI is null.");
        }
        if (fileURI.startsWith(HTTP_PREFIX) ||
                fileURI.startsWith(HTTPS_PREFIX) ||
                fileURI.startsWith(FTP_PREFIX) ||
                fileURI.startsWith(FILE_PREFIX)) {
            int index = fileURI.lastIndexOf('/');
            if (index > fileURI.indexOf("//") + 1) {
                return fileURI.substring(0, index);
            } else {
                throw new CWLException(ResourceLoader.getMessage(CWL_IO_FILE_INVALID_PATH, fileURI), 255);
            }
        } else {
            return Paths.get(fileURI).getParent().toString();
        }
    }

    /**
     * Resolve file URI if the file URI is a relative URI
     * 
     * @param baseURI
     *            The base URI where the source file locates
     * @param importURI
     *            The URI of import file, it might be a relative URI
     * @return A resolved file URI
     */
    public static String resolveImportURI(String baseURI, String importURI) {
        if (importURI == null) {
            throw new IllegalArgumentException("importURI is null.");
        }
        // case 1: absolute URI of a remote file
        if (importURI.startsWith(HTTP_PREFIX) ||
                importURI.startsWith(HTTPS_PREFIX) ||
                importURI.startsWith(FTP_PREFIX)) {
            return importURI;
        }
        // case 2: absolute URI of a local file
        if (importURI.startsWith(FILE_PREFIX)) {
            return importURI.replaceFirst(FILE_PREFIX, "");
        }
        // case 3: absolute file path of a local file
        Path path = Paths.get(importURI);
        if (path.isAbsolute()) {
            return importURI;
        }
        if (baseURI == null) {
            throw new IllegalArgumentException("baseURI is null.");
        }
        // case 4: relative URI of a remote file
        if (baseURI.startsWith(HTTP_PREFIX) ||
                baseURI.startsWith(HTTPS_PREFIX) ||
                baseURI.startsWith(FTP_PREFIX)) {
            return baseURI.endsWith("/") ? baseURI + importURI : baseURI + "/" + importURI;
        }
        // case 5: relative URI of a local file
        if (baseURI.startsWith(FILE_PREFIX)) {
            baseURI = baseURI.replaceFirst(FILE_PREFIX, "");
            return baseURI.endsWith("/") ? baseURI + importURI : baseURI + "/" + importURI;
        }
        // case 6: relative file path of a local file
        path = Paths.get(baseURI, importURI);
        return path.toString();
    }

    /**
     * Creates a CWLFile object from a file
     * 
     * @param cwlFilePath
     *            the path of a file
     * @param nochecksum
     *            If true, the md5 of file will not be calculated
     * @return A CWLFile object
     */
    public static CWLFile toCWLFile(Path cwlFilePath, boolean nochecksum) {
        CWLFile cwlFile = null;
        if (cwlFilePath != null) {
            cwlFile = new CWLFile();
            String basename = cwlFilePath.getFileName().toString();
            cwlFile.setBasename(basename);
            cwlFile.setLocation(FILE_PREFIX + cwlFilePath.toString());
            cwlFile.setPath(cwlFilePath.toString());
            if (!nochecksum) {
                cwlFile.setChecksum("sha1$" + IOUtil.md5(cwlFilePath.toString()));
            }
            cwlFile.setSize(cwlFilePath.toFile().length());
            if (basename.lastIndexOf('.') != -1) {
                cwlFile.setNameroot(basename.substring(0, basename.lastIndexOf('.')));
                cwlFile.setNameext(basename.substring(basename.lastIndexOf('.')));
            }
            if (cwlFilePath.getParent() != null) {
                cwlFile.setDirname(cwlFilePath.getParent().toString());
            }
        }
        return cwlFile;
    }

    /**
     * Calculate a file md5 by SHA1
     * 
     * @param inputFile
     *            A path of a file
     * @return the file md5
     */
    public static String md5(String inputFile) {
        String md5 = null;
        logger.debug("Start to calculate hashcode (SHA1) for {}", inputFile);
        try (DigestInputStream digestIn = new DigestInputStream(new FileInputStream(inputFile),
                MessageDigest.getInstance("SHA1"))) {
            byte[] buffer = new byte[1024 * 1024];
            while (digestIn.read(buffer) > 0) {
                // do nothing
            }
            md5 = toHexString(digestIn.getMessageDigest().digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.warn("Fail to md5 for {} ({})", inputFile, e.getMessage());
        }
        logger.debug("End to calculate hashcode (SHA1) for {}, {}", inputFile, md5);
        return md5;
    }

    /**
     * Converts a JSON file to a JSON object
     * 
     * @param file
     *            a JSON file
     * @return a JSON object
     * @throws IOException
     *             Failed to convert a JSON file
     */
    public static JsonNode toJsonNode(File file) throws IOException {
        return toJsonNode(file, true);
    }

    /**
     * Converts a JSON file to a JSON object
     * 
     * @param file
     *            a JSON file
     * @param enableFileTypeDetection
     *            A flag to enable file type detection
     * @return a JSON object
     * @throws IOException
     *             Failed to convert a JSON file
     */
    public static JsonNode toJsonNode(File file, boolean enableFileTypeDetection) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file is null.");
        }
        ObjectMapper mapper = null;
        // get file type from file extension
        boolean isJson = file.getName().endsWith(JSON_SUFFIX);
        if (isJson) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        try {
            return mapper.readTree(file);
        } catch (JsonProcessingException e) {
            if (!enableFileTypeDetection) {
                throw e;
            }
            JsonProcessingException exception = e;
            // if the parsing fails, try to parse file ends with '.json' as yaml
            // file
            // and file not ends with '.json' as json file
            if (isJson) {
                mapper = new ObjectMapper(new YAMLFactory());
            } else {
                mapper = new ObjectMapper();
            }
            try {
                return mapper.readTree(file);
            } catch (JsonProcessingException ex) {
                throw exception;
            }
        }
    }

    private static void walkFies(String filePath, String globPattern, Path location, List<Path> matched) {
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(globPattern);
        try {
            Files.walkFileTree(location, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(dir)) {
                        matched.add(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (pathMatcher.matches(file)) {
                        matched.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warn("Fail to glob {} in {}, ({})", filePath, location, e.getMessage());
        }
    }

    private static boolean validateExts(String fileURI, String[] exts) {
        boolean matched = false;
        for (String ext : exts) {
            if (fileURI.endsWith(ext)) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    private static String downloadFile(String fileURI, String tmpDir, boolean needSuffix) throws CWLException {
        if (tmpDir == null) {
            tmpDir = System.getProperty(WORK_TOP_DIR, System.getProperty("java.io.tmpdir"));
        }
        // throw exception if no file name exists in file URI
        int index = fileURI.lastIndexOf('/');
        if (index <= fileURI.indexOf("//") + 1) {
            throw new CWLException(ResourceLoader.getMessage(CWL_IO_FILE_INVALID_PATH, fileURI), 255);
        }
        String fileName = fileURI.substring(index + 1);
        // add random suffix to avoid file name conflict
        if (needSuffix) {
            index = fileName.lastIndexOf('.');
            if (index == -1) {
                fileName += "_" + CommonUtil.getRandomStr();
            } else {
                fileName = fileName.substring(0, index) + "_" + CommonUtil.getRandomStr() + fileName.substring(index);
            }
        }
        Path dest = Paths.get(tmpDir, fileName);
        // download remote file
        try {
            IOUtil.wget(new URL(fileURI), dest);
        } catch (MalformedURLException e) {
            throw new CWLException(ResourceLoader.getMessage(CWL_IO_FILE_INVALID_PATH, fileURI), 255);
        }
        return dest.toString();
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder hexStrings = new StringBuilder();
        for (int n = 0; n < bytes.length; n++) {
            String hexString = Integer.toHexString(bytes[n] & 0XFF);
            if (hexString.length() == 1) {
                hexStrings.append("0" + hexString);
            } else {
                hexStrings.append(hexString);
            }
        }
        return hexStrings.toString();
    }

    private static Path toFinalSrcPath(Path src) {
        Path finalPath = src;
        if (".".equals(src.getFileName().toString())) {
            finalPath = src.getParent();
        }
        return finalPath;
    }

    private static Path toFinalTargetPath(Path src, Path target) {
        Path finalTargetPath = target;
        if (target.toFile().isDirectory()) {
            if (".".equals(src.getFileName().toString())) {
                finalTargetPath = target.getParent();
                //Files.copy will re-create it
                try {
                    Files.delete(finalTargetPath);
                } catch (IOException e) {
                    logger.warn("Failed to delete {}", finalTargetPath);
                }
            } else {
                finalTargetPath = target.resolve(src.getFileName().toString());
            }
        }
        return finalTargetPath;
    }
}
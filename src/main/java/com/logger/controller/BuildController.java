package com.logger.controller;

import com.logger.model.BUILDSTATUS;
import com.logger.model.BuildModel;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class BuildController {

    private static RestTemplate httpsRestTemplate;
    static String gitUrl = "https://github.com/Portabull/PortabullMonolitic/archive/refs/heads/master.zip";
    static String deployableLocation = "C:\\Users\\91850\\Documents\\deployments\\servs\\";
    static String port = "443";
    static String TEMP_DIR = "java.io.tmpdir";
    static String TEMP_DIR_BUILD = "temp_dir_logger_portabull_deploy_jars_6548978";
    Map<String, BuildModel> buildDetails = new HashMap<>();

    @GetMapping("get-build-details")
    public ResponseEntity<?> getBuildDetails() {

        Map<String, Object> response = new HashMap<>();

        response.put("port", port);

        response.put("gitUrl", gitUrl);

        response.put("deployableLocation", deployableLocation);

        response.put("status", "SUCCESS");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("save-build-details")
    public ResponseEntity<?> saveBuildDetails(@RequestBody Map<String, Object> payload) {

        port = payload.get("port").toString();

        gitUrl = payload.get("gitUrl").toString();

        deployableLocation = payload.get("deployableLocation").toString();

        return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }


    @RequestMapping("/build-new-artifact")
    public synchronized ResponseEntity<?> buildNewArtifact(@RequestBody Map<String, Object> payload) throws IOException {
        String tempDir = System.getProperty(TEMP_DIR) + File.separator + TEMP_DIR_BUILD + File.separator;

        String buildNumber = payload.get("buildNumber").toString();

        BuildModel buildModel = buildDetails.get(buildNumber);

        buildModel.setBuildStatus(BUILDSTATUS.DOWNLOADING);

        File deployableDir = new File(tempDir + buildNumber);

        deployableDir.mkdirs();

        String fileName = deployableDir.getAbsolutePath() + File.separator + buildNumber + ".zip";

        ResponseEntity<byte[]> forEntity = new RestTemplate().getForEntity(gitUrl, byte[].class);

        if (forEntity.getStatusCodeValue() == 200) {

            try (OutputStream os = new FileOutputStream(fileName)) {
                os.write(forEntity.getBody());
            }

            buildModel.setBuildStatus(BUILDSTATUS.UNZIPPING);

            unzip(fileName, deployableDir.getAbsolutePath());

        }

        new File(fileName).delete();

        buildModel.setBuildStatus(BUILDSTATUS.BUILD);

        Runtime rt = Runtime.getRuntime();

        String dir = deployableDir.listFiles()[0].getAbsolutePath() + File.separator;
        String command = "mvn clean install";

        String[] commands = {"cmd.exe", "/c",
                "cd " + dir + " && " + command};

        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String s = null;
        StringBuilder buildOutput = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            buildOutput.append("<p>").append(s).append("</p>");
        }

        while ((s = stdError.readLine()) != null) {
            buildOutput.append("<p>").append(s).append("</p>");
        }

        buildModel.setBuildResponse(buildOutput.toString());

        buildModel.setBuildStatus(BUILDSTATUS.SHUTDOWN);

        shutdownCurrentApplication(port);

        buildModel.setBuildStatus(BUILDSTATUS.MOVING);

        String jarLocation = dir + "Services\\PortabullService\\${JAR_LOCATION}\\PortabullService\\PortabullService-exec.jar";

        String jarTargetDir = deployableLocation;

        new File(jarTargetDir).mkdirs();

        String finalExeJar = jarTargetDir + new File(jarLocation).getName();

        new File(finalExeJar).delete();

        try (InputStream stream = new FileInputStream(jarLocation)) {

            try (OutputStream outputStream = new FileOutputStream(finalExeJar)) {
                outputStream.write(IOUtils.toByteArray(stream));
            }

        }

//        Files.move(Paths.get(jarLocation), Paths.get(finalExeJar), StandardCopyOption.REPLACE_EXISTING);

        String jarRunnableCommand = "start javaw -Xmx4000m -Dserver.port=" + port + " -jar " + finalExeJar;

        String batFilePath = generateBatFile(jarRunnableCommand);

        try {
            Runtime.getRuntime().exec(batFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        buildModel.setBuildStatus(BUILDSTATUS.RUNNING);

        StringBuilder buildResponse = new StringBuilder();

        buildResponse.append(buildModel.getBuildResponse());

        deleteDirectory(deployableDir);

        return new ResponseEntity<>(buildResponse.toString(), HttpStatus.OK);

    }

    public static void deleteDirectory(File file) {

        deleteSubDirectory(file);

        file.delete();

    }

    public static void deleteSubDirectory(File file) {
        for (File subfile : file.listFiles()) {
            if (subfile.isDirectory()) {
                deleteDirectory(subfile);
            }
            subfile.delete();
        }
    }

    private String generateBatFile(String jarRunnableCommand) {
        String filePath = prepareTempPath() + File.separator + prepareTempName("jarExecution.bat");
        try {
            FileWriter myWriter = new FileWriter(filePath);
            myWriter.write(jarRunnableCommand);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public String prepareTempPath() {
        String tmpDir = System.getProperty("java.io.tmpdir");

        if (!tmpDir.endsWith(File.separator)) {
            tmpDir = tmpDir + File.separator;
        }

        tmpDir = tmpDir + "portabull_server_temp_dir_files_sd" + File.separator;

        if (!new File(tmpDir).exists()) {
            new File(tmpDir).mkdirs();
        }
        return tmpDir;
    }

    private static String prepareTempName(String defaultFileName) {
        return new StringBuilder("Doc").append("_").
                append(new Date().getTime()).append("_").
                append(new Random().nextInt(1000)).append(defaultFileName).toString();
    }

    private void shutdownCurrentApplication(String port) {

        try {
            String url = "https://localhost:" + port + "/APIGateway/actuator/shutdown";

            HttpEntity entity = new HttpEntity(new HttpHeaders());

            String response = BuildController.httpsRestTemplate.postForEntity(url, entity, String.class).getBody();
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("/get-build-status")
    public ResponseEntity<?> getBuildStatus(@RequestParam String buildNumber, @RequestParam boolean showBuildOutput) {

        Map<String, Object> response = new HashMap<>();

        BuildModel buildModel = buildDetails.get(buildNumber);

        if (showBuildOutput) {
            response.put("buildOutput", buildModel.getBuildResponse());
        }

        response.put("status", "SUCCESS");

        response.put("buildStatus", buildModel.getBuildStatus());

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping("/get-build-number")
    public synchronized ResponseEntity<?> getBuildNumber() {

        long build = new Date().getTime();

        String buildNumber = new StringBuilder("build_").append(build).append(random(3, true, false)).toString();

        buildDetails.put(buildNumber, new BuildModel(buildNumber));

        return new ResponseEntity<>(buildNumber, HttpStatus.OK);

    }

    public static void unzip(String zipFile, String outputFolder) throws IOException {

        final byte[] buffer = new byte[1024];
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            final File newFile = newFile(new File(outputFolder), zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                final FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private static final Random RANDOM = new Random();

    public static String random(int count, boolean letters, boolean numbers) {
        int start = 0;
        int end = 0;
        if (count == 0) {
            return "";
        } else if (count < 0) {
            throw new IllegalArgumentException("Requested random string length " + count + " is less than 0.");
        } else {
            if (start == 0 && end == 0) {
                end = 123;
                start = 32;
                if (!letters && !numbers) {
                    start = 0;
                    end = Integer.MAX_VALUE;
                }
            }

            char[] buffer = new char[count];
            int gap = end - start;

            while (true) {
                while (true) {
                    while (count-- != 0) {
                        char ch;

                        ch = (char) (RANDOM.nextInt(gap) + start);


                        if (letters && Character.isLetter(ch) || numbers && Character.isDigit(ch) || !letters && !numbers) {
                            if (ch >= '\udc00' && ch <= '\udfff') {
                                if (count == 0) {
                                    ++count;
                                } else {
                                    buffer[count] = ch;
                                    --count;
                                    buffer[count] = (char) ('\ud800' + RANDOM.nextInt(128));
                                }
                            } else if (ch >= '\ud800' && ch <= '\udb7f') {
                                if (count == 0) {
                                    ++count;
                                } else {
                                    buffer[count] = (char) ('\udc00' + RANDOM.nextInt(128));
                                    --count;
                                    buffer[count] = ch;
                                }
                            } else if (ch >= '\udb80' && ch <= '\udbff') {
                                ++count;
                            } else {
                                buffer[count] = ch;
                            }
                        } else {
                            ++count;
                        }
                    }

                    return new String(buffer);
                }
            }
        }
    }

    @Bean
    public RestTemplateBuilder buildNoHostVerifier() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(), NoopHostnameVerifier.INSTANCE);

        HttpClient client = HttpClients.custom().setSSLSocketFactory(scsf).build();

        Supplier<ClientHttpRequestFactory> clientHttpRequestFactorySupplier = () -> new HttpComponentsClientHttpRequestFactory(client);

        return restTemplateBuilder.requestFactory(clientHttpRequestFactorySupplier);
    }

    @Autowired
    public void setRestTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
        BuildController.httpsRestTemplate = restTemplateBuilder.build();
    }

}

package com.logger.controller;

import com.logger.model.ChatMessage;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class LoggerController {

    @Autowired
    ChatController chatController;

    boolean flag = true;


    @Autowired
    SimpMessagingTemplate template;


    @GetMapping("/dfhsgdhfghsgdhsgd")
    public void gsyg() {
        ChatMessage ch = new ChatMessage();
        ch.setSender("ddfgf");
        ch.setContent("Hi Babu");
        ch.setType(ChatMessage.MessageType.CHAT);
        chatController.sendMessage(ch);
    }

    @GetMapping(value = "/send")
    @ResponseStatus(HttpStatus.OK)
    public void invokeSend(@RequestParam String a) throws Exception {
        ChatMessage ch = new ChatMessage();
        ch.setSender("ddfgf");
        ch.setContent("Hi Babu" + a);
        ch.setType(ChatMessage.MessageType.CHAT);
        template.convertAndSend("/topic/public", ch);
    }

//    public static void main(String args[]) throws IOException {
//        String fileName = "C:\\Users\\91850\\Documents\\logs\\portabull.log";
//        try {
//            RandomAccessFile bufferedReader = new RandomAccessFile( fileName, "r"
//            );
//
//            long filePointer;
//            while ( true ) {
//                final String string = bufferedReader.readLine();
//
//                if ( string != null )
//                    System.out.println( string );
//                else {
//                    filePointer = bufferedReader.getFilePointer();
//                    bufferedReader.close();
//                    Thread.sleep( 2500 );
//                    bufferedReader = new RandomAccessFile( fileName, "r" );
//                    bufferedReader.seek( filePointer );
//                }
//
//            }
//        } catch ( IOException | InterruptedException e ) {
//            e.printStackTrace();
//        }


//        try{
//            FileInputStream fstream = new FileInputStream("");
//            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
//            String strLine;
//            /* read log line by line */
//            while ((strLine = br.readLine()) != null)   {
//                /* parse strLine to obtain what you want */
//                System.out.println (strLine);
//            }
//            fstream.close();
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//        }


//    }
//    public static void readNonStop(String filename, boolean goToEnd, FileReadCallback readCallback) {
//        if(readCallback == null) {
//            return;
//        }
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(filename));
//            try {
//                String line = br.readLine();
//                int lineNumber = 0;
//
//                if(goToEnd) {
//                    while(br.readLine() != null) {}
//                }
//
//                while (true) {
//                    if(line != null) {
//                        readCallback.onRead(lineNumber++, line);
//                    } else {
//                        Thread.sleep(1);
//                    }
//                    line = br.readLine();
//                }
//            } finally {
//                br.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    String tempFileName = "tempconfigFile4353454354.txt";

    @PostConstruct
    public void dodini() throws InterruptedException, IOException {
        Map<String, Object> payload = new HashMap<>();
        String tempFile = prepareTempPath() + File.separator + tempFileName;
        if (new File(tempFile).exists()) {
            payload.put("path", readFilePath(tempFile));
            fgsfg(payload);
        }
    }

    public static String prepareTempPath() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (!new File(tmpDir).exists()) {
            new File(tmpDir).mkdirs();
        }
        System.out.println(tmpDir);
        return tmpDir;
    }

    @PostMapping("save-config")
    public ResponseEntity<?> fgsfg(@RequestBody Map<String, Object> payload) throws InterruptedException, IOException {

        Map<String, Object> response = new HashMap<>();
        String filePath = payload.get("path").toString();

        if (!new File(filePath).exists()) {
            response.put("status", "FAILED");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        writeFileA(filePath);
        flag = false;
        Thread.sleep(3500);
        flag = true;
        new Thread(() -> {
            String fileName = filePath;
            try {
                RandomAccessFile bufferedReader = new RandomAccessFile(fileName, "r"
                );

                long filePointer;
                while (flag) {
                    final String string = bufferedReader.readLine();

                    if (string != null) {
                        ChatMessage ch = new ChatMessage();
                        ch.setSender("ddfgf");
                        ch.setContent(string);
                        ch.setType(ChatMessage.MessageType.CHAT);
                        template.convertAndSend("/topic/public", ch);
//                        chatController.data.add(string);
                    } else {
                        filePointer = bufferedReader.getFilePointer();
                        bufferedReader.close();
                        Thread.sleep(2500);
                        bufferedReader = new RandomAccessFile(fileName, "r");
                        bufferedReader.seek(filePointer);
                    }

                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }).start();

        response.put("status", "SUCCESS");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void writeFileA(String filePath) throws IOException {

        String tempFile1 = prepareTempPath() + File.separator + tempFileName;
        if (new File(tempFile1).exists()) {

            String existingPath = readFilePath(tempFile1);

            if (existingPath.trim().equalsIgnoreCase(filePath)) {
                return;
            }

        }

        String tempFile = prepareTempPath() + File.separator + tempFileName;
        Path path
                = Paths.get(tempFile);


        // Converting string to byte array
        // using getBytes() method
        byte[] arr = filePath.getBytes();

        // Try block to check for exceptions
        try {
            // Now calling Files.write() method using path
            // and byte array
            Files.write(path, arr);
        }

        // Catch block to handle the exceptions
        catch (IOException ex) {
            // Print message as exception occurred when
            // invalid path of local machine is passed
            System.out.print("Invalid Path");
        }
    }

    private String readFilePath(String tempFile) throws IOException {
        File file = new File(
                tempFile);
        Scanner sc = new Scanner(file);

        String path = "";

        while (sc.hasNextLine())
            path = path + sc.nextLine();

        return path.trim();
    }

    @GetMapping("/download-documents")
    public ResponseEntity<?> download() {
        try {


            String s = readFilePath(prepareTempPath() + File.separator + tempFileName);
            byte[] byteArrayInputStream1;
            try (InputStream inputStream = new FileInputStream(s)) {
                byteArrayInputStream1 = IOUtils.toByteArray(inputStream);

            }


            String contentType = "";


            Map<String, Object> fileResponse = new HashMap<>();


            fileResponse.put("file", "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(byteArrayInputStream1));

            fileResponse.put("fileName", "logFile.txt");

            Map<String, Object> resp = new HashMap<>();

            resp.put("message", "");
            resp.put("statusCode", 200);
            resp.put("status", "SUCCESS");
            resp.put("data", fileResponse);

            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (Exception e) {

            Map<String, Object> resp = new HashMap<>();

            resp.put("message", "");
            resp.put("statusCode", 500);
            resp.put("status", "FAILED");
            resp.put("data", null);
            return new ResponseEntity<>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping("logs")
    public ModelAndView handle1() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("logs.html");
        return modelAndView;
    }

    @RequestMapping("config")
    public ModelAndView handle2() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("configure.html");
        return modelAndView;
    }

    @RequestMapping("admin")
    public ModelAndView handle3() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("admin.html");
        return modelAndView;
    }

    @RequestMapping("buildconfig")
    public ModelAndView buildconfig() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("buildconfig.html");
        return modelAndView;
    }

    @RequestMapping("build")
    public ModelAndView build() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("build.html");
        return modelAndView;
    }

    @RequestMapping("APIGateway")
    public ModelAndView handle4() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index.html");
        return modelAndView;
    }


    @GetMapping("/get-path")
    public ResponseEntity<?> getPath() throws IOException {

        String tempFile1 = prepareTempPath() + File.separator + tempFileName;
        String existingPath = "";
        if (new File(tempFile1).exists()) {
            existingPath = readFilePath(tempFile1);
        }
        Map<String, Object> response = new HashMap<>();

        response.put("path", existingPath);

        return new ResponseEntity<>(response, HttpStatus.OK);

    }
}

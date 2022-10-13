import java.net.*;
import java.io.*;
import java.nio.file.*;

//New libraries for file permissions
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

public class MyServer {
    public static void main(String[] args) throws Exception {
        String root_path;
        int port;
        String[] parsedArgs = parseCommandLineArgs(args);

        root_path = parsedArgs[0];
        port = Integer.parseInt(parsedArgs[1]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                try  {
                    // multi-threading, spawn a worker thread to handle request
                    Socket client = serverSocket.accept();
                    Worker worker = new Worker(client, root_path);
                    worker.start();
                }catch(Exception e){
                    System.err.println("Creating worker error:"+e);
                }
            }
        }catch (Exception ee){
            System.err.println("Creating Server socket error:"+ee);
        }
    }

    // parse command line args
    private static String[] parseCommandLineArgs(String[] args){
        String root_path=" ", port=" ";
        for (int i=0; i < args.length; i++){
            if (args[i].equals("-document_root")){
                root_path = args[i+1];
            }
            else if (args[i].equals("-port")) {
                port = args[i+1];
            }
        }
        return new String[]{root_path, port};
    }

}

// worker thread class for multi-threading
class Worker extends Thread{
    Socket client;
    String root_path;

    public Worker(Socket client, String root_path){
        this.client = client;
        try {
        }catch(Exception e){
            System.out.println("Constructor error:");
            System.out.println(e);
        }
        this.root_path = root_path;
    }

    public void run(){
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while (!(line = br.readLine()).isBlank()) {
                requestBuilder.append(line + "\r\n");
            }
            //This prints out the "Get" request message to the console.
            String request = requestBuilder.toString();
            System.out.println(request);
            String[] splitedRequest = request.split("\r\n");
            String[] firstLine = splitedRequest[0].split("\\s+");
            String method = firstLine[0];
            String path = firstLine[1];
            Path filePath = this.getFilePath(path);


            System.out.println(filePath);
            System.out.println();

            // error handling
            if (!method.equals("GET")){
                //400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes()
                this.sendResponse("400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes());
            }

            else if(Files.exists(filePath))
            {
                //Need to check file permissions
                //Path filepaths = Paths.get(filePath);
                PosixFileAttributes attrs = Files.readAttributes(filePath, PosixFileAttributes.class);
                String permissionsString = PosixFilePermissions.toString(attrs.permissions());
                //If you cannot read or execute the file that the client wants (or both) then its forbidden.
                if(permissionsString.charAt(6) == '-')
                {
                    System.out.println("There is an issue here.");
                    this.sendResponse("403 Forbidden", "text/html", "<h1><b>403 Forbidden</b></h1>".getBytes());
                }
                //is a directory, then forbidden as well
                //else if()   
                else
                {
                    String contentType = getContentType(filePath);
                    this.sendResponse("200 OK", contentType, Files.readAllBytes(filePath));
                }
            }

            else
            {
                this.sendResponse("404 Not Found", "text/html", "<h1><b>404 Not Found</b></h1>".getBytes());
            }

        }
        
        catch (Exception e)
        {
            System.err.println(e);
            //Make this catch the deault repsone, something is wrong and its a bad request.
            try 
            {
                this.sendResponse("400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes());
            }
            catch (IOException ee){} 
        }
    }

    // return response
    private void sendResponse(String status, String contentType, byte[] content)
            throws IOException {
        OutputStream clientOutput = this.client.getOutputStream();


        //Helpful link: https://condor.depaul.edu/dmumaugh/readings/handouts/SE435/HTTP/node16.html
        //Other helpful link: https://www.w3.org/Protocols/HTTP/1.0/draft-ietf-http-v10-spec-01.html#Content-Type
        //https://www.tutorialspoint.com/http/http_requests.htm
        
        System.out.println("HTTP/1.0 response from the server to client request:\n");
        System.out.println("HTTP/1.0 "+status+"\r");
        System.out.println("ContentType: "+contentType+"\r");
        System.out.println("ContentLength: "+content.length+"\r");
        System.out.println("Date: "+(new java.util.Date()).toString()+"\r\n");
        System.out.println();

        clientOutput.write(("HTTP/1.0 "+status+"\r\n").getBytes());
        clientOutput.write(("ContentType: "+contentType+"\r\n").getBytes());
        clientOutput.write(("ContentLength: "+content.length+"\r\n").getBytes());
        clientOutput.write(("Date: "+(new java.util.Date()).toString()+"\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        // close connection after every request
        System.out.println("Closing socket......\r\n");
        System.out.println("------------------------------------------------\r\n");
        this.client.close();
    }

    private String getContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }


    private Path getFilePath(String path) {
        // handling the default page
        if ("/".equals(path)) {
            path = "/index.html";
        }
        String abs_path = this.root_path + path;
        return Paths.get(abs_path);
    }
}


//Import base libraries for console output and networking procedures.
import java.net.*;
import java.io.*;
import java.nio.file.*;

//Import Libraries for file attributes and file permissions.
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

public class MyServer 
{
    //Main function, throws an exception if we can't create a socket.
    public static void main(String[] args) throws Exception 
    {
        //This is the root document path represented as a string.
        String root_path;

        //This is the port number we pass via the command line.
        int port;

        //This function parses our two command line arguments and returns an array of string objects.
        String[] parsedArgs = parseCommandLineArgs(args);

        //Document root is the first arugment we pass, and port number is the second 
        //argument we pass. We need to call parse int to ensure that port is an integer.
        root_path = parsedArgs[0];
        port = Integer.parseInt(parsedArgs[1]);

        try 
        {
            //Create a new server socket object using the port number we passed in from the command line.
            ServerSocket serverSock = new ServerSocket(port);

            //Infinite loop, keep getting requests from clients. This means that we have to forcibly end our 
            //program and it also means that we will have to use differnet ports since the ServerSocket 
            //never gets closed if we forcibly end the program.
            while (true) 
            {
                try
                {
                    //Multi-threading approach, we will spawn a worker thread to handle each request
                    //made by any client.
                    Socket client = serverSock.accept();

                    //Pass the client socket as well as the document root path to the worker thread.
                    Worker worker = new Worker(client, root_path);

                    //Execute the thread's "run" function 
                    worker.start();

                }
                //If we can't create a owrker thread, throw an error.
                catch(Exception e)
                {
                    System.err.println("Error creating worker thread:"+e);
                }
            }
        }
        
        //If we can't create the server socket for some reason, throw an error
        catch (Exception ee)
        {
            System.err.println("Error creating server socket:"+ee);
        }
    }

    //Function to help parse our 2 command line arguments
    private static String[] parseCommandLineArgs(String[] arguments)
    {
        String root_path = " ";
        String port = " ";

        //If you run the program with document root first or second, this
        //for loop will correctly associate root and port with their associated 
        //string variables.
        for (int i = 0; i < arguments.length; i++)
        {
            if (arguments[i].equals("-document_root"))
            {
                root_path = arguments[i+1];
            }
            else if (arguments[i].equals("-port")) 
            {
                port = arguments[i+1];
            }
        }
        //Return array of strings with first element being document root path and second
        //element being port number.
        return new String[]{root_path, port};
    }

}

//Worker thread class that is basis for multithreading approach.
class Worker extends Thread
{
    Socket client;
    String root_path;

    //Worker constructor function, passes in client socket and document root path
    //in the constructor call.
    public Worker(Socket client, String root_path)
    {
        this.client = client;
        try 
        {

        }
        catch(Exception e)
        {
            System.out.println("Constructor error:");
            System.out.println(e);
        }

        this.root_path = root_path;
    }

    //This is our worker function
    public void run()
    {
        try 
        {
            //Create a buffer reader that reads what the client is going to send the server (the GET request).
            BufferedReader br = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            StringBuilder requestBuilder = new StringBuilder();
            String line;

            //Take each line of the HTTP GET request and append it to the requestBuilder object (as long as the 
            //line isn't blank)
            while (!(line = br.readLine()).isBlank()) 
            {
                requestBuilder.append(line + "\r\n");
            }

            //No we need to print out the GET request to the console, so we need to convert the
            //StringBuilder object to a string and then print it out so we can log the GET requests.
            //as per the assignment requirements.
            String request = requestBuilder.toString();
            System.out.println(request);

            //Split the HTTP request up by lines (as we need the first line for error detection)
            String[] splitedRequest = request.split("\r\n");

            //With the lines plit, we take the first line of the HTTP request (for example GET /index.html HTTP/1.1)
            //so we can access the method (GET, HEAD, POST, etc) for checking Error code 400 as well as the file path
            //for checking error code 404.
            String[] firstLine = splitedRequest[0].split("\\s+");

            //Method is a string with the HTTP request type (GET, HEAD, POST, etc).
            String method = firstLine[0];

            //This is the file path 
            String path = firstLine[1];

            //The "getFilePath" function helps to resolve the file path if there is only a "/" given by the user
            //as it defaults to "document root + index.html" instead. This function eseentially takes the file path 
            //given by the user and adds on the document
            Path filePath = this.getFilePath(path);


            System.out.println(filePath);
            System.out.println();

            //
            if (!method.equals("GET"))
            {
                //400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes()
                this.sendResponse("400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes());
            }

            else if(Files.exists(filePath))
            {
                //Need to check file permissions
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
    private void sendResponse(String status, String contentType, byte[] content) throws IOException 
    {
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

    private String getContentType(Path filePath) throws IOException 
    {
        return Files.probeContentType(filePath);
    }


    private Path getFilePath(String path) 
    {
        // handling the default page
        if ("/".equals(path)) {
            path = "/index.html";
        }
        String abs_path = this.root_path + path;
        return Paths.get(abs_path);
    }
}

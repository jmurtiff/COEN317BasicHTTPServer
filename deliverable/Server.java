//Name: Jordan Murtiff
//Date: 10-13-22
//Course: COEN 317 - Distributed Systems
//Assignment: Programming Assignment 1

//Import base libraries for console output, networking procedures, as well as reading from files the
//client requests.
import java.net.*;
import java.io.*;
import java.nio.file.*;

//Import libraries for file attributes and file permissions (we need this for error code 403).
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

//The Server class is the base class for main and for parsing arguments.
public class Server 
{
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

    //Main function, throws an exception if we can't create a socket.
    public static void main(String[] args) throws Exception 
    {
        //This is the root document path represented as a string.
        String root_path;

        //This is the port number we pass via the command line.
        int port;

        //This function parses our two command line arguments and returns an array of string objects.
        String[] parsedArgs = parseCommandLineArgs(args);

        //Document root is the first argument we pass, and port number is the second 
        //argument we pass. We need to call parse int to ensure that port is an integer.
        root_path = parsedArgs[0];
        port = Integer.parseInt(parsedArgs[1]);

        try 
        {
            //Create a new server socket object using the port number we passed in from the command line.
            ServerSocket serverSock = new ServerSocket(port);

            //Infinite loop, keep getting requests from clients. This means that we have to forcibly end our 
            //program and it also means that we will have to use different ports since the ServerSocket 
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
                //If we can't create a worker thread, throw an error.
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

    //This function sends a HTTP version 1.0 response from the server to the client through the socket's 
    //output stream. In this case the server has already read the file and just needs to write to the socket
    //in order to transmit the contents of the HTTP response as well as the content of the requested file.

    //All passed values (status, content type, and the content itself is given) is passed from above.
    private void sendResponse(String status, String contentType, byte[] content) throws IOException 
    {
        OutputStream clientOutput = this.client.getOutputStream();

        //Here we are printing out the HTTP response from the server to the console (for logging purposes).
        System.out.println("HTTP/1.0 response from the server to client request:\n");
        System.out.println("HTTP/1.0 "+status+"\r");
        System.out.println("ContentType: "+contentType+"\r");
        System.out.println("ContentLength: "+content.length+"\r");
        System.out.println("Date: "+(new java.util.Date()).toString()+"\r\n");
        System.out.println();

        //Here we are writing the ContentType, ContentLength, and Date headers (at a minimum given the assignment)
        //to the client. Afterwards we are transmitting the actual bytes of the file content to te client and then 
        //we flush out the output stream.
        clientOutput.write(("HTTP/1.0 "+status+"\r\n").getBytes());
        clientOutput.write(("ContentType: "+contentType+"\r\n").getBytes());
        clientOutput.write(("ContentLength: "+content.length+"\r\n").getBytes());
        clientOutput.write(("Date: "+(new java.util.Date()).toString()+"\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();

        //Now that the server has transmitted the HTTP response and file, the thread has completed its
        //work and can close the socket.
        System.out.println("Closing socket connection......\r\n");
        this.client.close();
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
            //line isn't blank).
            while (!(line = br.readLine()).isBlank()) 
            {
                requestBuilder.append(line + "\r\n");
            }

            //Now we need to print out the HTTP request to the console, so we need to convert the
            //StringBuilder object to a string and then print it out so we can log the HTTP requests.
            //as per the assignment requirements. Its good that HTTP requests have the same formatting 
            //(at least for the beginning of the request )
            String request = requestBuilder.toString();
            System.out.println(request);

            //Split the HTTP request up by each lines (as we need the first line for error detection purposes)
            //and output to an array of Strings.
            String[] splitedRequest = request.split("\r\n");

            //With the lines split, we take the first line of the HTTP request (for example GET /index.html HTTP/1.1)
            //so we can access the method (GET, HEAD, POST, etc) for checking Error code 400 as well as the file path
            //for checking error code 404.
            String[] firstLine = splitedRequest[0].split("\\s+");

            //Method is a string with the HTTP request type (GET, HEAD, POST, etc).
            String method = firstLine[0];

            //This is the file path 
            String path = firstLine[1];

            //The "getFilePath" function helps to resolve the file path if there is only a "/" given by the user
            //as it defaults to "document root + index.html" instead. This function essentially takes the file path 
            //given by the user and adds on the document root passed in the command line.
            Path filePath = this.getFilePath(path);

            //We print out the entire file path to the console so we can see what file is being referenced by the 
            //HTTP request.
            System.out.println(filePath);
            System.out.println();

            //The below if-else block handles our 4 status codes (3 of which are error codes).

            //First error we handle is error code 400, which is defined as anything that is not a GET request, fairly
            //simple, but its the first error we should handle before we try seeing if the file exists or not.
            if (!method.equals("GET"))
            {
                //
                this.sendResponse("400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes());
            }

            //If the file the client is trying to access exists, we have to check permissions before we try 
            //accessing the file itself.
            else if(Files.exists(filePath))
            {
                //Access the file's attributes (one of which is the file attributes) and create a 
                //string with all of a given file's permissions (all 9 values, so for example rwx rwx rwx).
                PosixFileAttributes attrs = Files.readAttributes(filePath, PosixFileAttributes.class);
                String permissionsString = PosixFilePermissions.toString(attrs.permissions());

                //If the file is not world readable xxx xxx --- then you cannot read file that the client wants
                //and therefore we should throw error 403.
                if(permissionsString.charAt(6) == '-')
                {
                    this.sendResponse("403 Forbidden", "text/html", "<h1><b>403 Forbidden</b></h1>".getBytes());
                }
 
                //If the file permissions are set to world readable, then we send status code 200 and we call
                //the send response function (which in turn sends an HTTP status message to the client given the 
                //content type as well as the content of the file itself).
                else
                {
                    String contentType = getContentType(filePath);

                    //The contentType could be text/html, jpg, gif, etc, and we need to read all the bytes 
                    //of a file in order to 
                    this.sendResponse("200 OK", contentType, Files.readAllBytes(filePath));
                }
            }

            //If the file that the client wants does not exist (so it !(Files.exists(filePath)) then we send error 
            //code 400 as a response.
            else
            {
                this.sendResponse("404 Not Found", "text/html", "<h1><b>404 Not Found</b></h1>".getBytes());
            }

        }
        
        //If there is some sort of Java exception, then we can send error code 400 as the only time that this happens
        //is if we got some sort of NULL pointer exception or some sort of socket communication error.
        catch (Exception e)
        {
            //Print out the Java exception and then print out error code 400.
            System.err.println(e);
            try 
            {
                this.sendResponse("400 Bad Request", "text/html", "<h1><b>400 Bad Request</b></h1>".getBytes());
            }
            catch (IOException ee){} 
        }
    }

    //In order to handle the default page (the filepath being just "/") we need to write a function
    //in order to return the document path + /index/html added on the end of it. If the ("/") is not used
    //then all we have to do is take the document root and add on the file path we are given (so for example /home/
    //test/ServerDocuments/ + test.txt).
    private Path getFilePath(String path) 
    {
        // handling the default page
        if ("/".equals(path)) 
        {
            path = "/index.html";
        }
        String absolute_path = this.root_path + path;
        return Paths.get(absolute_path);
    }
    

    //This function is called above in order to get the file type that we need to transmit as part 
    //of the HTTP response header. We only get the file type as long as we don't satisfy any of the 
    //three HTTP error codes (404, 403, or 400).
    private String getContentType(Path filePath) throws IOException 
    {
        return Files.probeContentType(filePath);
    }

}

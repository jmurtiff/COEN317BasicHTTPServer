Name: 
    Yujie Zhu
Assignment: 
    COEN 317 Programming Assignment 1
Date: 
    Apr 26, 2022
High-level description:
    This program is a HTTP web server that returns static files upon client's request.
    When running this program, command line options are required. (document_root and port).
    This server will listen on a spcified port, waiting for incoming request.
    Once a request comes, the server will spawn a worker thread to handle the request,
    while the main thread keeps waiting for new request at the port.
    Worker thread will analyze incoming request, send back static file if everything goes well.
    A request to "/" will be changed to "/index.html" which is the default page.
    If requested file is not found, a 404 error will be returned.
    If request meathod is not GET, a 403 error will be returned.
    For other exceptions, a 400 error will be returned.
    Connection is closed after every response.
List of submitted files:
    readme.md
    scriptFile.pdf
    MyServer.java
    compileInstruction.txt
    index.html (for testing)
    Multiserver.png (for testing)
Instructions for running program:
    Java Runtime Environment is required.
    Java version 17 or higher.
    To run this program, compile it first:
        javac MyServer.java
    Then run it with command line options:
        java MyServer -document_root <$DOCUMENT_ROOT> -port <$PORT>
        example:
            java MyServer -document_root /home/ubuntu/webserver_files -port 8808
    Make sure files have permission to be read/written/executed.

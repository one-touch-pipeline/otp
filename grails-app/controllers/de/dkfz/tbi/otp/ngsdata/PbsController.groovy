package de.dkfz.tbi.otp.ngsdata

import ch.ethz.ssh2.*;

class PbsController {

    def index() {

        String hostname = "193.174.51.121";
        String username = "$USER";
        String fileName = "${home}.ssh/id_dsa";

        File keyfile = new File(filename);

        /*
        if (keyfile.canRead()) {
            System.out.println("file found " + keyfile);
        } else {
            System.out.println("file not found " + keyfile);
        }
        */
    }
    /*
        try  {
            // Create a connection instance 
            Connection conn = new Connection(hostname);

            // Now connect 
            conn.connect();

            // Authenticate 
            boolean isAuthenticated = 
                conn.authenticateWithPublicKey(username, keyfile, keyfilePass);

            if (isAuthenticated == false)
                throw new IOException("Authentication failed.");

                /// Create a session 

            Session sess = conn.openSession();
            sess.execCommand("uname -a && date && uptime && who");

            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

            render ("Here is some information about the remote host:");

            while (true) {

                String line = br.readLine();
                if (line == null)
                break;
                render (line);
            }

            // Close this session 
            sess.close();

            // Close the connection 
            conn.close();

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    */
}
